package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public abstract class ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ValueMapper.class.getName());

    protected final Class<?> type;

    protected ValueMapper(Builder<?> builder) {
        this.type = builder.type;
    }

    public abstract Object fromResultSet(Storage pm, Object obj,
            ResultSet rs);
    public Object fromResultSet(ResultSet rs, int ix) {
        throw new JedoException("Cannot get value from ResultSet");
    }
    public void fixForwardFields(Map<Class<?>, ClassMapper> map, Field field) {
    }

    void afterInsert(Storage pm, Object self, FieldMapper fm) {
    }
    void beforeDelete(Storage pm, Object self, FieldMapper fm) {
    }

    public static abstract class Builder<T extends ValueMapper> {
        protected final Class<?> type;

        protected Builder(Class<?> type) {
            this.type = type;
        }

        public abstract T build();
        public abstract void fixForwards(Map<Class<?>, ClassMapper.Builder> map);
    }
}
