package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.LazyCollection;
import org.tastefuljava.jedo.rel.LazyList;

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

    public boolean setAt(Storage pm, Object parent, Object o, int index) {
        if (setAt == null) {
            return false;
        }
        pm.insert(elmClass, setAt, o, new Object[]{parent, o, index});
        return true;
    }


    public boolean addAt(Storage pm, Object parent, Object o, int index) {
        if (addAt == null) {
            return false;
        }
        pm.execute(addAt, o, new Object[]{parent, o, index});
        return true;
    }

    public boolean removeAt(Storage pm, Object parent, int index) {
        if (removeAt == null) {
            return false;
        }
        pm.execute(removeAt, parent, new Object[]{parent, index});
        return true;
    }

    @Override
    void beforeDelete(Storage pm, Object self, FieldMapper fm) {
        LazyList<?> list = (LazyList<?>)fm.getValue(self);
        pm.dispose(list);
        list.clear();
    }

    @Override
    protected LazyCollection<Object> newCollection(Storage pm, Object parent) {
        if (type == List.class) {
            return new LazyList<>(pm, this, parent);
        } else {
            throw new JedoException("Unsupported list field type "
                    + type.getName());
        }
    }

    public static class Builder extends CollectionMapper.Builder {
        private Statement.Builder setAt;
        private Statement.Builder addAt;
        private Statement.Builder removeAt;

        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode) {
            super(parentClass, field, fetchMode);
        }

        public Statement.Builder newSetAt(String... paramNames) {
            return setAt = new Statement.Builder(elmClass, paramNames);
        }

        public Statement.Builder newAddAtStatement(boolean collectKeys,
                String... paramNames) {
            addAt = new Statement.Builder(elmClass, paramNames);
            if (collectKeys) {
                // temporarily set the gererate keys to an empty array. It will
                // be updated with the actual column names in fixForwards.
                addAt.setGeneratedKeys(new String[0]);
            }
            return addAt;
        }

        public Statement.Builder newRemoveAt(String... paramNames) {
            return removeAt = new Statement.Builder(elmClass, paramNames);
        }

        private Statement buildSetAt() {
            return setAt == null ? null : setAt.build();
        }

        private Statement buildAddAt() {
            return addAt == null ? null : addAt.build();
        }

        private Statement buildRemoveAt() {
            return removeAt == null ? null : removeAt.build();
        }

        @Override
        public void fixForwards(Map<Class<?>, ClassMapper.Builder> map) {
            if (addAt != null && addAt.hasGeneratedKeys()) {
                ClassMapper.Builder cm = map.get(elmClass);
                if (cm == null) {
                    throw new JedoException(
                            "Unresolved collection element class: " 
                                    + type.getName());
                }
                addAt.setGeneratedKeys(cm.getIdColumns());
            }
        }

        @Override
        public ListMapper build() {
            return new ListMapper(this);
        }
    }
}
