package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;

public class SetMapper extends CollectionMapper {
    public SetMapper(Builder builder) {
        super(builder);
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
