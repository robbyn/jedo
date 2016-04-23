package org.tastefuljava.jedo.rel;

import java.sql.Connection;
import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.mapping.ClassMapper;

public class LazyRef<T> extends Ref<T> {
    private final Connection cnt;
    private final Cache<Object,Object> cache;
    private final ClassMapper cm;
    private final Object[] values;
    private boolean isSet;

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
            return super.get();
        }
        @SuppressWarnings("unchecked")
        T result = (T) cm.load(cnt, cache, values);
        return result;
    }

    @Override
    public void set(T referee) {
        super.set(referee);
        this.isSet = true;
    }
}
