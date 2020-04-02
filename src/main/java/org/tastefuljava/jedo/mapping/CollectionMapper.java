package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Collection;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.JedoCollection;
import org.tastefuljava.jedo.util.Reflection;

public abstract class CollectionMapper extends ValueMapper {
    private final FetchMode fetchMode;
    protected ValueMapper elmMapper;
    private final Statement fetch;
    private final Statement clear;
    private Statement add;
    private final Statement remove;

    protected CollectionMapper(BuildContext context, Builder builder) {
        super(builder);
        this.elmMapper = builder.buildElmMapper(context);
        this.fetchMode = builder.fetchMode;
        this.fetch = builder.buildFetch();
        this.clear = builder.buildClear();
        this.remove = builder.buildRemove();
        context.addFinalizer(()->{
            String[] keys = null;
            if (elmMapper instanceof ClassMapper) {
                keys = ((ClassMapper)elmMapper).getIdColumns();
            }
            add = builder.buildAdd(keys);
        });
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs,
            ValueAccessor fm) {
        Collection<?> model = (Collection<?>)fm.getValue(obj);
        return createCollection(pm, obj, model, false);
    }

    public void fetch(Storage pm, Object parent, Collection<?> result) {
        Collection<Object> col = (Collection<Object>)result;
        pm.query(elmMapper, fetch, parent, new Object[]{parent}, col);
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
    void afterInsert(Storage pm, Object obj, ValueAccessor fm) {
        Collection<?> prevCol = (Collection<?>)fm.getValue(obj);
        Collection<Object> newCol = createCollection(pm, obj, prevCol, true);
        fm.setValue(obj, newCol);
        if (prevCol != null) {
            newCol.addAll(prevCol);
        }
    }

    private JedoCollection<Object> createCollection(Storage pm,
            Object parent, Collection<?> model, boolean empty) {
        JedoCollection<Object> col = newCollection(pm, parent, model);
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

    protected abstract JedoCollection<Object> newCollection(
            Storage pm, Object parent, Collection<?> model);

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
        pm.execute(add, o, new Object[]{parent,o});
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
        private ValueMapper.Builder elements;
        private Statement.Builder fetch;
        private Statement.Builder clear;
        private Statement.Builder add;
        private Statement.Builder remove;

        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode) {
            super(field.getType());
            this.parentClass = parentClass;
            elmClass = Reflection.getReferencedClass(field);
            this.fetchMode = fetchMode;
        }

        public void setElements(Class<?> clazz, String column) {
            elements = new ColumnMapper.Builder(
                    clazz == null ? elmClass : clazz, column);
        }

        public Statement.Builder newFetchStatement(String... paramNames) {
            return fetch = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }

        public Statement.Builder newClearStatement(String... paramNames) {
            return clear = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }

        public Statement.Builder newAddStatement(String... paramNames) {
            return add = new Statement.Builder(elmClass, paramNames);
        }

        public Statement.Builder newRemove(String... paramNames) {
            return remove = new Statement.Builder(elmClass, paramNames);
        }

        private ValueMapper buildElmMapper(BuildContext context) {
            return elements == null ? null : elements.build(context);
        }

        private Statement buildFetch() {
            return fetch == null ? null : fetch.build(null);
        }

        private Statement buildAdd(String[] generatedKeys) {
            return add == null ? null : add.build(generatedKeys);
        }

        private Statement buildClear() {
            return clear == null ? null : clear.build(null);
        }

        private Statement buildRemove() {
            return remove == null ? null : remove.build(null);
        }
 
        @Override
        protected void initialize(BuildContext context, CollectionMapper colm) {
            if (elements == null) {
                context.addForwardClassRef(elmClass, (cm)->{
                    colm.elmMapper = cm;
                });
            }
        }
    }
}
