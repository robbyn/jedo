package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;

public class ListMapper extends CollectionMapper {
    private final Statement setAt;
    private final Statement addAt;
    private final Statement removeAt;

    private ListMapper(Builder builder) {
        super(builder);
        this.setAt = builder.buildSetAt();
        this.addAt = builder.buildAddAt();
        this.removeAt = builder.buildRemoveAt();
    }
 
    public static class Builder extends CollectionMapper.Builder {
        private Statement.Builder setAt;
        private Statement.Builder addAt;
        private Statement.Builder removeAt;

        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode) {
            super(parentClass, field, fetchMode);
        }

        public Statement buildSetAt() {
            return setAt == null ? null : setAt.build();
        }

        public Statement buildAddAt() {
            return addAt == null ? null : addAt.build();
        }

        public Statement buildRemoveAt() {
            return removeAt == null ? null : removeAt.build();
        }

        @Override
        public ListMapper build() {
            return new ListMapper(this);
        }
    }
}
