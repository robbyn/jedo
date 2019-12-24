package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import org.tastefuljava.jedo.util.Reflection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class ClassMapper {
    private static final Logger LOG
            = Logger.getLogger(ClassMapper.class.getName());

    private final Class<?> clazz;
    private final SimpleFieldMapper[] idFields;
    private final FieldMapper[] fields;
    private final Map<String,Statement> queries;
    private final Map<String,Statement> stmts;
    private final Statement load;
    private final Statement insert;
    private final Statement update;
    private final Statement delete;

    private ClassMapper(Builder builder) {
        this.clazz = builder.clazz;
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
        return clazz;
    }

    public Object newInstance() {
        try {
            return clazz.getConstructor().newInstance();
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
            values[i] = idFields[i].fromResultSet(rs);
        }
        return values;
    }

    public Object load(Storage pm, Object[] parms) {
        if (load == null) {
            throw new JedoException("No loader for class " + clazz.getName());
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
                    "No statement " + name + " for " + clazz.getName());
        }
        pm.execute(stmt, null, parms);
    }

    public void insert(Storage pm, Object obj) {
        if (insert == null) {
            throw new JedoException("No inserter for " + clazz.getName());
        }
        pm.insert(this, insert, obj, null);
    }

    public void afterInsert(Storage pm, Object self) {
        for (FieldMapper prop: fields) {
            prop.afterInsert(pm, self);
        }
    }

    public void collectKeys(Statement statement, PreparedStatement stmt,
            Object obj) {
        statement.collectKeys(stmt, idFields, obj);
    }

    public void update(Storage pm, Object obj) {
        if (update == null) {
            throw new JedoException(
                    "No updater for " + clazz.getName());
        }
        pm.execute(update, obj, null);
    }

    public void delete(Storage pm, Object obj) {
        if (delete == null) {
            throw new JedoException(
                    "No deleter for " + clazz.getName());
        }
        pm.delete(this, delete, obj);
    }

    Statement getQuery(String queryName) {
        return queries.get(queryName);
    }

    public void writeTo(XMLWriter out) {
        out.startTag("class");
        out.attribute("name", clazz.getName());
        if (idFields.length > 0) {
            out.startTag("id");
            for (SimpleFieldMapper pm: idFields) {
                pm.writeTo(out);
            }
            out.endTag();
            for (FieldMapper pm: fields) {
                pm.writeTo(out);
            }
            if (load != null) {
                load.writeTo(out, "load", null);
            }
            for (Map.Entry<String,Statement> e: queries.entrySet()) {
                e.getValue().writeTo(out, "query", e.getKey());
            }
            if (insert != null) {
                insert.writeTo(out, "insert", null);
            }
            if (update != null) {
                update.writeTo(out, "update", null);
            }
            if (delete != null) {
                delete.writeTo(out, "delete", null);
            }
        }
        out.endTag();
    }

    void fixForwards(Map<Class<?>, ClassMapper> map) {
        for (FieldMapper fm: fields) {
            fm.fixForwards(map);
        }
    }

    public void setFieldsFromResultSet(Storage pm, Object obj,
            ResultSet rs) {
        for (SimpleFieldMapper field: idFields) {
            field.setValue(obj, field.fromResultSet(pm, obj, rs));
        }
        for (FieldMapper field: fields) {
            field.setValue(obj, field.fromResultSet(pm, obj, rs));
        }
    }

    public static class Builder {
        private final Class<?> clazz;
        private final List<SimpleFieldMapper.Builder> idField = new ArrayList<>();
        private final List<FieldMapper.Builder<? extends FieldMapper>> fields
                = new ArrayList<>();
        private final Map<String,Statement.Builder> queries = new HashMap<>();
        private final Map<String,Statement.Builder> stmts = new HashMap<>();
        private Statement.Builder load;
        private Statement.Builder insert;
        private Statement.Builder update;
        private Statement.Builder delete;

        public Builder(Class<?> clazz) {
            this.clazz = clazz;
        }

        public void fixForwards(Map<Class<?>, ClassMapper.Builder> map) {
            for (FieldMapper.Builder fm: fields) {
                fm.fixForwards(map);
            }
        }

        public Class<?> getMappedClass() {
            return clazz;
        }

        public void addIdField(String field, String column) {
            idField.add(newSimpleField(field, column));
        }

        public void addField(String field, String column) {
            fields.add(newSimpleField(field, column));
        }

        public void addReference(String field, String[] columns,
                String fetchMode) {
            ReferenceMapper.Builder ref = newReference(field, columns, fetchMode);
            fields.add(ref);
        }

        public CollectionMapper.Builder newCollection(String name,
                String fetchMode) {
            Field field = Reflection.getInstanceField(clazz, name);
            if (field == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new CollectionMapper.Builder(this, field,
                    fetchMode(fetchMode, FetchMode.LAZY));
        }

        public void addCollection(CollectionMapper.Builder ref) {
             fields.add(ref);
        }

        public ComponentMapper.Builder newComponent(String name) {
            return new ComponentMapper.Builder(
                    Reflection.getInstanceField(clazz, name));
        }

        public void addComponent(ComponentMapper.Builder cm) {
            fields.add(cm);
        }

        public Statement.Builder newStatement(String[] paramNames) {
            return newStatement(paramNames, false);
        }

        public Statement.Builder newStatement(boolean generatedKeys) {
            return newStatement(null, generatedKeys);
        }

        public Statement.Builder newStatement(String[] paramNames,
                boolean generatedKeys) {
            Statement.Builder stmt = new Statement.Builder(clazz, paramNames);
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

        private SimpleFieldMapper[] buildIdFields() {
            SimpleFieldMapper[] result = new SimpleFieldMapper[idField.size()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = idField.get(i).build();
            }
            return result;
        }

        private FieldMapper[] buildFields() {
            FieldMapper[] result = new FieldMapper[fields.size()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = fields.get(i).build();
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

        private SimpleFieldMapper.Builder newSimpleField(
                String name, String column) {
            Field field = Reflection.getInstanceField(clazz, name);
            if (field == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new SimpleFieldMapper.Builder(field, column);
        }

        private ReferenceMapper.Builder newReference(String name,
                String[] columns, String fetchMode) {
            Field field = Reflection.getInstanceField(clazz, name);
            if (field == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new ReferenceMapper.Builder(field, columns,
                    fetchMode(fetchMode, FetchMode.EAGER));
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
            for (int i = 0; i < idField.size(); ++i) {
                result[i] = idField.get(i).getFieldName();
            }
            return result;
        }

        public String[] getIdColumns() {
            String[] result = new String[idField.size()];
            for (int i = 0; i < idField.size(); ++i) {
                result[i] = idField.get(i).getColumn();
            }
            return result;
        }
    }
}
