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
import org.tastefuljava.jedo.util.XMLWriter;

public class ReferenceMapper extends FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(ReferenceMapper.class.getName());

    private ClassMapper targetClass;
    private final String[] columns;
    private final FetchMode fetchMode;

    private ReferenceMapper(Builder builder) {
        super(builder.field);
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
            if (field.getType() != Ref.class) {
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

    @Override
    public void fixForwards(Map<Class<?>, ClassMapper> map) {
        Class<?> type = field.getType();
        if (type == Ref.class) {
            type = Reflection.getReferencedType(field);
        }
        targetClass = map.get(type);
        if (targetClass == null) {
            throw new JedoException("Unresolved reference target class: "
                    + field.getType().getName());
        }
    }

    @Override
    public void writeTo(XMLWriter out) {
        out.startTag("reference");
        out.attribute("name", getFieldName());
        out.attribute("column", columnList());
        out.endTag();
    }

    private String columnList() {
        StringBuilder buf = new StringBuilder();
        if (columns.length > 0) {
            buf.append(columns[0]);
            for (int i = 1; i < columns.length; ++i) {
                buf.append(',');
                buf.append(columns[i]);
            }
        }
        return buf.toString();
    }

    public static class Builder extends FieldMapper.Builder<ReferenceMapper> {
        private final String[] columns;
        private final FetchMode fetchMode;

        public Builder(Field field, String[] columns, FetchMode fetchMode) {
            super(field);
            this.columns = columns;
            this.fetchMode = fetchMode;
        }

        @Override
        public ReferenceMapper build() {
            return new ReferenceMapper(this);
        }
    }
}
