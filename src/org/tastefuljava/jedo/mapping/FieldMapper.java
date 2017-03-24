package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.util.XMLWriter;

public abstract class FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(PropertyMapper.class.getName());

    protected final Field field;

    protected FieldMapper(Field field) {
        this.field = field;
    }

    String getFieldName() {
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

    public abstract Object fromResultSet(Connection cnt, Cache cache,
            ResultSet rs);
    public abstract void writeTo(XMLWriter out);

    void fixReferences(ClassMapper contClass, Map<Class<?>, ClassMapper> map) {
    }

    void afterInsert(Connection cnt, Cache cache, Object obj) {
    }
}
