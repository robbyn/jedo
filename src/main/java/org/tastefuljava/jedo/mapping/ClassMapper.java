package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.Reflection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class ClassMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ClassMapper.class.getName());

    private final FieldMapper<ColumnMapper>[] idFields;
    private final FieldMapper<ValueMapper>[] fields;
    private final Map<String,Statement> queries;
    private final Map<String,Statement> stmts;
    private final Statement load;
    private final Statement insert;
    private final Statement update;
    private final Statement delete;

    private ClassMapper(Builder builder) {
        super(builder);
        this.idFields = builder.buildIdFields();
        this.fields = builder.buildFields();
        this.queries = builder.buildQueries();
        this.stmts = builder.buildStatements();
        this.load = builder.buildLoad();
        this.insert = builder.buildInsert();
        this.update = builder.buildUpdate();
        this.delete = builder.buildDelete();
    }

    public Class<?> getMappedClass() {
        return type;
    }

    public Object newInstance() {
        try {
            return type.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | NoSuchMethodException | SecurityException
                | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public Object[] getIdValues(Object obj) {
        Object[] values = new Object[idFields.length];
        for (int i = 0; i < idFields.length; ++i) {
            values[i] = idFields[i].getValue(obj);
        }
        return values;
    }

    public Object[] getIdValuesFromResultSet(ResultSet rs) {
        Object[] values = new Object[idFields.length];
        for (int i = 0; i < idFields.length; ++i) {
            values[i] = idFields[i].fromResultSet(null, null, rs);
        }
        return values;
    }

    public Object load(Storage pm, Object[] parms) {
        if (load == null) {
            throw new JedoException("No loader for class " + type.getName());
        }
        return pm.queryOne(this, load, parms);
    }

    public Object queryOne(Storage pm, String name,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        return pm.queryOne(this, stmt, parms);
    }

    public List<Object> query(Storage pm, String name, Object self,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        List<Object> result = new ArrayList<>();
        pm.query(this, stmt, self, parms, result);
        return result;
    }

    public void invoke(Storage pm, String name,
            Object[] parms) {
        Statement stmt = stmts.get(name);
        if (stmt == null) {
            throw new JedoException(
                    "No statement " + name + " for " + type.getName());
        }
        pm.execute(stmt, null, parms);
    }

    public void insert(Storage pm, Object obj) {
        if (insert == null) {
            throw new JedoException("No inserter for " + type.getName());
        }
        pm.insert(this, insert, obj, null);
    }

    public void afterInsert(Storage pm, Object self) {
        for (FieldMapper<ValueMapper> prop: fields) {
            prop.afterInsert(pm, self);
        }
    }

    public void collectKeys(Storage pm, Object obj, ResultSet rs) {
        for (FieldMapper<ColumnMapper> prop : idFields) {
            prop.setFromResultSet(pm, obj, rs);
        }
    }

    public void update(Storage pm, Object obj) {
        if (update == null) {
            throw new JedoException(
                    "No updater for " + type.getName());
        }
        pm.update(this, update, obj, null);
    }

    public void beforeUpdate(Storage pm, Object self) {
        for (FieldMapper<ValueMapper> prop: fields) {
            prop.beforeUpdate(pm, self);
        }
    }

    public void afterUpdate(Storage pm, Object self) {
        for (FieldMapper<ValueMapper> prop: fields) {
            prop.afterUpdate(pm, self);
        }
    }

    public void delete(Storage pm, Object obj) {
        if (delete == null) {
            throw new JedoException(
                    "No deleter for " + type.getName());
        }
        pm.delete(this, delete, obj);
    }

    public void beforeDelete(Storage pm, Object self) {
        for (FieldMapper<ValueMapper> prop: fields) {
            prop.beforeDelete(pm, self);
        }
    }

    Statement getQuery(String queryName) {
        return queries.get(queryName);
    }

    void fixForwards(Map<Class<?>, ClassMapper> map) {
        for (FieldMapper<ValueMapper> fm: fields) {
            fm.fixForwards(map);
        }
    }

    public void setFieldsFromResultSet(Storage pm, Object obj,
            ResultSet rs) {
        for (FieldMapper<ColumnMapper> field: idFields) {
            field.setFromResultSet(pm, obj, rs);
        }
        for (FieldMapper<ValueMapper> field: fields) {
            field.setFromResultSet(pm, obj, rs);
        }
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        return pm.loadFromResultSet(this, rs);
    }

    public static class Builder extends ValueMapper.Builder<ClassMapper> {
        private final Map<Field,ColumnMapper.Builder> idField
                = new LinkedHashMap<>();
        private final Map<Field,ValueMapper.Builder> fields
                = new LinkedHashMap<>();
        private final Map<String,Statement.Builder> queries = new HashMap<>();
        private final Map<String,Statement.Builder> stmts = new HashMap<>();
        private Statement.Builder load;
        private Statement.Builder insert;
        private Statement.Builder update;
        private Statement.Builder delete;

        public Builder(Class<?> type) {
            super(type);
        }

        public void addIdField(String fieldName, String column) {
            Field field = getField(fieldName);
            idField.put(field, new ColumnMapper.Builder(field.getType(), column));
        }

        public void addField(String fieldName, String column) {
            Field field = getField(fieldName);
            fields.put(field, new ColumnMapper.Builder(field.getType(), column));
        }

        public void addReference(String fieldName, String[] columns,
                String fetchMode) {
            Field field = getField(fieldName);
            FetchMode mode = fetchMode(fetchMode, FetchMode.EAGER);
            ReferenceMapper.Builder ref = new ReferenceMapper.Builder(
                    field.getType(), columns, mode);
            fields.put(field, ref);
        }

        public SetMapper.Builder newSet(String name, String fetchMode,
                String order) {
            Field field = getField(name);
            Field[] orderFields;
            if (order == null) {
                orderFields = null;
            } else {
                Class<?> targetClass = Reflection.getReferencedType(field);
                String[] orderNames = order.split("\\s*[\\s,;]\\s*");
                orderFields = Reflection.fieldList(targetClass, orderNames);
            }
            FetchMode realFetchMode = fetchMode(fetchMode, FetchMode.LAZY);
            SetMapper.Builder result = new SetMapper.Builder(
                    this, field, realFetchMode, orderFields);
            fields.put(field, result);
            return result;
        }

        public ListMapper.Builder newList(String name, String fetchMode) {
            Field field = getField(name);
            ListMapper.Builder result = new ListMapper.Builder(
                    this, field, fetchMode(fetchMode, FetchMode.LAZY));
            fields.put(field, result);
            return result;
        }

        public ComponentMapper.Builder newComponent(String name) {
            Field field = getField(name);
            ComponentMapper.Builder cm
                    = new ComponentMapper.Builder(field.getType());
            fields.put(field, cm);
            return cm;
        }

        public Statement.Builder newStatement(String[] paramNames) {
            return newStatement(paramNames, false);
        }

        public Statement.Builder newStatement(boolean generatedKeys) {
            return newStatement(null, generatedKeys);
        }

        public Statement.Builder newStatement(String[] paramNames,
                boolean generatedKeys) {
            Statement.Builder stmt = new Statement.Builder(type, paramNames);
            if (generatedKeys) {
                stmt.setGeneratedKeys(getIdColumns());
            }
            return stmt;
        }

        public Statement.Builder newLoadStatement() {
            return load = newStatement(getIdFieldNames());
        }

        public void addQuery(String name, Statement.Builder stmt) {
            queries.put(name, stmt);
        }

        public void addStatement(String name, Statement.Builder stmt) {
            stmts.put(name, stmt);
        }

        public void setLoad(Statement.Builder stmt) {
            load = stmt;
        }

        public void setInsert(Statement.Builder stmt) {
            insert = stmt;
        }

        public void setUpdate(Statement.Builder stmt) {
            update = stmt;
        }

        public void setDelete(Statement.Builder stmt) {
            delete = stmt;
        }

        private FieldMapper<ColumnMapper>[] buildIdFields() {
            FieldMapper<ColumnMapper>[] result = new FieldMapper[idField.size()];
            int i = 0;
            for (Map.Entry<Field,ColumnMapper.Builder> e: idField.entrySet()) {
                result[i++] = new FieldMapper<>(e.getKey(), e.getValue().build());
            }
            return result;
        }

        private FieldMapper<ValueMapper>[] buildFields() {
            FieldMapper<ValueMapper>[] result = new FieldMapper[fields.size()];
            int i = 0;
            for (Map.Entry<Field,ValueMapper.Builder> e: fields.entrySet()) {
                result[i++] = new FieldMapper<>(e.getKey(), e.getValue().build());
            }
            return result;
        }

        private Map<String,Statement> buildQueries() {
            Map<String,Statement> result = new HashMap<>();
            for (Map.Entry<String,Statement.Builder> e: queries.entrySet()) {
                result.put(e.getKey(), e.getValue().build());
            }
            return result;
        }

        private Map<String,Statement> buildStatements() {
            Map<String,Statement> result = new HashMap<>();
            for (Map.Entry<String,Statement.Builder> e: stmts.entrySet()) {
                result.put(e.getKey(), e.getValue().build());
            }
            return result;
        }

        public Statement buildLoad() {
            return load == null ? null : load.build();
        }

        public Statement buildInsert() {
            return insert == null ? null : insert.build();
        }

        public Statement buildUpdate() {
            return update == null ? null : update.build();
        }

        public Statement buildDelete() {
            return delete == null ? null : delete.build();
        }

        public ClassMapper build() {
            return new ClassMapper(this);
        }

        private FetchMode fetchMode(String fetchMode, FetchMode def) {
            FetchMode fm = null;
            if (fetchMode !=  null) {
                fm = FetchMode.fromString(fetchMode);
            }
            if (fm == null) {
                fm = def;
            }
            return fm;
        }

        private String[] getIdFieldNames() {
            String[] result = new String[idField.size()];
            int i = 0;
            for (Map.Entry<Field,ColumnMapper.Builder> e: idField.entrySet()) {
                result[i++] = e.getKey().getName();
            }
            return result;
        }

        public String[] getIdColumns() {
            String[] result = new String[idField.size()];
            int i = 0;
            for (Map.Entry<Field,ColumnMapper.Builder> e: idField.entrySet()) {
                result[i++] = e.getValue().getColumn();
            }
            return result;
        }

        @Override
        public void fixForwards(Map<Class<?>, Builder> map) {
            for (ValueMapper.Builder fm: fields.values()) {
                fm.fixForwards(map);
            }
        }
    }
}
