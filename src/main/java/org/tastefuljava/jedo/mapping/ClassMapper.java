package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.rel.ObjectId;
import org.tastefuljava.jedo.util.XMLWriter;
import org.tastefuljava.jedo.util.Reflection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;

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

    Class<?> getMappedClass() {
        return clazz;
    }

    public ObjectId getId(Object obj) {
        if (idFields == null || idFields.length == 0) {
            return null;
        } else {
            return new ObjectId(clazz, getIdValues(obj));
        }
    }

    Object[] getIdValues(Object obj) {
        Object[] values = new Object[idFields.length];
        for (int i = 0; i < idFields.length; ++i) {
            values[i] = idFields[i].getValue(obj);
        }
        return values;
    }

    Object[] getIdValuesFromResultSet(ResultSet rs) {
        Object[] values = new Object[idFields.length];
        for (int i = 0; i < idFields.length; ++i) {
            values[i] = idFields[i].fromResultSet(rs);
        }
        return values;
    }

    public ObjectId getIdFromResultSet(ResultSet rs) {
        if (idFields == null || idFields.length == 0) {
            return null;
        } else {
            return new ObjectId(clazz, getIdValuesFromResultSet(rs));
        }
    }

    public Object getInstance(Connection cnt, Cache cache, ResultSet rs) {
        ObjectId oid = getIdFromResultSet(rs);
        Object obj;
        if (oid != null) {
            obj = cache.get(oid);
            if (obj != null) {
                return obj;
            }
        }
        try {
            obj = clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | NoSuchMethodException | SecurityException
                | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
        for (SimpleFieldMapper field: idFields) {
            field.setValue(obj, field.fromResultSet(cnt, cache, obj, rs));
        }
        if (oid != null) {
            cache.put(oid, obj);
        }
        for (FieldMapper field: fields) {
            field.setValue(obj, field.fromResultSet(cnt, cache, obj, rs));
        }
        return obj;
    }

    public Object load(Connection cnt, Cache cache, Object[] parms) {
        if (load == null) {
            throw new JedoException(
                    "No loader for class " + clazz.getName());
        }
        ObjectId oid = newId(parms);
        Object obj = cache.get(oid);
        if (obj == null) {
            obj = queryOne(load, cnt, cache, parms);
        }
        return obj;
    }

    public Object queryOne(Connection cnt, Cache cache, String name,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        return queryOne(stmt, cnt, cache, parms);
    }

    private Object queryOne(Statement stmt, Connection cnt, Cache cache,
            Object[] parms) {
        Object result = null;
        try (PreparedStatement pstmt = stmt.prepare(cnt, null, parms);
                ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                result = getInstance(cnt, cache, rs);
                if (rs.next()) {
                    throw new JedoException("Only one result allowed");
                }
            }
        } catch (final SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
        return result;
    }

    public List<Object> query(Connection cnt, Cache cache, String name,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        List<Object> result = new ArrayList<>();
        query(cnt, cache, stmt, parms, result);
        return result;
    }

    public void query(Connection cnt, Cache cache, Statement stmt,
            Object[] parms, Collection<Object> result) {
        try (PreparedStatement pstmt = stmt.prepare(cnt, null, parms);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(getInstance(cnt, cache, rs));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }        
    }

    public void invoke(Connection cnt, Cache cache, String name,
            Object[] parms) {
        Statement stmt = stmts.get(name);
        if (stmt == null) {
            throw new JedoException(
                    "No statement " + name + " for " + clazz.getName());
        }
        stmt.executeUpdate(cnt, null, parms);
    }

    public void insert(Connection cnt, Cache cache, Object obj) {
        if (insert == null) {
            throw new JedoException(
                    "No inserter for " + clazz.getName());
        }
        insert(cnt, cache, insert, obj, null);
    }

    public void insert(Connection cnt, Cache cache, Statement stmt, Object obj,
            Object[] parms) {
        try (PreparedStatement pstmt = stmt.prepare(cnt, obj, parms)) {
            pstmt.executeUpdate();
            collectKeys(stmt, pstmt, obj, cache);
            for (FieldMapper prop: fields) {
                prop.afterInsert(cnt, cache, obj);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public void collectKeys(Statement statement, PreparedStatement stmt,
            Object obj, Cache cache) {
        try {
            statement.collectKeys(stmt, idFields, obj);
            cache.put(getId(obj), obj);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public void update(Connection cnt, Cache cache, Object obj) {
        if (update == null) {
            throw new JedoException(
                    "No updater for " + clazz.getName());
        }
        try (final PreparedStatement stmt = update.prepare(cnt, obj, null)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public void delete(Connection cnt, Cache cache, Object obj) {
        if (delete == null) {
            throw new JedoException(
                    "No deleter for " + clazz.getName());
        }
        try (PreparedStatement stmt = delete.prepare(cnt, obj, null)) {
            stmt.executeUpdate();
            cache.remove(getId(obj));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
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

    private ObjectId newId(Object[] values) {
        if (values.length != idFields.length) {
            throw new JedoException("Wrong number of columns: expected "
                    + idFields.length + " found " + values.length);
        }
        for (int i = 0; i < idFields.length; ++i) {
            values[i] = idFields[i].convert(values[i]);
        }
        return new ObjectId(clazz, values);
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

        public Builder(String packageName, String className) {
            this(loadClass(packageName, className));
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
            return new CollectionMapper.Builder(field,
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
            return Builder.this.newStatement(getIdFieldNames());
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

        private String[] getIdColumns() {
            String[] result = new String[idField.size()];
            for (int i = 0; i < idField.size(); ++i) {
                result[i] = idField.get(i).getColumn();
            }
            return result;
        }

        private static Class<?> loadClass(String packageName,
                String className) {
            try {
                String fullName = packageName == null
                        ? className : packageName + "." + className;
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                return cl.loadClass(fullName);
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
                throw new JedoException(ex.getMessage());
            }
        }
    }
}
