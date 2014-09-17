package org.tastefuljava.jedo.mapping;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import org.tastefuljava.jedo.cache.Cache;

public abstract class LazyCollection<T> implements Collection<T> {
    private final Connection cnt;
    private final Cache<Object,Object> cache;
    private final CollectionMapper mapper;
    private final Object[] args;

    protected LazyCollection(Connection cnt, Cache<Object,Object> cache,
            CollectionMapper mapper, Object[] args) {
        this.cnt = cnt;
        this.cache = cache;
        this.mapper = mapper;
        this.args = args;
    }

    @Override
    public int size() {
        return get().size();
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return get().contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return get().iterator();
    }

    @Override
    public Object[] toArray() {
        return get().toArray();
    }

    @Override
    public <TT> TT[] toArray(TT[] a) {
        return get().toArray(a);
    }

    @Override
    public boolean add(T e) {
        return get().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return get().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return get().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return get().addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return get().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return get().retainAll(c);
    }

    @Override
    public void clear() {
        get().clear();
    }

    protected Collection<T> get() {
        Collection<T> col = newCollection();
        mapper.fetch(cnt, cache, args, col);
        return col;
    }

    protected abstract Collection<T> newCollection();
}
