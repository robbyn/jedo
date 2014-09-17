package org.tastefuljava.jedo.mapping;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import org.tastefuljava.jedo.cache.Cache;

public class LazyList<T> extends LazyCollection<T> implements List<T> {
    public LazyList(Connection cnt, Cache<Object, Object> cache,
            CollectionMapper mapper, Object[] args) {
        super(cnt, cache, mapper, args);
    }

    @Override
    protected Collection<T> newCollection() {
        return new ArrayList<>();
    }

    protected final List<T> list() {
        return (List<T>)super.get();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return list().addAll(index, c);
    }

    @Override
    public T get(int index) {
        return list().get(index);
    }

    @Override
    public T set(int index, T element) {
        return list().set(index, element);
    }

    @Override
    public void add(int index, T element) {
        list().add(index, element);
    }

    @Override
    public T remove(int index) {
        return list().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return list().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list().lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return list().listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return list().listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return list().subList(fromIndex, toIndex);
    }
}
