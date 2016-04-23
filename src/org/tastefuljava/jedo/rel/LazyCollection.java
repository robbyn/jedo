package org.tastefuljava.jedo.rel;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.mapping.CollectionMapper;

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
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public boolean remove(Object o) {
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return get().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return get().retainAll(c);
    }

    @Override
    public void clear() {
        throw new JedoException("Cannot modify collection");
    }

    protected Collection<T> get() {
        Collection<T> col = newCollection();
        mapper.fetch(cnt, cache, args, col);
        return col;
    }

    protected abstract Collection<T> newCollection();
}
