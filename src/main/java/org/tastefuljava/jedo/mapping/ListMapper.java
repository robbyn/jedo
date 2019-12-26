package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import org.tastefuljava.jedo.JedoException;

public class ListMapper extends CollectionMapper {
    private final Statement setAt;

    private ListMapper(Builder builder) {
        super(builder);
        this.setAt = builder.buildSetAt();
    }

    public boolean setAt(Storage pm, Object parent, Object o, int index) {
        if (setAt == null) {
            return false;
        }
        pm.insert(elmClass, setAt, o, new Object[]{parent, o, index});
        return true;
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
