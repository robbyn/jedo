package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.SortedSet;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.JedoCollection;
import org.tastefuljava.jedo.rel.JedoSet;
import org.tastefuljava.jedo.rel.JedoSortedSet;

public class SetMapper extends CollectionMapper {
    private final Field[] orderFields;

    public SetMapper(BuildContext context, Builder builder) {
        super(context, builder);
        this.orderFields = builder.orderFields;
    }

    @Override
    public <T> T accept(ValueMapperVisitor<T> vtor) {
        return vtor.visitSetMapper(this);
    }

    @Override
    protected JedoCollection<Object> newCollection(Storage pm, Object parent,
            Collection<?> model) {
        if (type.isAssignableFrom(JedoSet.class)) {
            if (model != null && model instanceof SortedSet) {
                return new JedoSortedSet<>(pm, this, parent,
                        ((SortedSet)model).comparator());
            } else {
                return new JedoSet<>(pm, this, parent);
            }
        } else if (type.isAssignableFrom(JedoSortedSet.class)) {
            if (model != null && model instanceof SortedSet) {
                return new JedoSortedSet<>(pm, this, parent,
                        ((SortedSet)model).comparator());
            } else if (orderFields != null && orderFields.length > 0) {
                return new JedoSortedSet<>(pm, this, parent, orderFields);
            } else {
                throw new JedoException("Could not create Set of type "
                        + type.getName());
            }
        } else {
            throw new JedoException("Unsupported set field type "
                    + type.getName());
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
        protected CollectionMapper create(BuildContext context) {
            return new SetMapper(context, this);
        }
    }
}
