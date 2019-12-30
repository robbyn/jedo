package org.tastefuljava.jedo.rel;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.tastefuljava.jedo.mapping.MapMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class JedoSortedMap<K,V> extends JedoMap<K,V> implements SortedMap<K,V> {
    private static final Logger LOG
            = Logger.getLogger(JedoSortedMap.class.getName());

    private final Comparator<? super K> comp;

    public JedoSortedMap(Storage pm, MapMapper mapper, Object parent,
            Comparator<? super K> comp) {
        super(pm, mapper, parent);
        this.comp = comp;
    }

    @Override
    public SortedMap<K, V> get() {
        return (SortedMap<K, V>)super.get();
    }

    @Override
    public Comparator<? super K> comparator() {
        return comp;
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return get().subMap(fromKey, toKey);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return get().headMap(toKey);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return get().tailMap(fromKey);
    }

    @Override
    public K firstKey() {
        return get().firstKey();
    }

    @Override
    public K lastKey() {
        return get().lastKey();
    }

    @Override
    public SortedMap<K, V> newMap() {
        return new TreeMap<>(comp);
    }
}
