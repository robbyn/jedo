package org.tastefuljava.jedo.rel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.CollectionMapper;
import org.tastefuljava.jedo.mapping.ListMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class LazyList<T> extends LazyCollection<T> implements List<T> {
    private boolean dirty;

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
        boolean done = false;
        for (T elm: c) {
            add(index++, elm);
            done = true;
        }
        return done;
    }

    @Override
    public T get(int index) {
        return list().get(index);
    }

    @Override
    public T set(int index, T element) {
        List<T> list = list();
        T result = list.set(index, element);
        if (!dirty && (mapper instanceof ListMapper)) {
            ListMapper lm = (ListMapper)mapper;
            if (!lm.setAt(pm, parent, element, index)) {
                dirty = true;
            }
        }
        return result;
    }

    @Override
    public void add(int index, T element) {
        list().add(index, element);
        dirty = true;
    }

    @Override
    public T remove(int index) {
        T result = list().remove(index);
        dirty = true;
        return result;
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
