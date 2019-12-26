package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.util.XMLWriter;

public abstract class FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(SimpleFieldMapper.class.getName());

    protected final Field field;

    protected FieldMapper(Field field) {
        this.field = field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Object getValue(Object object) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get field value "
                    + field.getName());
        }
    }

    public void setValue(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not set field value "
                    + field.getName());
        }
    }

    public void fixForwards(Map<Class<?>, ClassMapper> map) {
    }

    public abstract Object fromResultSet(Storage pm, Object obj,
            ResultSet rs);
    public abstract void writeTo(XMLWriter out);

    void afterInsert(Storage pm, Object obj) {
    }

    void beforeUpdate(Storage pm, Object self) {
    }

    void afterUpdate(Storage pm, Object self) {
    }

    void beforeDelete(Storage pm, Object self) {
    }

    public static abstract class Builder<T extends FieldMapper> {
        protected final Field field;

        protected Builder(Field field) {
            this.field = field;
        }

        public String getFieldName() {
            return field.getName();
        }

        public abstract T build();
        public abstract void fixForwards(Map<Class<?>, ClassMapper.Builder> map);
    }
}
