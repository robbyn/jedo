package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.util.Reflection;

public class ComponentMapper extends FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(ComponentMapper.class.getName());

    private final SimpleFieldMapper[] props;

    private ComponentMapper(Builder builder) {
        super(builder.field);
        this.props = builder.buildProps();
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        try {
            boolean allNull = true;
            Object[] values = new Object[props.length];
            for (int i = 0; i < props.length; ++i) {
                Object value = props[i].fromResultSet(rs);
                values[i] = value;
                if (value != null) {
                    allNull = false;
                }
            }
            if (allNull) {
                return null;
            } else {
                Object comp = field.getType().getConstructor().newInstance();
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

    @Override
    public void writeTo(XMLWriter out) {
        out.startTag("component");
        out.attribute("name", field.getName());
        for (SimpleFieldMapper prop: props) {
            prop.writeTo(out);
        }
        out.endTag();
    }

    public static class Builder extends FieldMapper.Builder<ComponentMapper> {
        private final List<SimpleFieldMapper.Builder> props = new ArrayList<>();

        public Builder(Field field) {
            super(field);
        }

        public void addProp(String field, String column) {
            props.add(newPropertyMapper(field, column));
        }

        public ComponentMapper getMapper() {
            return new ComponentMapper(this);
        }

        private SimpleFieldMapper.Builder newPropertyMapper(String name,
                String column) {
            Field f = Reflection.getInstanceField(field.getType(), name);
            if (f == null) {
                throw new JedoException("Field " + name
                        + " not in class " + field.getType().getName());
            }
            return new SimpleFieldMapper.Builder(f, column);
        }

        private SimpleFieldMapper[] buildProps() {
            SimpleFieldMapper[] result = new SimpleFieldMapper[props.size()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = props.get(i).build();
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
