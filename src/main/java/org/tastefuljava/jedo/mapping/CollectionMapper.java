package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.rel.LazyList;
import org.tastefuljava.jedo.rel.LazySet;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.util.Reflection;
import org.tastefuljava.jedo.util.XMLWriter;

public class CollectionMapper extends FieldMapper {
    private final String queryName;
    private final FetchMode fetchMode;
    private ClassMapper contClass;
    private ClassMapper elmClass;
    private Statement fetch;
    private final Statement clear;
    private final Statement add;
    private final Statement remove;

    CollectionMapper(Builder builder) {
        super(builder.field);
        this.queryName = builder.queryName;
        this.fetchMode = builder.fetchMode;
        this.fetch = builder.fetch;
        this.add = builder.add;
        this.clear = builder.clear;
        this.remove = builder.remove;
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

    @Override
    void fixReferences(ClassMapper contClass, Map<Class<?>, ClassMapper> map) {
        this.contClass = contClass;
        Class<?> clazz = Reflection.getReferencedType(field);
        elmClass = map.get(clazz);
        if (elmClass == null) {
            throw new JedoException("Unresolved collection element class: "
                    + field.getType().getName());
        }
        if (fetch == null && queryName != null) {
            fetch = elmClass.getQuery(queryName);
            if (fetch == null) {
                throw new JedoException("Query " + queryName
                        + " not found in class " + clazz.getName());
            }
        }
    }

    @Override
    void afterInsert(Connection cnt, Cache cache, Object obj) {
        Object[] values = contClass.getIdValues(obj);
        this.setValue(obj, createCollection(cnt, cache, values));
    }

    private Collection<?> createCollection(Connection cnt, Cache cache,
            Object parent) {
        switch (fetchMode) {
            case EAGER: {
                    if (field.getType() == Set.class
                            || field.getType() == Collection.class) {
                        Set<?> result = new HashSet<>();
                        fetch(cnt, cache, parent, result);
                        return Collections.unmodifiableSet(result);
                    } else if (field.getType() == List.class) {
                        List<?> result = new ArrayList<>();
                        fetch(cnt, cache, parent, result);
                        return Collections.unmodifiableList(result);
                    } else {
                        throw new JedoException(
                                "Unsupported collection field type "
                                 + field.getType().getName());
                    }
                }
            case LAZY:
                if (field.getType() == Set.class
                        || field.getType() == Collection.class) {
                    return new LazySet<>(cnt, cache, this, parent);
                } else if (field.getType() == List.class) {
                    return new LazyList<>(cnt, cache, this, parent);
                } else {
                    throw new JedoException("Unsupported collection field type "
                            + field.getType().getName());
                }
            default:
                throw new JedoException("Invalid fetch mode: " + fetchMode);
        }
    }

    @Override
    public void writeTo(XMLWriter out) {
        out.startTag("collection");
        out.attribute("name", field.getName());
        out.attribute("query", queryName);
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

    public static class Builder {
        private final Field field;
        private final String queryName;
        private final FetchMode fetchMode;
        private Statement fetch;
        private Statement clear;
        private Statement add;
        private Statement remove;

        public Builder(Field field, String queryName,
            FetchMode fetchMode) {
            this.field = field;
            this.queryName = queryName;
            this.fetchMode = fetchMode;
        }

        public void setFetch(Statement fetch) {
            this.fetch = fetch;
        }

        public void setClear(Statement clear) {
            this.clear = clear;
        }

        public void setAdd(Statement add) {
            this.add = add;
        }

        public void setRemove(Statement remove) {
            this.remove = remove;
        }
    }
}
