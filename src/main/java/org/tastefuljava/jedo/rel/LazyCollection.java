package org.tastefuljava.jedo.rel;

import java.util.Collection;
import java.util.Iterator;
import org.tastefuljava.jedo.mapping.CollectionMapper;
import org.tastefuljava.jedo.mapping.Storage;

public abstract class LazyCollection<T> implements Collection<T> {
    protected final Storage pm;
    protected final Object parent;
    private Collection<T> col;

    protected LazyCollection(Storage pm, Object parent) {
        this.pm = pm;
        this.parent = parent;
    }

    public void setEmpty() {
        this.col = newCollection();
    }

    protected abstract CollectionMapper mapper();
    public abstract Collection<T> newCollection();

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
    public boolean containsAll(Collection<?> c) {
        return get().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T e: c) {
            if (add(e)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object e: c) {
            if (remove((T)e)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        Object[] all = get().toArray();
        for (Object e: all) {
            if (c.contains(e) && remove((T)e)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        mapper().clear(pm, parent);
        if (col == null) {
            setEmpty();
        } else {
            col.clear();
        }
    }

    public Collection<T> get() {
        if (col == null) {
            setEmpty();
            mapper().fetch(pm, parent, col);
        }
        return col;
    }
}
