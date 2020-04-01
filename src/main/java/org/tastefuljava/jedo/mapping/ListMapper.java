package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.JedoCollection;
import org.tastefuljava.jedo.rel.JedoList;

public class ListMapper extends CollectionMapper {
    private final Statement setAt;
    private final Statement addAt;
    private final Statement removeAt;

    private ListMapper(BuildContext context, Builder builder) {
        super(context, builder);
        this.setAt = builder.buildSetAt();
        this.addAt = builder.buildAddAt();
        this.removeAt = builder.buildRemoveAt();
    }

    public boolean setAt(Storage pm, Object parent, Object o, int index) {
        if (setAt == null) {
            return false;
        }
        pm.execute(setAt, o, new Object[]{parent, o, index});
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
    void beforeDelete(Storage pm, Object self, ValueAccessor fm) {
        JedoList<?> list = (JedoList<?>)fm.getValue(self);
        pm.dispose(list);
        list.clear();
    }

    @Override
    protected JedoCollection<Object> newCollection(Storage pm, Object parent,
            Collection<?> model) {
        if (type == List.class) {
            return new JedoList<>(pm, this, parent);
        } else {
            throw new JedoException("Unsupported list field type "
                    + type.getName());
        }
    }

    public static class Builder extends CollectionMapper.Builder {
        private Statement.Builder setAt;
        private Statement.Builder addAt;
        private Statement.Builder removeAt;

        public Builder(ClassMapper.Builder parentClass, Field field, FetchMode fetchMode) {
            super(parentClass, field, fetchMode);
        }

        public Statement.Builder newSetAt(String... paramNames) {
            return setAt = new Statement.Builder(elmClass, paramNames);
        }

        public Statement.Builder newAddAtStatement(String... paramNames) {
            return addAt = new Statement.Builder(elmClass, paramNames);
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
        protected ListMapper create(BuildContext context) {
            return new ListMapper(context, this);
        }
    }
}
