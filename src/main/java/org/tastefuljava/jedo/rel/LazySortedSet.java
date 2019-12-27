package org.tastefuljava.jedo.rel;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.mapping.SetMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class LazySortedSet<T> extends LazySet<T> implements SortedSet<T> {
    private static final Logger LOG
            = Logger.getLogger(LazySortedSet.class.getName());

    private final Comparator<? super T> comp;

    public LazySortedSet(Storage pm, SetMapper mapper, Object parent,
            Field[] fields) {
        super(pm, mapper, parent);
        comp = (a,b) -> compareFields(a, b, fields);
    }

    @Override
    public SortedSet<T> get() {
        return (SortedSet<T>)super.get();
    }

    @Override
    public SortedSet<T> newCollection() {
        return new TreeSet<>(comp);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comp;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return get().subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return get().headSet(toElement);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return get().tailSet(fromElement);
    }

    @Override
    public T first() {
        return get().first();
    }

    @Override
    public T last() {
        return get().last();
    }

    private static int compareFields(Object a, Object b, Field[] fields) {
        for (Field field: fields) {
            try {
                Object va = field.get(a);
                Object vb = field.get(b);
                if (va != vb) {
                    if (va == null) {
                        return -1;
                    } else if (vb == null) {
                        return 1;
                    } else if (va instanceof Comparable) {
                        int r = ((Comparable<Object>)va).compareTo(vb);
                        if (r != 0) {
                            return r;
                        }
                    }
                }
            } catch (IllegalAccessException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        return 0;
    }
}
