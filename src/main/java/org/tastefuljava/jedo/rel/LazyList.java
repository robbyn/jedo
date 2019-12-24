package org.tastefuljava.jedo.rel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.CollectionMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class LazyList<T> extends LazyCollection<T> implements List<T> {
    public LazyList(Storage pm, CollectionMapper mapper,
            Object parent) {
        super(pm, mapper, parent);
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
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public T get(int index) {
        return list().get(index);
    }

    @Override
    public T set(int index, T element) {
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public void add(int index, T element) {
        throw new JedoException("Cannot modify collection");
    }

    @Override
    public T remove(int index) {
        throw new JedoException("Cannot modify collection");
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
