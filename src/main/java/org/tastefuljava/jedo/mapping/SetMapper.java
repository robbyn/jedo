package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.Set;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.LazyCollection;
import org.tastefuljava.jedo.rel.LazySet;

public class SetMapper extends CollectionMapper {
    public SetMapper(Builder builder) {
        super(builder);
    }

    @Override
    protected LazyCollection<Object> newCollection(Storage pm, Object parent) {
        if (field.getType() == Set.class) {
            return new LazySet<>(pm, this, parent);
        } else {
            throw new JedoException("Unsupported set field type "
                    + field.getType().getName());
        }
    }

    public static class Builder extends CollectionMapper.Builder {
        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode) {
            super(parentClass, field, fetchMode);
        }

        @Override
        public CollectionMapper build() {
            return new SetMapper(this);
        }
    }
}
