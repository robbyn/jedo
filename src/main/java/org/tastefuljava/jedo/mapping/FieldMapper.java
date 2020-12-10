package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class FieldMapper<V extends ValueMapper> implements ValueAccessor {
    private static final Logger LOG
            = Logger.getLogger(FieldMapper.class.getName());
    
    private final Field field;
    private final V vm;
    private final boolean nullable;

    public FieldMapper(Field field, V vm, boolean nullable) {
        this.field = field;
        this.vm = vm;
        this.nullable = nullable;
    }

    @Override
    public Object getValue(Object object) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get field value " + field.getName());
        }
    }

    @Override
    public void setValue(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not set field value " + field.getName());
        }
    }

    Field getField() {
        return field;
    }

    public V getValueMapper() {
        return vm;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getFieldName() {
        return field.getName();
    }

    Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        return vm.fromResultSet(pm, obj, rs, this);
    }

    void setFromResultSet(Storage pm, Object self, ResultSet rs) {
        setValue(self, vm.fromResultSet(pm, self, rs, this));
    }

    void afterInsert(Storage pm, Object self) {
        vm.afterInsert(pm, self, this);
    }

    void beforeDelete(Storage pm, Object self) {
        vm.beforeDelete(pm, self, this);
    }

    void beforeUpdate(Storage pm, Object self) {
    }

    void afterUpdate(Storage pm, Object self) {
    }

    public static class Builder<V extends ValueMapper> {
        private final Field field;
        private final V.Builder vm;
        private final boolean nullable;

        public Builder(Field field, V.Builder vm, boolean nullable) {
            this.field = field;
            this.vm = vm;
            this.nullable = nullable;
        }

        public FieldMapper<V> build(BuildContext context) {
            return new FieldMapper(field, vm.create(context), nullable);
        }
    }
}
