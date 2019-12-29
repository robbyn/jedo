package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.util.Reflection;

public class ComponentMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ComponentMapper.class.getName());

    private final FieldMapper<ColumnMapper>[] props;

    private ComponentMapper(Builder builder) {
        super(builder);
        this.props = builder.buildProps();
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        try {
            boolean allNull = true;
            Object[] values = new Object[props.length];
            for (int i = 0; i < props.length; ++i) {
                Object value = props[i].fromResultSet(null, null, rs);
                values[i] = value;
                if (value != null) {
                    allNull = false;
                }
            }
            if (allNull) {
                return null;
            } else {
                Object comp = type.getConstructor().newInstance();
                for (int i = 0; i < props.length; ++i) {
                    props[i].setValue(comp, values[i]);
                }
                return comp;
            }
        } catch (IllegalAccessException | InstantiationException
                | NoSuchMethodException | SecurityException
                | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public static class Builder extends ValueMapper.Builder<ComponentMapper> {
        private final Map<Field,ColumnMapper.Builder> props
                = new LinkedHashMap<>();

        public Builder(Class<?> type) {
            super(type);
        }

        public void addProp(String field, String column) {
            Field f = Reflection.getInstanceField(type, field);
            if (f == null) {
                throw new JedoException("Field " + field + " not in class "
                        + type.getName());
            }
            props.put(f, new ColumnMapper.Builder(f.getType(), column));
        }

        public ComponentMapper getMapper() {
            return new ComponentMapper(this);
        }

        private FieldMapper<ColumnMapper>[] buildProps() {
            FieldMapper<ColumnMapper>[] result = new FieldMapper[props.size()];
            int i = 0;
            for (Map.Entry<Field,ColumnMapper.Builder> e: props.entrySet()) {
                result[i++] = new FieldMapper<>(e.getKey(), e.getValue().build());
            }
            return result;
        }

        @Override
        public void fixForwards(Map<Class<?>, ClassMapper.Builder> map) {
        }

        @Override
        public ComponentMapper build() {
            return new ComponentMapper(this);
        }
    }
}
