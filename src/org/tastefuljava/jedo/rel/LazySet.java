package org.tastefuljava.jedo.rel;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.mapping.CollectionMapper;

public class LazySet<T> extends LazyCollection<T> implements Set<T> {
    public LazySet(Connection cnt, Cache cache,
            CollectionMapper mapper, Object[] args) {
        super(cnt, cache, mapper, args);
    }

    @Override
    protected Collection<T> newCollection() {
        return new HashSet<>();
    }
}
