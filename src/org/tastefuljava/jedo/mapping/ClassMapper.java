package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.ClassUtil;
import org.tastefuljava.jedo.cache.ObjectId;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
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

    public Object getIdFromResultSet(ResultSet rs) {
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

    public Object getInstance(Cache<?,?> ucache, ResultSet rs) {
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>) ucache;
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
        cache.put(id, obj);
        return obj;
    }

    public Object load(Connection cnt, Cache<?,?> ucache, Object[] parms) {
        if (load == null) {
            throw new JedoException(
                    "No loader for class " + clazz.getName());
        }
        ObjectId oid = new ObjectId(clazz, parms);
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
        Object obj = cache.get(oid);
        if (obj == null) {
            obj = load.queryOne(cnt, this, cache, parms);
            cache.put(oid, obj);
        }
        return obj;
    }

    public Object queryOne(Connection cnt, Cache cache, String name,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        return stmt.queryOne(cnt, this, cache, parms);
    }

    public List<Object> query(Connection cnt, Cache<?,?> cache, String name,
            Object[] parms) {
        Statement stmt = queries.get(name);
        if (stmt == null) {
            throw new JedoException("No query named " + name);
        }
        return stmt.query(cnt, this, cache, parms);
    }

    public void insert(Connection cnt, Cache<?,?> ucache, Object obj) {
        if (insert == null) {
            throw new JedoException(
                    "No inserter for " + clazz.getName());
        }
        insert.update(cnt, this, obj);
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
        cache.put(getId(obj), obj);
    }

    public void update(Connection cnt, Cache cache, Object obj) {
        if (update == null) {
            throw new JedoException(
                    "No updater for " + clazz.getName());
        }
        update.update(cnt, this, obj);
    }

    public void delete(Connection cnt, Cache<?,?> ucache, Object obj) {
        if (delete == null) {
            throw new JedoException(
                    "No deleter for " + clazz.getName());
        }
        delete.update(cnt, this, obj);
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
        cache.remove(getId(obj));
    }

    public void getGeneratedKeys(ResultSet rs, Object obj) {
        int ix = 0;
        for (PropertyMapper prop: idProps) {
            prop.setValue(obj, prop.fromResultSet(rs, ++ix));
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

        public ClassMapper getMapper() {
            return new ClassMapper(this);
        }

        private PropertyMapper newPropertyMapper(String name, String column) {
            Field field = ClassUtil.getInstanceField(clazz, name);
            return new PropertyMapper(field, column);
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
                result[i] = idProps.get(i).getFieldName();
            }
            return result;
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
