package org.tastefuljava.jedo.rel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tastefuljava.jedo.mapping.CollectionMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class LazySet<T> extends LazyCollection<T> implements Set<T> {
    public LazySet(Storage pm,
            CollectionMapper mapper, Object parent) {
        super(pm, mapper, parent);
    }

    @Override
    protected Collection<T> newCollection() {
        return new HashSet<>();
    }
}
