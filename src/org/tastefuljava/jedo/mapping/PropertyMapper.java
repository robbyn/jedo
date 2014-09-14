package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public void setValue(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RuntimeException("Could not set property value "
                    + field.getName());
        }
    }

    public Object fromResultSet(ResultSet rs) throws SQLException {
        return rs.getObject(column, field.getType());
    }

    void writeTo(XMLWriter out) {
        out.startTag("property");
        out.attribute("name", field.getName());
        out.attribute("column", column);
        out.endTag();
    }
}
