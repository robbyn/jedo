package org.tastefuljava.jedo.rel;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.tastefuljava.jedo.mapping.MapMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class JedoMap<K,V> extends AbstractMap<K,V> {
    protected final Storage pm;
    protected final Object parent;
    private final MapMapper mapper;
    private Map<K,V> map;

    public JedoMap(Storage pm, MapMapper mapper, Object parent) {
        this.pm = pm;
        this.parent = parent;
        this.mapper = mapper;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return get().entrySet();
    }

    public Map<K,V> get() {
        if (map == null) {
            map = newMap();
            mapper.fetch(pm, parent, map);
        }
        return map;
    }

    @Override
    public V get(Object key) {
        return get().get(key);
    }

    @Override
    public int hashCode() {
        return get().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return get().equals(o);
    }

    @Override
    public Collection<V> values() {
        return get().values();
    }

    @Override
    public Set<K> keySet() {
        return get().keySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return get().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return get().containsValue(value);
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @Override
    public int size() {
        return get().size();
    }

    @Override
    public void clear() {
        mapper.clear(pm, parent);
        setEmpty();
    }

    @Override
    public V put(K key, V value) {
        V prev = get().put(key, value);
        if (prev != null) {
            // the key already exists: it needs to be removed
            mapper.removeKey(pm, parent, key);
        }
        mapper.put(pm, parent, key, value);
        return prev;
    }

    @Override
    public V remove(Object key) {
        V prev = get().remove(key);
        if (prev != null) {
            mapper.removeKey(pm, parent, key);
        }
        return prev;
    }

    public Map<K,V> newMap() {
        return new HashMap<>();
    }

    public void setEmpty() {
        if (map == null) {
            map = newMap();
        } else {
            map.clear();
        }
    }
}
