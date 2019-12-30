package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
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

    public MapMapper(Builder builder) {
        super(builder);
        this.fetchMode = builder.fetchMode;
        this.keyMapper = builder.buildKeyMapper();
        this.elmMapper = builder.buildElmMapper();
        this.fetch = builder.buildFetch();
        this.clear = builder.buildClear();
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
            throw new JedoException("Cannot clear collection");
        }
        pm.execute(clear, parent, new Object[]{parent});
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
        } else {
            throw new JedoException("Unsupported set field type "
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

    public static class Builder extends ValueMapper.Builder<MapMapper> {
        private final FetchMode fetchMode;
        private final ClassMapper.Builder parentClass;
        private final Class<?> keyClass;
        private final Class<?> elmClass;
        private ValueMapper.Builder keys;
        private ValueMapper.Builder elements;
        private Statement.Builder fetch;
        private Statement.Builder clear;

        public Builder(BuildContext context, ClassMapper.Builder parentClass,
                Field field, FetchMode fetchMode) {
            super(context, field.getType());
            this.parentClass = parentClass;
            this.fetchMode = fetchMode;
            Class<?>[] argTypes = Reflection.getReferencedClasses(field, 2);
            this.keyClass = argTypes[0];
            this.elmClass = argTypes[1];
        }

        public void setKeys(Class<?> clazz, String column) {
            keys = new ColumnMapper.Builder(
                    context, clazz == null ? keyClass : clazz, column);
        }

        public void setElements(Class<?> clazz, String column) {
            elements = new ColumnMapper.Builder(
                    context, clazz == null ? elmClass : clazz, column);
        }

        public Statement.Builder newFetchStatement(String... paramNames) {
            return fetch = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }
 
        public Statement.Builder newClearStatement(String... paramNames) {
            return clear = new Statement.Builder(
                    parentClass.getType(), paramNames);
        }
 
        private Statement buildFetch() {
            return fetch == null ? null : fetch.build();
        }

        private Statement buildClear() {
            return clear == null ? null : clear.build();
        }

        private void postBuild(MapMapper mm) {
            if (keys == null) {
                context.addForwardClassRef(keyClass, (cm)->{
                    mm.keyMapper = cm;
                });
            }
            if (elements == null) {
                context.addForwardClassRef(elmClass, (cm)->{
                    mm.elmMapper = cm;
                });
            }
        }

        private ValueMapper buildKeyMapper() {
            return keys == null ? null : keys.build();
        }

        private ValueMapper buildElmMapper() {
            return elements == null ? null : elements.build();
        }

        @Override
        public MapMapper build() {
            MapMapper result = new MapMapper(this);
            postBuild(result);
            return result;
        }
    }
}
