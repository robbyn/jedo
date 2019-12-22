package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.rel.LazyList;
import org.tastefuljava.jedo.rel.LazySet;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.rel.LazyCollection;
import org.tastefuljava.jedo.util.Reflection;
import org.tastefuljava.jedo.util.XMLWriter;

public class CollectionMapper extends FieldMapper {
    private final FetchMode fetchMode;
    private ClassMapper elmClass;
    private final Statement fetch;
    private final Statement clear;
    private final Statement add;
    private final Statement remove;

    private CollectionMapper(Builder builder) {
        super(builder.field);
        this.fetchMode = builder.fetchMode;
        this.fetch = builder.buildFetch();
        this.add = builder.buildAdd();
        this.clear = builder.buildClear();
        this.remove = builder.buildRemove();
    }

    @Override
    public Object fromResultSet(Connection cnt, Cache cache, Object obj,
            ResultSet rs) {
        return createCollection(cnt, cache, obj);
    }

    public void fetch(Connection cnt, Cache cache, Object parent,
            Collection<?> result) {
        @SuppressWarnings("unchecked")
        Collection<Object> col = (Collection<Object>)result;
        elmClass.query(cnt, cache, fetch, new Object[]{parent}, col);
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
    void afterInsert(Connection cnt, Cache cache, Object obj) {
        Collection<?> prevCol = (Collection<?>)this.getValue(obj);
        Collection<Object> newCol = createCollection(cnt, cache, obj);
        this.setValue(obj, newCol);
        if (prevCol != null) {
            newCol.addAll(prevCol);
        }
    }

    private Collection<Object> createCollection(Connection cnt, Cache cache,
            Object parent) {
        LazyCollection<Object> col;
        if (field.getType() == Set.class
                || field.getType() == Collection.class) {
            col = new LazySet<>(cnt, cache, this, parent);
        } else if (field.getType() == List.class) {
            col = new LazyList<>(cnt, cache, this, parent);
        } else {
            throw new JedoException("Unsupported collection field type "
                    + field.getType().getName());
        }
        switch (fetchMode) {
            case EAGER:
                col.get();
                break;
            case LAZY:
                break;
            default:
                throw new JedoException("Invalid fetch mode: " + fetchMode);
        }
        return col;
    }

    @Override
    public void fixForwards(Map<Class<?>, ClassMapper> map) {
        Class<?> clazz = Reflection.getReferencedType(field);
        elmClass = map.get(clazz);
        if (elmClass == null) {
            throw new JedoException("Unresolved collection element class: "
                    + field.getType().getName());
        }
    }

    @Override
    public void writeTo(XMLWriter out) {
        out.startTag("collection");
        out.attribute("name", field.getName());
        if (fetch != null) {
            fetch.writeTo(out, "fetch", null);
        }
        if (clear != null) {
            clear.writeTo(out, "clear", null);
        }
        if (add != null) {
            add.writeTo(out, "add", null);
        }
        if (remove != null) {
            remove.writeTo(out, "remove", null);
        }
        out.endTag();
    }

    public static class Builder extends FieldMapper.Builder<CollectionMapper> {
        private final FetchMode fetchMode;
        private Statement.Builder fetch;
        private Statement.Builder clear;
        private Statement.Builder add;
        private Statement.Builder remove;

        public Builder(Field field, FetchMode fetchMode) {
            super(field);
            this.fetchMode = fetchMode;
        }

        public void setFetch(Statement.Builder fetch) {
            this.fetch = fetch;
        }

        public void setClear(Statement.Builder clear) {
            this.clear = clear;
        }

        public void setAdd(Statement.Builder add) {
            this.add = add;
        }

        public void setRemove(Statement.Builder remove) {
            this.remove = remove;
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
        public CollectionMapper build() {
            return new CollectionMapper(this);
        }
   }
}
