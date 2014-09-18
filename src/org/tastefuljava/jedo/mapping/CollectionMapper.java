package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.util.ClassUtil;
import org.tastefuljava.jedo.util.XMLWriter;

public class CollectionMapper extends FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(CollectionMapper.class.getName());

    private final String[] columns;
    private final String queryName;
    private ClassMapper elmClass;
    private Statement query;
    private FetchMode fetchMode;

    CollectionMapper(Field field, String queryName, String[] columns,
            FetchMode fetchMode) {
        super(field);
        this.columns = columns;
        this.queryName = queryName;
        this.fetchMode = fetchMode;
    }

    @Override
    public Object fromResultSet(Connection cnt, Cache<Object, Object> cache,
            ResultSet rs) {
        try {
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; ++i) {
                values[i] = rs.getObject(columns[i]);
            }
            return createCollection(cnt, cache, values);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    void fixReferences(Map<Class<?>, ClassMapper> map) {
        Class<?> clazz = ClassUtil.getElementType(field);
        elmClass = map.get(clazz);
        if (elmClass == null) {
            throw new JedoException("Unresolved collection element class: "
                    + field.getType().getName());
        }
        query = elmClass.getQuery(queryName);
        if (query == null) {
            throw new JedoException("Query " + queryName
                    + " not found in class " + clazz.getName());
        }
    }

    void fetch(Connection cnt, Cache<Object, Object> cache, Object[] args,
            Collection<?> result) {
        @SuppressWarnings("unchecked")
        Collection<Object> col = (Collection<Object>)result;
        elmClass.query(cnt, cache, query, args, col);
    }

    private Collection<?> createCollection(Connection cnt,
            Cache<Object, Object> cache, Object[] args) {
        switch (fetchMode) {
            case EAGER: {
                    if (field.getType() == Set.class
                            || field.getType() == Collection.class) {
                        Set<?> result = new HashSet<>();
                        fetch(cnt, cache, args, result);
                        return Collections.unmodifiableSet(result);
                    } else if (field.getType() == List.class) {
                        List<?> result = new ArrayList<>();
                        fetch(cnt, cache, args, result);
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
                    return new LazySet<>(cnt, cache, this, args);
                } else if (field.getType() == List.class) {
                    return new LazyList<>(cnt, cache, this, args);
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
        out.attribute("column", columnList());
        out.endTag();
    }

    private String columnList() {
        StringBuilder buf = new StringBuilder();
        if (columns.length > 0) {
            buf.append(columns[0]);
            for (int i = 1; i < columns.length; ++i) {
                buf.append(',');
                buf.append(columns[i]);
            }
        }
        return buf.toString();
    }
}
