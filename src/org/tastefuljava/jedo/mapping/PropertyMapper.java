package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.conversion.Converter;

public class PropertyMapper {
    private static final Logger LOG
            = Logger.getLogger(PropertyMapper.class.getName());

    private final Field field;
    private final String column;

    public PropertyMapper(Field field, String column) {
        this.field = field;
        this.column = column;
    }

    String getFieldName() {
        return field.getName();
    }

    public Object getValue(Object object) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get property value "
                    + field.getName());
        }
    }

    public void setValue(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not set property value "
                    + field.getName());
        }
    }

    public Object fromResultSet(ResultSet rs) throws SQLException {
        return Converter.convert(rs.getObject(column), field.getType());
    }

    void writeTo(XMLWriter out) {
        out.startTag("property");
        out.attribute("name", field.getName());
        out.attribute("column", column);
        out.endTag();
    }
}
