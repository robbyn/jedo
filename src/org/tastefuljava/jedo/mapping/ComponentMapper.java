package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.util.ClassUtil;

public class ComponentMapper {
    private static final Logger LOG
            = Logger.getLogger(ComponentMapper.class.getName());

    private final Field field;
    private final PropertyMapper[] props;

    private ComponentMapper(Builder builder) {
        this.field = builder.field;
        this.props = builder.props.toArray(
                new PropertyMapper[builder.props.size()]);
    }

    public void collect(Object obj, ResultSet rs) {
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
            Object comp;
            if (allNull) {
                comp = null;
            } else {
                comp = field.getType().newInstance();
                for (int i = 0; i < props.length; ++i) {
                    props[i].setValue(comp, values[i]);
                }
            }
            field.set(obj, comp);
        } catch (IllegalArgumentException | IllegalAccessException
                | InstantiationException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public void writeTo(XMLWriter out) {
        out.startTag("component");
        out.attribute("name", field.getName());
        for (PropertyMapper prop: props) {
            prop.writeTo(out);
        }
        out.endTag();
    }

    public static class Builder {
        private final Field field;
        private final List<PropertyMapper> props = new ArrayList<>();

        public Builder(Field field) {
            this.field = field;
        }

        public void addProp(String field, String column) {
            props.add(newPropertyMapper(field, column));
        }

        public ComponentMapper getMapper() {
            return new ComponentMapper(this);
        }

        private PropertyMapper newPropertyMapper(String name, String column) {
            Field f = ClassUtil.getInstanceField(field.getType(), name);
            if (f == null) {
                throw new JedoException("Field " + name
                        + " not in class " + field.getType().getName());
            }
            return new PropertyMapper(f, column);
        }
    }
}
