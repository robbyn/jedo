package org.tastfuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastfuljava.jedo.transaction.WeakCache;

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
        } else if (idProps.length == 1) {
            return idProps[0].getValue(obj);
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
        } else if (idProps.length == 1) {
            return idProps[0].fromResultSet(rs);
        } else {
            Object[] values = new Object[idProps.length];
            for (int i = 0; i < idProps.length; ++i) {
                values[i] = idProps[i].fromResultSet(rs);
            }
            return new ObjectId(clazz, values);
        }
    }

    public Object getInstance(WeakCache<Object,Object> cache, ResultSet rs)
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

        public void addStatement(String type, String name,
                String[] paramNames, String sql) {
            Statement stmt = Statement.parse(sql, clazz, paramNames);
            switch (type) {
                case "query":
                    queries.put(name, stmt);
                    break;
                case "load":
                    load = stmt;
                    break;
                case "insert":
                    insert = stmt;
                    break;
                case "update":
                    update = stmt;
                    break;
                case "delete":
                    delete = stmt;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid statement type " + type);
            }
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

    private static class ObjectId {
        private final Class<?> clazz;
        private final Object[] values;

        private ObjectId(Class<?> clazz, Object[] values) {
            if (values == null || values.length < 1) {
                throw new IllegalArgumentException(
                        "At least one value is needed");
            }
            this.clazz = clazz;
            this.values = values;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(clazz.getName());
            buf.append('[');
            buf.append(values[0]);
            for (int i = 1; i < values.length; ++i) {
                buf.append(',');
                buf.append(values[i]);
            }
            buf.append(']');
            return buf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !(obj instanceof ObjectId)) {
                return false;
            }
            ObjectId other = (ObjectId)obj;
            if (other.clazz != clazz) {
                return false;
            }
            if (other.values.length != values.length) {
                return false;
            }
            for (int i = 0; i < values.length; ++i) {
                if (!values[i].equals(other.values[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int h = clazz.hashCode();
            for (Object value: values) {
                h = 37*h + value.hashCode();
            }
            return h;
        }
    }
    }
