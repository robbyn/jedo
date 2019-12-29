package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.LazyCollection;
import org.tastefuljava.jedo.util.Reflection;

public abstract class CollectionMapper extends ValueMapper {
    private final FetchMode fetchMode;
    protected ClassMapper elmClass;
    private final Statement fetch;
    private final Statement clear;
    private final Statement add;
    private final Statement remove;

    protected CollectionMapper(Builder builder) {
        super(builder);
        this.fetchMode = builder.fetchMode;
        this.fetch = builder.buildFetch();
        this.add = builder.buildAdd();
        this.clear = builder.buildClear();
        this.remove = builder.buildRemove();
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs) {
        return createCollection(pm, obj, false);
    }

    public void fetch(Storage pm, Object parent,
            Collection<?> result) {
        @SuppressWarnings("unchecked")
        Collection<Object> col = (Collection<Object>)result;
        pm.query(elmClass, fetch, parent, new Object[]{parent}, col);
    }

    public ClassMapper getElementClass() {
        return elmClass;
    }

    public Statement getClear() {
        return clear;
    }

    public Statement getAdd() {
        return add;
    }

    public Statement getRemove() {
        return remove;
    }

    @Override
    void afterInsert(Storage pm, Object obj, FieldMapper fm) {
        Collection<?> prevCol = (Collection<?>)fm.getValue(obj);
        Collection<Object> newCol = createCollection(pm, obj, true);
        fm.setValue(obj, newCol);
        if (prevCol != null) {
            newCol.addAll(prevCol);
        }
    }

    private LazyCollection<Object> createCollection(Storage pm,
            Object parent, boolean empty) {
        LazyCollection<Object> col = newCollection(pm, parent);
        if (empty) {
            col.setEmpty();
        } else {
            switch (fetchMode) {
                case EAGER:
                    col.get();
                    break;
                case LAZY:
                    break;
                default:
                    throw new JedoException("Invalid fetch mode: " + fetchMode);
            }
        }
        return col;
    }

    protected abstract LazyCollection<Object> newCollection(
            Storage pm, Object parent);

    @Override
    public void fixForwardFields(Map<Class<?>, ClassMapper> map, Field field) {
        Class<?> clazz = Reflection.getReferencedType(field);
        elmClass = map.get(clazz);
        if (elmClass == null) {
            throw new JedoException("Unresolved collection element class: "
                    + field.getType().getName());
        }
    }

    public void clear(Storage pm, Object parent) {
        if (clear == null) {
            throw new JedoException("Cannot clear collection");
        }
        pm.execute(clear, parent, new Object[]{parent});
    }

    public void add(Storage pm, Object parent, Object o) {
        if (add == null) {
            throw new JedoException("Cannot add to collection");
        }
        pm.insert(elmClass, add, o, new Object[]{parent,o});
    }

    public void remove(Storage pm, Object parent, Object o) {
        if (remove == null) {
            throw new JedoException("Cannot remove from collection");
        }
        pm.execute(remove, o, new Object[]{parent, o});
    }

    public abstract static class Builder
            extends ValueMapper.Builder<CollectionMapper> {
        private final FetchMode fetchMode;
        private final ClassMapper.Builder parentClass;
        protected final Class<?> elmClass;
        private Statement.Builder fetch;
        private Statement.Builder clear;
        private Statement.Builder add;
        private Statement.Builder remove;

        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode) {
            super(field.getType());
            this.parentClass = parentClass;
            elmClass = Reflection.getReferencedType(field);
            this.fetchMode = fetchMode;
        }

        public Statement.Builder newFetchStatement(String... paramNames) {
            return fetch = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }

        public Statement.Builder newClearStatement(String... paramNames) {
            return clear = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }

        public Statement.Builder newAddStatement(boolean collectKeys,
                String... paramNames) {
            add = new Statement.Builder(elmClass, paramNames);
            if (collectKeys) {
                // temporarily set the gererate keys to an empty array. It will
                // be updated with the actual column names in fixForwards.
                add.setGeneratedKeys(new String[0]);
            }
            return add;
        }

        public Statement.Builder newRemove(String... paramNames) {
            return remove = new Statement.Builder(elmClass, paramNames);
        }

        private Statement buildFetch() {
            return fetch == null ? null : fetch.build();
        }

        private Statement buildAdd() {
            return add == null ? null : add.build();
        }

        private Statement buildClear() {
            return clear == null ? null : clear.build();
        }

        private Statement buildRemove() {
            return remove == null ? null : remove.build();
        }
 
        @Override
        public void fixForwards(Map<Class<?>, ClassMapper.Builder> map) {
            if (add != null && add.hasGeneratedKeys()) {
                ClassMapper.Builder cm = map.get(elmClass);
                if (cm == null) {
                    throw new JedoException(
                            "Unresolved collection element class: " 
                                    + type.getName());
                }
                add.setGeneratedKeys(cm.getIdColumns());
            }
        }
    }
}
