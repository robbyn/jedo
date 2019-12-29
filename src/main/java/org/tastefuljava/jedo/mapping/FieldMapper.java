package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class FieldMapper<V extends ValueMapper> {
    private static final Logger LOG
            = Logger.getLogger(FieldMapper.class.getName());
    
    private final Field field;
    private final ValueMapper vm;

    public FieldMapper(Field field, V vm) {
        this.field = field;
        this.vm = vm;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Object getValue(Object object) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not get field value " + field.getName());
        }
    }

    public void setValue(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Could not set field value " + field.getName());
        }
    }

    public void fixForwards(Map<Class<?>, ClassMapper> map) {
        vm.fixForwardFields(map, field);
    }

    Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        return vm.fromResultSet(pm, obj, rs);
    }

    void setFromResultSet(Storage pm, Object obj, ResultSet rs) {
        setValue(obj, vm.fromResultSet(pm, obj, rs));
    }

    void setFromResultSet(Object obj, ResultSet rs, int i) {
        setValue(obj, vm.fromResultSet(rs, i));
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
}
