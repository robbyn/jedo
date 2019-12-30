package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.JedoCollection;
import org.tastefuljava.jedo.rel.JedoSet;
import org.tastefuljava.jedo.rel.JedoSortedSet;

public class SetMapper extends CollectionMapper {
    private final Field[] orderFields;

    public SetMapper(Builder builder) {
        super(builder);
        this.orderFields = builder.orderFields;
    }

    @Override
    protected JedoCollection<Object> newCollection(Storage pm, Object parent) {
        if (type.isAssignableFrom(JedoSet.class)) {
            return new JedoSet<>(pm, this, parent);
        } else if (orderFields != null && orderFields.length > 0
                && type.isAssignableFrom(JedoSortedSet.class)) {
            return new JedoSortedSet<>(pm, this, parent, orderFields);
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
