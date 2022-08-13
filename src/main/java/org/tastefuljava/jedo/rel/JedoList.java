package org.tastefuljava.jedo.rel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.Flushable;
import org.tastefuljava.jedo.mapping.ListMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class JedoList<T> extends JedoCollection<T>
        implements List<T>, Flushable {
    private boolean dirty;
    private final ListMapper mapper;

    public JedoList(Storage pm, ListMapper mapper, Object parent) {
        super(pm, parent);
        this.mapper = mapper;
    }

    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setEmpty() {
        super.setEmpty();
        dirty = false;
    }

    @Override
    protected ListMapper mapper() {
        return mapper;
    }

    @Override
    public List<T> newCollection() {
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
                markDirty();
            }
        }
        return result;
    }

    @Override
    public boolean add(T e) {
        add(size(), e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        int index = list().lastIndexOf(o);
        if (index < 0) {
            return false;
        }
        remove(index);
        return true;
    }

    @Override
    public void add(int index, T element) {
        List<T> list = list();
        list.add(index, element);
        if (dirty ) {
            // nothing 
        } else if (index+1 == list.size()) {
            // try do it immediately
            if (!mapper().addAt(pm, parent, element, index)) {
                throw new JedoException(
                        "Could not add element at index " + index);
            }
        } else {
            markDirty();
        }
    }

    @Override
    public T remove(int index) {
        List<T> list = list();
        T result = list.remove(index);
        if (dirty) {
        } else if (index == list.size()) {
            // try do it immediately
            if (!mapper().removeAt(pm, parent, index)) {
                // no removeAt statement: mark dirty so that a clear will be used.
                markDirty();
            }
        } else {
            markDirty();
        }
        return result;
    }

    @Override
    public void clear() {
        super.clear();
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

    @Override
    public void flush(Storage pm) {
        if (dirty) {
            // clear and readd all the elements
            List<T> list = list();
            mapper.clear(pm, parent);
            for (int i = 0; i < list.size(); ++i) {
                if (!mapper().addAt(pm, parent, list.get(i), i)) {
                    throw new JedoException("Could not add element at index "
                            + i);
                }
            }
            dirty = false;
        }
    }

    private void markDirty() {
        if (!dirty) {
            pm.markDirty(this);
            dirty = true;
        }
    }
}
