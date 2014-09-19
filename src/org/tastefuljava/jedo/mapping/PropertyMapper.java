package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.conversion.Converter;

public class PropertyMapper extends FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(PropertyMapper.class.getName());

    private final String column;

    public PropertyMapper(Field field, String column) {
        super(field);
        this.column = column;
    }

    String getColumn() {
        return column;
    }

    @Override
    public Object fromResultSet(Connection cnt, Cache<Object,Object> cache,
            ResultSet rs) {
        try {
            return convert(rs.getObject(column));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get property value "
                    + field.getName());
        }
    }

    public Object fromResultSet(ResultSet rs) {
        return fromResultSet(null, null, rs);
    }

    public Object fromResultSet(ResultSet rs, int ix) {
        try {
            return convert(rs.getObject(ix));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get property value "
                    + field.getName());
        }
    }

    public final Object convert(Object value) {
        return Converter.convert(value, field.getType());
    }

    @Override
    public void writeTo(XMLWriter out) {
        out.startTag("property");
        out.attribute("name", field.getName());
        out.attribute("column", column);
        out.endTag();
    }
}
