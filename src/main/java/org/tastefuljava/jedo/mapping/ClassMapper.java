package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.Reflection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class ClassMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ClassMapper.class.getName());

    private final String tableName;
    private ClassMapper superClass;
    private ClassMapper[] subclasses;
    private final Discriminator discriminator;
    private final FieldMapper<ColumnMapper>[] idFields;
    private final FieldMapper<? extends ValueMapper>[] fields;
    private final Map<String,Statement> queries;
    private final Map<String,Statement> stmts;
    private Statement load;
    private Statement insert;
    private final Statement update;
    private final Statement delete;

    private ClassMapper(BuildContext context, Builder builder) {
        super(builder);
        this.tableName = builder.buildTableName();
        this.discriminator = builder.buildDiscriminator(context);
        this.idFields = builder.buildIdFields(context);
        this.fields = builder.buildFields(context);
        this.queries = builder.buildQueries();
        this.stmts = builder.buildStatements();
        this.update = builder.buildUpdate();
        this.delete = builder.buildDelete();
        context.addForward((mapper)->{
            if (builder.superClass != null) {
                ClassMapper cm = mapper.getClassMapper(builder.superClass);
                if (!cm.idFieldsCompatible(idFields)) {
                     throw new JedoException("ID fields of class "
                             + type.getName()
                             + " are not compatible with those of class "
                             + cm.type.getName());
                }
                superClass = cm;
            }
            subclasses = builder.buildSubclasses(mapper);
        });
        context.addFinalizer(()->{
            load = builder.buildLoad(getIdFieldNames());
            insert = builder.buildInsert(getIdColumns());
        });
    }

    @Override
    public <T> T accept(ValueMapperVisitor<T> vtor) {
        return vtor.visitClassMapper(this);
    }

    public String getTableName() {
        return tableName;
    }

    public Class<?> getMappedClass() {
        return type;
    }

    public ClassMapper getBaseClass() {
        ClassMapper cm = this;
        ClassMapper sup;
        while ((sup = cm.superClass) != null) {
            cm = sup;
        }
        return cm;
    }

    public ClassMapper getSuperClass() {
        return superClass;
    }

    public FieldMapper<ColumnMapper>[] getIdFields() {
        return idFields.clone();
    }

    public FieldMapper<? extends ValueMapper>[] getEachNonIdFields() {
        return fields.clone();
    }

    public ClassMapper resolveClass(ResultSet rs) {
        try {
            if (discriminator == null) {
                return this;
            }
            ClassMapper cm = discriminator.resolve(rs);
            return cm == null ? this : cm.resolveClass(rs);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
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
        if (superClass != null) {
            return superClass.getIdValues(obj);
        }
        Object[] values = new Object[idFields.length];
        for (int i = 0; i < idFields.length; ++i) {
            values[i] = idFields[i].getValue(obj);
        }
        return values;
    }

    public Object[] getIdValuesFromResultSet(ResultSet rs) {
        if (superClass != null) {
            return superClass.getIdValuesFromResultSet(rs);
        }
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

    public int invoke(Storage pm, String name,
            Object[] parms) {
        Statement stmt = stmts.get(name);
        if (stmt == null) {
            throw new JedoException(
                    "No statement " + name + " for " + type.getName());
        }
        return pm.execute(stmt, null, parms);
    }

    public void insert(Storage pm, Object obj) {
        if (superClass != null) {
            superClass.insert(pm, obj);
        }
        if (insert == null) {
            throw new JedoException("No inserter for " + type.getName());
        }
        pm.insert(this, insert, obj, null);
    }

    public void afterInsert(Storage pm, Object self) {
        if (superClass != null) {
            superClass.afterInsert(pm, self);
        }
        for (FieldMapper<? extends ValueMapper> field: fields) {
            field.afterInsert(pm, self);
        }
    }

    public void collectKeys(Storage pm, Object obj, ResultSet rs) {
        if (superClass != null) {
            superClass.collectKeys(pm, obj, rs);
        } else {
            for (FieldMapper<ColumnMapper> prop : idFields) {
                prop.setFromResultSet(pm, obj, rs);
            }
        }
    }

    public void update(Storage pm, Object obj) {
        if (superClass != null) {
            superClass.update(pm, obj);
        }
        if (update == null) {
            throw new JedoException(
                    "No updater for " + type.getName());
        }
        pm.update(this, update, obj, null);
    }

    public void beforeUpdate(Storage pm, Object self) {
        for (FieldMapper<? extends ValueMapper> field: fields) {
            field.beforeUpdate(pm, self);
        }
        if (superClass != null) {
            superClass.beforeUpdate(pm, self);
        }
    }

    public void afterUpdate(Storage pm, Object self) {
        if (superClass != null) {
            superClass.afterUpdate(pm, self);
        }
        for (FieldMapper<? extends ValueMapper> field: fields) {
            field.afterUpdate(pm, self);
        }
    }

    public void delete(Storage pm, Object obj) {
        if (delete == null) {
            throw new JedoException(
                    "No deleter for " + type.getName());
        }
        pm.delete(this, delete, obj);
        if (superClass != null) {
            superClass.delete(pm, obj);
        }
    }

    public void beforeDelete(Storage pm, Object self) {
        for (FieldMapper<? extends ValueMapper> field: fields) {
            field.beforeDelete(pm, self);
        }
        if (superClass != null) {
            superClass.beforeDelete(pm, self);
        }
    }

    Statement getQuery(String queryName) {
        return queries.get(queryName);
    }

    public void setFieldsFromResultSet(Storage pm, Object obj,
            ResultSet rs) {
        if (superClass != null) {
            superClass.setFieldsFromResultSet(pm, obj, rs);
        } else {
            for (FieldMapper<ColumnMapper> field: idFields) {
                field.setFromResultSet(pm, obj, rs);
            }
        }
        for (FieldMapper<? extends ValueMapper> field: fields) {
            field.setFromResultSet(pm, obj, rs);
        }
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs,
            ValueAccessor fm) {
        return pm.loadFromResultSet(this, rs);
    }

    public String[] getIdFieldNames() {
        String[] result = new String[idFields.length];
        int i = 0;
        for (FieldMapper<ColumnMapper> fm: idFields) {
            result[i++] = fm.getFieldName();
        }
        return result;
    }

    public String[] getIdColumns() {
        String[] result = new String[idFields.length];
        int i = 0;
        for (FieldMapper<ColumnMapper> fm: idFields) {
            result[i++] = fm.getValueMapper().getColumn();
        }
        return result;
    }

    private boolean idFieldsCompatible(FieldMapper<ColumnMapper>[] idFields) {
        if (idFields.length != this.idFields.length) {
            return false;
        }
        for (int i = 0; i < idFields.length; ++i) {
            if (!idFields[i].getField().equals(this.idFields[i].getField())) {
                return false;
            }
        }
        return true;
    }

    public static class Builder extends ValueMapper.Builder<ClassMapper> {
        private final String tableName;
        private Class<?> superClass;
        private final List<Class<?>> subclasses = new ArrayList<>();
        private Discriminator.Builder discriminator;
        private final Map<Field,FieldMapper.Builder<ColumnMapper>> idFields
                = new LinkedHashMap<>();
        private final Map<
                Field,FieldMapper.Builder<? extends ValueMapper>> fields
                = new LinkedHashMap<>();
        private final Map<String,Statement.Builder> queries = new HashMap<>();
        private final Map<String,Statement.Builder> stmts = new HashMap<>();
        private Statement.Builder load;
        private Statement.Builder insert;
        private Statement.Builder update;
        private Statement.Builder delete;

        public Builder(Class<?> type, String tableName) {
            super(type);
            this.tableName = tableName;
        }

        public String buildTableName() {
            return tableName != null ? tableName : type.getSimpleName();
        }

        public void setSuperClass(Class<?> superClass) {
            this.superClass = superClass;
        }

        public void setDiscriminator(Discriminator.Builder disc) {
            this.discriminator = disc;
        }

        public void addIdField(String fieldName, String column) {
            Field field = getField(fieldName);
            ColumnMapper.Builder vm = new ColumnMapper.Builder(
                    field.getType(), column);
            idFields.put(field, new FieldMapper.Builder<>(
                    field, vm, false));
        }

        public void addField(String fieldName, String column) {
            Field field = getField(fieldName);
            ColumnMapper.Builder vm = new ColumnMapper.Builder(
                    field.getType(), column);
            fields.put(field, new FieldMapper.Builder<>(
                    field, vm, false));
        }

        public void addReference(String fieldName, String[] columns,
                String fetchMode, boolean nullable) {
            Field field = getField(fieldName);
            FetchMode mode = fetchMode(fetchMode, FetchMode.EAGER);
            ReferenceMapper.Builder ref = new ReferenceMapper.Builder(
                    field, columns, mode);
            fields.put(field, new FieldMapper.Builder<>(
                    field, ref, nullable));
        }

        public SetMapper.Builder newSet(String name, String fetchMode,
                String order) {
            Field field = getField(name);
            Field[] orderFields;
            if (order == null) {
                orderFields = null;
            } else {
                Class<?> targetClass = Reflection.getReferencedClass(field);
                String[] orderNames = order.split("\\s*[\\s,;]\\s*");
                orderFields = Reflection.fieldList(targetClass, orderNames);
            }
            FetchMode realFetchMode = fetchMode(fetchMode, FetchMode.LAZY);
            SetMapper.Builder result = new SetMapper.Builder(
                    this, field, realFetchMode, orderFields);
            fields.put(field, new FieldMapper.Builder<>(field, result, false));
            return result;
        }

        public ListMapper.Builder newList(String name, String fetchMode) {
            Field field = getField(name);
            FetchMode mode = fetchMode(fetchMode, FetchMode.LAZY);
            ListMapper.Builder result = new ListMapper.Builder(
                    this, field, mode);
            fields.put(field, new FieldMapper.Builder<>(field, result, false));
            return result;
        }

        public MapMapper.Builder newMap(String name, String fetchMode) {
            Field field = getField(name);
            FetchMode mode = fetchMode(fetchMode, FetchMode.LAZY);
            MapMapper.Builder result = new MapMapper.Builder(
                    this, field, mode);
            fields.put(field, new FieldMapper.Builder<>(field, result, false));
            return result;
        }

        public ComponentMapper.Builder newComponent(
                String name, boolean nullable) {
            Field field = getField(name);
            ComponentMapper.Builder cm
                    = new ComponentMapper.Builder(field.getType());
            fields.put(field, new FieldMapper.Builder<>(field, cm, nullable));
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
            stmt.setUseGeneratedKeys(generatedKeys);
            return stmt;
        }

        public Statement.Builder newLoadStatement() {
            return load = newStatement(null);
        }

        public Statement.Builder newInsertStatement(boolean generatedKeys) {
            return insert = newStatement(null, generatedKeys);
        }

        public Statement.Builder newUpdateStatement() {
            return update = newStatement(null);
        }

        public Statement.Builder newDeleteStatement() {
            return delete = newStatement(null);
        }

        public void addQuery(String name, Statement.Builder stmt) {
            queries.put(name, stmt);
        }

        public void addStatement(String name, Statement.Builder stmt) {
            stmts.put(name, stmt);
        }

        public void setInsert(Statement.Builder stmt) {
            insert = stmt;
        }

        public Discriminator buildDiscriminator(BuildContext context) {
            return discriminator == null ? null : discriminator.build(context);
        }

        Class<?> getSuperClass() {
            return superClass;
        }

        void addSubclass(Class<?> subclass) {
            subclasses.add(subclass);
        }

        private ClassMapper[] buildSubclasses(Mapper mapper) {
            ClassMapper[] result = new ClassMapper[subclasses.size()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = mapper.getClassMapper(subclasses.get(i));
            }
            return result;
        }

        private FieldMapper<ColumnMapper>[] buildIdFields(BuildContext context) {
            FieldMapper<ColumnMapper>[] result = new FieldMapper[idFields.size()];
            int i = 0;
            for (Map.Entry<Field,FieldMapper.Builder<ColumnMapper>> e
                    : idFields.entrySet()) {
                result[i++] = e.getValue().build(context);
            }
            return result;
        }

        private FieldMapper<? extends ValueMapper>[] buildFields(
                BuildContext context) {
            FieldMapper<? extends ValueMapper>[] result
                    = new FieldMapper[fields.size()];
            int i = 0;
            for (Map.Entry<Field,FieldMapper.Builder<? extends ValueMapper>> e
                    : fields.entrySet()) {
                result[i++] = e.getValue().build(context);
            }
            return result;
        }

        private Map<String,Statement> buildQueries() {
            Map<String,Statement> result = new HashMap<>();
            for (Map.Entry<String,Statement.Builder> e: queries.entrySet()) {
                result.put(e.getKey(), e.getValue().build(null));
            }
            return result;
        }

        private Map<String,Statement> buildStatements() {
            Map<String,Statement> result = new HashMap<>();
            for (Map.Entry<String,Statement.Builder> e: stmts.entrySet()) {
                result.put(e.getKey(), e.getValue().build(null));
            }
            return result;
        }

        public Statement buildLoad(String[] fieldNames) {
            if (load == null) {
                return null;
            }
            load.setParamNames(fieldNames);
            return load.build(null);
        }

        public Statement buildInsert(String[] columns) {
            if (insert == null) {
                return null;
            }
            return insert.getUseGeneratedKeys()
                    ? insert.build(columns) : insert.build(null);
        }

        public Statement buildUpdate() {
            return update == null ? null : update.build(null);
        }

        public Statement buildDelete() {
            return delete == null ? null : delete.build(null);
        }

        @Override
        protected ClassMapper create(BuildContext context) {
            return new ClassMapper(context, this);
        }

        @Override
        protected void initialize(BuildContext context, ClassMapper vm) {
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
    }
}
