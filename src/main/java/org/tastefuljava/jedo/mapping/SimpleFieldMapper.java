package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.conversion.Converter;

public class SimpleFieldMapper extends FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(SimpleFieldMapper.class.getName());

    private final String column;

    private SimpleFieldMapper(Builder builder) {
        super(builder.field);
        this.column = builder.column;
    }

    String getColumn() {
        return column;
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        return fromResultSet(rs);
    }

    public Object fromResultSet(ResultSet rs) {
        try {
            return convert(rs.getObject(column));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get property value "
                    + field.getName());
        }
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

    public static class Builder extends FieldMapper.Builder<SimpleFieldMapper> {
        private final String column;

        public Builder(Field field, String column) {
            super(field);
            this.column = column;
        }

        public String getColumn() {
            return column;
        }

        @Override
        public void fixForwards(Map<Class<?>, ClassMapper.Builder> map) {
        }

        @Override
        public SimpleFieldMapper build() {
            return new SimpleFieldMapper(this);
        }
    }
}
