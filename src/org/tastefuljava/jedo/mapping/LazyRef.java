package org.tastefuljava.jedo.mapping;

import java.sql.Connection;
import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.cache.Cache;

class LazyRef<T> implements Ref<T> {
    private final Connection cnt;
    private final Cache<Object,Object> cache;
    private final ClassMapper cm;
    private final Object[] values;
    private boolean isSet;
    private T referee;

    public LazyRef(Connection cnt, Cache<Object, Object> cache, ClassMapper cm,
            Object[] values) {
        this.cnt = cnt;
        this.cache = cache;
        this.cm = cm;
        this.values = values;
    }

    @Override
    public T get() {
        if (isSet) {
            return referee;
        }
        @SuppressWarnings("unchecked")
        T result = (T) cm.load(cnt, cache, values);
        return result;
    }

    @Override
    public void set(T referee) {
        this.referee = referee;
        this.isSet = true;
    }
}
