package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class ComponentMapper extends ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ComponentMapper.class.getName());

    private final FieldMapper<ColumnMapper>[] fields;
    private final Map<String,FieldMapper<ColumnMapper>> fieldMap
            = new HashMap<>();

    private ComponentMapper(BuildContext context, Builder builder) {
        super(builder);
        this.fields = builder.buildFields(context);
        for (FieldMapper<ColumnMapper> fm: fields) {
            fieldMap.put(fm.getFieldName(), fm);
        }
    }

    public FieldMapper<ColumnMapper>[] getFields() {
        return fields.clone();
    }

    public FieldMapper<? extends ValueMapper> getField(String name) {
        return fieldMap.get(name);
    }

    @Override
    public <T> T accept(ValueMapperVisitor<T> vtor) {
        return vtor.visitComponentMapper(this);
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs,
            ValueAccessor fm) {
        try {
            boolean allNull = true;
            Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; ++i) {
                Object value = fields[i].fromResultSet(null, null, rs);
                values[i] = value;
                if (value != null) {
                    allNull = false;
                }
            }
            if (allNull) {
                return null;
            } else {
                Object comp = type.getConstructor().newInstance();
                for (int i = 0; i < fields.length; ++i) {
                    fields[i].setValue(comp, values[i]);
                }
                return comp;
            }
        } catch (IllegalAccessException | InstantiationException
                | NoSuchMethodException | SecurityException
                | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    void afterInsert(Storage pm, Object self, ValueAccessor fm) {
        Object comp = fm.getValue(self);
        if (comp != null) {
            for (FieldMapper<ColumnMapper> field: fields) {
                field.afterInsert(pm, comp);
            }
        }
    }

    @Override
    void beforeDelete(Storage pm, Object self, ValueAccessor fm) {
        Object comp = fm.getValue(self);
        if (comp != null) {
            for (FieldMapper<ColumnMapper> field: fields) {
                field.beforeDelete(pm, comp);
            }
        }
    }

    public static class Builder extends ValueMapper.Builder<ComponentMapper> {
        private final Map<Field,FieldMapper.Builder<ColumnMapper>>
                fields = new LinkedHashMap<>();

        public Builder(Class<?> type) {
            super(type);
        }

        public void addProp(String name, String column, boolean nullable) {
            Field field = getField(name);
            ColumnMapper.Builder vm = new ColumnMapper.Builder(
                    field.getType(), column);
            fields.put(field, new FieldMapper.Builder<>(field, vm, nullable));
        }

        private FieldMapper<ColumnMapper>[] buildFields(BuildContext context) {
            FieldMapper<ColumnMapper>[] result = new FieldMapper[fields.size()];
            int i = 0;
            for (Map.Entry<Field,FieldMapper.Builder<ColumnMapper>> e
                    : fields.entrySet()) {
                result[i++] = e.getValue().build(context);
            }
            return result;
        }

        @Override
        protected ComponentMapper create(BuildContext context) {
            return new ComponentMapper(context, this);
        }
    }
}
