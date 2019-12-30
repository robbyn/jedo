package org.tastefuljava.jedo.rel;

import java.util.HashSet;
import java.util.Set;
import org.tastefuljava.jedo.mapping.SetMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class JedoSet<T> extends JedoCollection<T> implements Set<T> {
    private final SetMapper mapper;

    public JedoSet(Storage pm, SetMapper mapper, Object parent) {
        super(pm, parent);
        this.mapper = mapper;
    }

    @Override
    public Set<T> newCollection() {
        return new HashSet<>();
    }

    @Override
    protected SetMapper mapper() {
        return mapper;
    }

    @Override
    public boolean add(T e) {
        boolean result = get().add(e);
        if (result) {
            mapper().add(pm, parent, e);
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
         boolean result = get().remove(o);
        if (result) {
            mapper().remove(pm, parent, o);
        }
        return result;
    }
}
