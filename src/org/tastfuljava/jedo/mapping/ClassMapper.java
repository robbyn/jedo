package org.tastfuljava.jedo.mapping;

import org.tastfuljava.jedo.cache.ObjectId;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastfuljava.jedo.cache.Cache;

public class ClassMapper {
    private static final Logger LOG
            = Logger.getLogger(ClassMapper.class.getName());

    private final Class<?> clazz;
    private final PropertyMapper[] idProps;
    private final PropertyMapper[] props;
    private final Map<String,Statement> queries;
    private final Statement load;
    private final Statement insert;
    private final Statement update;
    private final Statement delete;

    private ClassMapper(Builder builder) {
        this.clazz = builder.clazz;
        this.idProps = builder.idProps.toArray(
                new PropertyMapper[builder.idProps.size()]);
        this.props = builder.props.toArray(
                new PropertyMapper[builder.props.size()]);
        this.queries = builder.queries;
        this.load = builder.load;
        this.insert = builder.insert;
        this.update = builder.update;
        this.delete = builder.delete;
    }

    public Class<?> getMappedClass() {
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

    public Object getIdFromResultSet(ResultSet rs) throws SQLException {
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

    public Object getInstance(Cache<Object,Object> cache, ResultSet rs)
            throws SQLException {
        Object id = getIdFromResultSet(rs);
        Object obj = cache.get(id);
        if (obj != null) {
            return obj;
        }
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        for (PropertyMapper prop: idProps) {
            prop.setValue(obj, prop.fromResultSet(rs));
        }
        for (PropertyMapper prop: props) {
            prop.setValue(obj, prop.fromResultSet(rs));
        }
        return obj;
    }

    public Object load(Connection cnt, Cache cache, Object[] parms)
            throws SQLException {
        if (load == null) {
            throw new IllegalArgumentException(
                    "No loader for class " + clazz.getName());
        }
        ObjectId oid = new ObjectId(clazz, parms);
        Object obj = cache.get(oid);
        if (obj == null) {
            obj = load.queryOne(cnt, this, cache, parms);
            cache.put(oid, obj);
        }
        return obj;
    }

    public Object queryOne(Connection cnt, Cache cache, String name,
            Object[] parms) throws SQLException {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new IllegalArgumentException("No query named " + name);
        }
        return stmt.queryOne(cnt, this, cache, parms);
    }

    public void insert(Connection cnt, Cache cache, Object obj)
            throws SQLException {
        if (insert == null) {
            throw new IllegalArgumentException(
                    "No inserter for " + clazz.getName());
        }
        insert.update(cnt, this, obj);
        cache.put(getId(obj), obj);
    }

    public void update(Connection cnt, Cache cache, Object obj)
            throws SQLException {
        if (update == null) {
            throw new IllegalArgumentException(
                    "No inserter for " + clazz.getName());
        }
        update.update(cnt, this, obj);
    }

    public void delete(Connection cnt, Cache cache, Object obj)
            throws SQLException {
        if (delete == null) {
            throw new IllegalArgumentException(
                    "No inserter for " + clazz.getName());
        }
        delete.update(cnt, this, obj);
        cache.remove(getId(obj));
    }

    public void getGeneratedKeys(ResultSet rs, Object obj) throws SQLException {
        for (PropertyMapper prop: idProps) {
            prop.setValue(obj, prop.fromResultSet(rs));
        }
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
            for (PropertyMapper pm: props) {
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
        private List<PropertyMapper> props = new ArrayList<>();
        private final Map<String,Statement> queries = new HashMap<>();
        private Statement load;
        private Statement insert;
        private Statement update;
        private Statement delete;

        public Builder(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Builder(String packageName, String className)
                throws ClassNotFoundException {
            this(loadClass(packageName, className));
        }

        public void addIdProp(String field, String column) {
            idProps.add(newPropertyMapper(field, column));
        }

        public void addProp(String field, String column) {
            props.add(newPropertyMapper(field, column));
        }

        public Statement.Builder newStatementBuilder(String[] paramNames) {
            return new Statement.Builder(clazz, paramNames);
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

        public ClassMapper getMapper() {
            return new ClassMapper(this);
        }

        private PropertyMapper newPropertyMapper(String name, String column) {
            Field field = ClassUtil.getInstanceField(clazz, name);
            return new PropertyMapper(field, column);
        }

        private static Class<?> loadClass(String packageName,
                String className) throws ClassNotFoundException {
            String fullName = packageName == null
                    ? className : packageName + "." + className;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return cl.loadClass(fullName);
        }
    }

}
