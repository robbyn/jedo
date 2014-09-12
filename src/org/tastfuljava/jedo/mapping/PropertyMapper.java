package org.tastfuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertyMapper {
    private static final Logger LOG
            = Logger.getLogger(PropertyMapper.class.getName());

    private final Field field;
    private final String column;

    public PropertyMapper(Field field, String column) {
        this.field = field;
        this.column = column;
    }

    public Object getValue(Object object) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RuntimeException("Could not get property value "
                    + field.getName());
        }
    }
}
