package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;
import java.util.SortedMap;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.rel.JedoMap;
import org.tastefuljava.jedo.rel.JedoSortedMap;
import org.tastefuljava.jedo.util.Reflection;

public class MapMapper extends ValueMapper {
    private final FetchMode fetchMode;
    private ValueMapper keyMapper;
    private ValueMapper elmMapper;
    private final Statement fetch;
    private final Statement clear;
    private final Statement put;
    private final Statement removeKey;

    public MapMapper(BuildContext context, Builder builder) {
        super(builder);
        this.fetchMode = builder.fetchMode;
        this.keyMapper = builder.buildKeyMapper(context);
        this.elmMapper = builder.buildElmMapper(context);
        this.fetch = builder.buildFetch();
        this.clear = builder.buildClear();
        this.put = builder.buildPut();
        this.removeKey = builder.buildRemoveKey();
    }

    @Override
    public Object fromResultSet(Storage pm, Object obj, ResultSet rs,
            ValueAccessor fm) {
        Map<?,?> model = (Map<?,?>)fm.getValue(obj);
        return createMap(pm, obj, model, false);
    }

    public void fetch(Storage pm, Object parent, Map<?,?> result) {
        @SuppressWarnings("unchecked")
        Map<Object,Object> map = (Map<Object,Object>)result;
        pm.query(keyMapper, elmMapper, fetch, parent, new Object[]{parent}, map);
    }

    public void clear(Storage pm, Object parent) {
        if (clear == null) {
            throw new JedoException("Cannot clear map: no statement defined.");
        }
        pm.execute(clear, parent, new Object[]{parent});
    }

    public void put(Storage pm, Object parent, Object key, Object value) {
        if (put == null) {
            throw new JedoException("Cannot put to map: no statement defined.");
        }
        pm.execute(put, parent, new Object[]{parent, key, value});
    }

    public void removeKey(Storage pm, Object parent, Object key) {
        if (removeKey == null) {
            throw new JedoException(
                    "Cannot remove from map: no statement defined.");
        }
        pm.execute(removeKey, parent, new Object[]{parent, key});
    }

    private JedoMap<Object,Object> createMap(Storage pm,
            Object parent, Map<?,?> model, boolean empty) {
        JedoMap<Object,Object> map = newMap(pm, parent, model);
        if (empty) {
            map.setEmpty();
        } else {
            switch (fetchMode) {
                case EAGER:
                    map.get(); // this will load the content
                    break;
                case LAZY:
                    break;
                default:
                    throw new JedoException("Invalid fetch mode: " + fetchMode);
            }
        }
        return map;
    }

    protected JedoMap<Object,Object> newMap(Storage pm, Object parent,
            Map<?,?> model) {
        if (type.isAssignableFrom(JedoMap.class)) {
            if (model != null && model instanceof SortedMap) {
                return new JedoSortedMap<>(pm, this, parent,
                        ((SortedMap)model).comparator());
            } else {
                return new JedoMap<>(pm, this, parent);
            }
        } else if (type.isAssignableFrom(JedoSortedMap.class)) {
            if (model != null && model instanceof SortedMap) {
                return new JedoSortedMap<>(pm, this, parent,
                        ((SortedMap)model).comparator());
            } else {
                throw new JedoException("Could not create Set of type "
                        + type.getName());
            }
        } else {
            throw new JedoException("Unsupported map field type "
                    + type.getName());
        }
    }

    @Override
    void afterInsert(Storage pm, Object obj, ValueAccessor fm) {
        Map<?,?> prevMap = (Map<?,?>)fm.getValue(obj);
        Map<Object,Object> newMap = createMap(pm, obj, prevMap, true);
        fm.setValue(obj, newMap);
        if (prevMap != null) {
            newMap.putAll(prevMap);
        }
    }

    @Override
    void beforeDelete(Storage pm, Object self, ValueAccessor fm) {
        Map<?,?> map = (Map<?,?>)fm.getValue(self);
        if (map != null) {
            map.clear();
        }
    }

    public static class Builder extends ValueMapper.Builder<MapMapper> {
        private final FetchMode fetchMode;
        private final ClassMapper.Builder parentClass;
        private final Class<?> keyClass;
        private final Class<?> elmClass;
        private ValueMapper.Builder keys;
        private ValueMapper.Builder elements;
        private Statement.Builder fetch;
        private Statement.Builder clear;
        private Statement.Builder put;
        private Statement.Builder removeKey;

        public Builder(ClassMapper.Builder parentClass, Field field,
                FetchMode fetchMode) {
            super(field.getType());
            this.parentClass = parentClass;
            this.fetchMode = fetchMode;
            Class<?>[] argTypes = Reflection.getReferencedClasses(field, 2);
            this.keyClass = argTypes[0];
            this.elmClass = argTypes[1];
        }

        public void setKeys(Class<?> clazz, String column) {
            keys = new ColumnMapper.Builder(
                    clazz == null ? keyClass : clazz, column);
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
 
        public Statement.Builder newPutStatement(String... paramNames) {
            return put = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }

        public Statement.Builder newRemoveKeyStatement(String... paramNames) {
            return removeKey = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }

        private Statement buildFetch() {
            return fetch == null ? null : fetch.build(null);
        }

        private Statement buildClear() {
            return clear == null ? null : clear.build(null);
        }

        private Statement buildPut() {
            return put == null ? null : put.build(null);
        }

        private Statement buildRemoveKey() {
            return removeKey == null ? null : removeKey.build(null);
        }

        private ValueMapper buildKeyMapper(BuildContext context) {
            return keys == null ? null : keys.build(context);
        }

        private ValueMapper buildElmMapper(BuildContext context) {
            return elements == null ? null : elements.build(context);
        }

        @Override
        protected MapMapper create(BuildContext context) {
            return new MapMapper(context, this);
        }

        @Override
        protected void initialize(BuildContext context, MapMapper mm) {
            if (keys == null) {
                context.addForward((mapper)->{
                    mm.keyMapper = mapper.getClassMapper(keyClass);
                });
            }
            if (elements == null) {
                context.addForward((mapper)->{
                    mm.elmMapper = mapper.getClassMapper(elmClass);
                });
            }
        }
    }
}
