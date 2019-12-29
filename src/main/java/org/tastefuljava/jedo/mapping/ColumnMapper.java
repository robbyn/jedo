package org.tastefuljava.jedo.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.conversion.Converter;

public class ColumnMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ColumnMapper.class.getName());

    private final String column;

    private ColumnMapper(Builder builder) {
        super(builder);
        this.column = builder.column;
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
            throw new JedoException(
                    "Could not get column " + column + " from ResultSet");
        }
    }

    public final Object convert(Object value) {
        return Converter.convert(value, type);
    }

    public static class Builder extends ValueMapper.Builder<ColumnMapper> {
        private final String column;

        public Builder(BuildContext context, Class<?> type, String column) {
            super(context, type);
            this.column = column;
        }

        public String getColumn() {
            return column;
        }

        @Override
        public void fixForwards(Map<Class<?>, ClassMapper.Builder> map) {
        }

        @Override
        public ColumnMapper build() {
            return new ColumnMapper(this);
        }
    }
}
