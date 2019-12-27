package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.SortedSet;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.LazyCollection;
import org.tastefuljava.jedo.rel.LazySet;
import org.tastefuljava.jedo.rel.LazySortedSet;

public class SetMapper extends CollectionMapper {
    private final Field[] orderFields;

    public SetMapper(Builder builder) {
        super(builder);
        this.orderFields = builder.orderFields;
    }

    @Override
    protected LazyCollection<Object> newCollection(Storage pm, Object parent) {
        if (orderFields != null && orderFields.length > 0
                && field.getType().isAssignableFrom(LazySortedSet.class)) {
            return new LazySortedSet<>(pm, this, parent, orderFields);
        } else if (field.getType().isAssignableFrom(LazySet.class)) {
            return new LazySet<>(pm, this, parent);
        } else {
            throw new JedoException("Unsupported set field type "
                    + field.getType().getName());
        }
    }

    public static class Builder extends CollectionMapper.Builder {
        private final Field[] orderFields;

        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode, Field[] orderFields) {
            super(parentClass, field, fetchMode);
            this.orderFields = orderFields;
        }

        @Override
        public CollectionMapper build() {
            return new SetMapper(this);
        }
    }
}
