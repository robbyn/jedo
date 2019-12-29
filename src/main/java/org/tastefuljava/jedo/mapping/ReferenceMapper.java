package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.rel.LazyRef;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.util.Reflection;

public class ReferenceMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ReferenceMapper.class.getName());

    private ClassMapper targetClass;
    private final String[] columns;
    private final FetchMode fetchMode;

    private ReferenceMapper(Builder builder) {
        super(builder);
        this.columns = builder.columns;
        this.fetchMode = builder.fetchMode;
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        try {
            boolean allNull = true;
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; ++i) {
                Object val = rs.getObject(columns[i]);
                values[i] = val;
                if (val != null) {
                    allNull = false;
                }
            }
            if (type != Ref.class) {
                return allNull
                        ? null : pm.loadFromId(targetClass, values);
            } else if (allNull || fetchMode == FetchMode.EAGER) {
                Object result = allNull
                        ? null : pm.loadFromId(targetClass, values);
                return new Ref<>(result);
            } else {
                return new LazyRef<>(pm, targetClass, values);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public static class Builder extends ValueMapper.Builder<ReferenceMapper> {
        private final String[] columns;
        private final FetchMode fetchMode;
        private final Class<?> refClass;

        public Builder(BuildContext context, Field field, String[] columns,
                FetchMode fetchMode) {
            super(context, field.getType());
            refClass = type == Ref.class
                    ? Reflection.getReferencedType(field) : type;
            this.columns = columns;
            this.fetchMode = fetchMode;
        }

        @Override
        public ReferenceMapper build() {
            ReferenceMapper rm = new ReferenceMapper(this);
            context.addForwardClassRef(refClass, (cm)->{
                rm.targetClass = cm;
            });
            return rm;
        }
    }
}
