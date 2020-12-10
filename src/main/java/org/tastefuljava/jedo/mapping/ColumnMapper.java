package org.tastefuljava.jedo.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.conversion.Conversion;

public class ColumnMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ColumnMapper.class.getName());

    private final String column;

    private ColumnMapper(BuildContext context, Builder builder) {
        super(builder);
        this.column = builder.column;
    }

    @Override
    public <T> T accept(ValueMapperVisitor<T> vtor) {
        return vtor.visitColumnMapper(this);
    }

    public String getColumn() {
        return column;
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs,
            ValueAccessor fm) {
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
        return Conversion.convert(value, type);
    }

    public static class Builder extends ValueMapper.Builder<ColumnMapper> {
        private final String column;

        public Builder(Class<?> type, String column) {
            super(type);
            this.column = column;
        }

        public String getColumn() {
            return column;
        }

        @Override
        protected ColumnMapper create(BuildContext context) {
            return new ColumnMapper(context, this);
        }
    }
}
