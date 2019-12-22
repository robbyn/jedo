package org.tastefuljava.jedo.rel;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.mapping.CollectionMapper;
import org.tastefuljava.jedo.mapping.Statement;

public abstract class LazyCollection<T> implements Collection<T> {
    private final Connection cnt;
    private final Cache cache;
    private final CollectionMapper mapper;
    private final Object parent;
    private Collection<T> col;

    protected LazyCollection(Connection cnt, Cache cache,
            CollectionMapper mapper, Object parent) {
        this.cnt = cnt;
        this.cache = cache;
        this.mapper = mapper;
        this.parent = parent;
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
        Statement stmt = mapper.getAdd();
        if (stmt == null) {
            throw new JedoException("Cannot add to collection");
        }
        boolean result = get().add(e);
        if (result) {
            stmt.executeUpdate(cnt, parent, new Object[] {parent, e});
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        Statement stmt = mapper.getRemove();
        if (stmt == null) {
            throw new JedoException("Cannot remove from collection");
        }
        boolean result = get().remove(o);
        if (result) {
            stmt.executeUpdate(cnt, parent, new Object[] {parent, o});
        }
        return result;
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
            if (remove(e)) {
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
            if (c.contains(e) && remove(e)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        Statement stmt = mapper.getClear();
        if (stmt == null) {
            throw new JedoException("Cannot remove from collection");
        }
        stmt.executeUpdate(cnt, parent, new Object[] {parent});
        if (col == null) {
            col = newCollection();
        } else {
            col.clear();
        }
    }

    public Collection<T> get() {
        if (col == null) {
            col = newCollection();
            mapper.fetch(cnt, cache, parent, col);
        }
        return col;
    }

    protected abstract Collection<T> newCollection();
}
