package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import org.tastefuljava.jedo.util.ClassUtil;
import org.tastefuljava.jedo.cache.ObjectId;
import java.lang.reflect.Field;
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
    private final PropertyMapper[] idProps;
    private final FieldMapper[] fields;
    private final Map<String,Statement> queries;
    private final Statement load;
    private final Statement insert;
    private final Statement update;
    private final Statement delete;

    private ClassMapper(Builder builder) {
        this.clazz = builder.clazz;
        this.idProps = builder.idProps.toArray(
                new PropertyMapper[builder.idProps.size()]);
        this.fields = builder.fields.toArray(
                new FieldMapper[builder.fields.size()]);
        this.queries = builder.queries;
        this.load = builder.load;
        this.insert = builder.insert;
        this.update = builder.update;
        this.delete = builder.delete;
    }

    Class<?> getMappedClass() {
        return clazz;
    }

    public Object getId(Object obj) {
        if (idProps == null || idProps.length == 0) {
            return null;
        } else {
            Object[] values = new Object[idProps.length];
            for (int i = 0; i < idProps.length; ++i) {
                values[i] = idProps[i].getValue(obj);
            }
            return new ObjectId(clazz, values);
        }
    }

    public ObjectId newId(Object[] values) {
        if (values.length != idProps.length) {
            throw new JedoException("Wrong number of columns: expected "
                    + idProps.length + " found " + values.length);
        }
        for (int i = 0; i < idProps.length; ++i) {
            values[i] = idProps[i].convert(values[i]);
        }
        return new ObjectId(clazz, values);
    }

    public ObjectId getIdFromResultSet(ResultSet rs) {
        if (idProps == null || idProps.length == 0) {
            return null;
        } else {
            Object[] values = new Object[idProps.length];
            for (int i = 0; i < idProps.length; ++i) {
                values[i] = idProps[i].fromResultSet(rs);
            }
            return new ObjectId(clazz, values);
        }
    }

    public Object getInstance(Connection cnt, Cache<?,?> ucache, ResultSet rs) {
        Object oid = getIdFromResultSet(rs);
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>) ucache;
        Object obj = cache.get(oid);
        if (obj != null) {
            return obj;
        }
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        for (PropertyMapper prop: idProps) {
            prop.setValue(obj, prop.fromResultSet(cnt, cache, rs));
        }
        for (FieldMapper prop: fields) {
            prop.setValue(obj, prop.fromResultSet(cnt, cache, rs));
        }
        cache.put(oid, obj);
        return obj;
    }

    public Object load(Connection cnt, Cache<?,?> ucache, Object[] parms) {
        if (load == null) {
            throw new JedoException(
                    "No loader for class " + clazz.getName());
        }
        ObjectId oid = newId(parms);
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
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

    public List<Object> query(Connection cnt, Cache<?,?> ucache, String name,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        List<Object> result = new ArrayList<>();
        query(cnt, ucache, stmt, parms, result);
        return result;
    }

    public void query(Connection cnt, Cache<?,?> ucache, Statement stmt,
            Object[] parms, Collection<Object> result) {
        @SuppressWarnings(value = "unchecked")
        final Cache<Object, Object> cache = (Cache<Object, Object>) ucache;
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

    public void insert(Connection cnt, Cache<?,?> ucache, Object obj) {
        if (insert == null) {
            throw new JedoException(
                    "No inserter for " + clazz.getName());
        }
        try (final PreparedStatement stmt = insert.prepare(cnt, obj, null)) {
            stmt.executeUpdate();
            insert.collectKeys(stmt, idProps, obj);
            @SuppressWarnings("unchecked")
            Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
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

    public void delete(Connection cnt, Cache<?,?> ucache, Object obj) {
        if (delete == null) {
            throw new JedoException(
                    "No deleter for " + clazz.getName());
        }
        try (PreparedStatement stmt = delete.prepare(cnt, obj, null)) {
            stmt.executeUpdate();
            @SuppressWarnings("unchecked")
            Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
            cache.remove(getId(obj));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    void fixReferences(Map<Class<?>, ClassMapper> map) {
        for (FieldMapper fm: fields) {
            fm.fixReferences(map);
        }
    }

    Statement getQuery(String queryName) {
        return queries.get(queryName);
    }

    public void writeTo(XMLWriter out) {
        out.startTag("class");
        out.attribute("name", clazz.getName());
        if (idProps.length > 0) {
            out.startTag("id");
            for (PropertyMapper pm: idProps) {
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

    public static class Builder {
        private final Class<?> clazz;
        private List<PropertyMapper> idProps = new ArrayList<>();
        private List<FieldMapper> fields = new ArrayList<>();
        private final Map<String,Statement> queries = new HashMap<>();
        private Statement load;
        private Statement insert;
        private Statement update;
        private Statement delete;

        public Builder(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Builder(String packageName, String className) {
            this(loadClass(packageName, className));
        }

        public Class<?> getMappedClass() {
            return clazz;
        }

        public void addIdProp(String field, String column) {
            idProps.add(newProperty(field, column));
        }

        public void addProp(String field, String column) {
            fields.add(newProperty(field, column));
        }

        public void addReference(String field, String[] columns) {
            ReferenceMapper ref = newReference(field, columns);
            fields.add(ref);
        }

        public void addCollection(String field, String query,
                String[] columns) {
            CollectionMapper ref = newReference(field, query, columns);
            fields.add(ref);
        }

        public ComponentMapper.Builder newComponent(String name) {
            return new ComponentMapper.Builder(
                    ClassUtil.getInstanceField(clazz, name));
        }

        public void addComponent(ComponentMapper cm) {
            fields.add(cm);
        }

        public Statement.Builder newStatement(String[] paramNames) {
            return new Statement.Builder(clazz, paramNames);
        }

        public Statement.Builder newInsertStatement(boolean generatedKeys) {
            Statement.Builder stmt = new Statement.Builder(clazz, null);
            if (generatedKeys) {
                stmt.setGeneratedKeys(getIdColumns());
            }
            return stmt;
        }

        public Statement.Builder newLoadStatement() {
            return newStatement(getIdFieldNames());
        }

        public void addQuery(String name, Statement stmt) {
            queries.put(name, stmt);
        }

        public void setLoad(Statement stmt) {
            load = stmt;
        }

        public void setInsert(Statement stmt) {
            insert = stmt;
        }

        public void setUpdate(Statement stmt) {
            update = stmt;
        }

        public void setDelete(Statement stmt) {
            delete = stmt;
        }

        ClassMapper getMapper() {
            return new ClassMapper(this);
        }

        private PropertyMapper newProperty(String name, String column) {
            Field field = ClassUtil.getInstanceField(clazz, name);
            if (field == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new PropertyMapper(field, column);
        }

        private ReferenceMapper newReference(String name, String[] columns) {
            Field field = ClassUtil.getInstanceField(clazz, name);
            if (field == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new ReferenceMapper(field, columns);
        }

        private CollectionMapper newReference(String name, String query,
                String[] columns) {
            Field field = ClassUtil.getInstanceField(clazz, name);
            if (field == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new CollectionMapper(field, query, columns);
        }

        private String[] getIdFieldNames() {
            String[] result = new String[idProps.size()];
            for (int i = 0; i < idProps.size(); ++i) {
                result[i] = idProps.get(i).getFieldName();
            }
            return result;
        }

        private String[] getIdColumns() {
            String[] result = new String[idProps.size()];
            for (int i = 0; i < idProps.size(); ++i) {
                result[i] = idProps.get(i).getColumn();
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
