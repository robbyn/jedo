package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
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
                && type.isAssignableFrom(LazySortedSet.class)) {
            return new LazySortedSet<>(pm, this, parent, orderFields);
        } else if (type.isAssignableFrom(LazySet.class)) {
            return new LazySet<>(pm, this, parent);
        } else {
            throw new JedoException("Unsupported set field type "
                    + type.getName());
        }
    }

    public static class Builder extends CollectionMapper.Builder {
        private final Field[] orderFields;

        public Builder(BuildContext context, ClassMapper.Builder parentClass,
                Field field, FetchMode fetchMode, Field[] orderFields) {
            super(context, parentClass, field, fetchMode);
            this.orderFields = orderFields;
        }

        @Override
        public CollectionMapper build() {
            SetMapper result = new SetMapper(this);
            postBuild(result);
            return result;
        }
    }
}
