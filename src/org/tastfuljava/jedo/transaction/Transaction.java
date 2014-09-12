package org.tastfuljava.jedo.transaction;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Transaction implements DataProvider {
    private final DataProvider dp;
    private final WeakCache<Object,Object> cache = new WeakCache<>();
    private final List<Runnable> opQueue = new LinkedList<>();

    public Transaction(DataProvider dp) {
        this.dp = dp;
    }

    @Override
    public Object load(Object id) {
        Object obj = cache.get(id);
        if (obj == null) {
            obj = dp.load(id);
            if (obj != null) {
                cache.put(id, obj);
            }
        }
        return obj;
    }

    public Object getFromCache(Object id) {
        return cache.get(id);
    }

    @Override
    public void insert(final Object obj) {
        cache.put(dp.getIdentity(obj), obj);
        opQueue.add(new Runnable() {
            @Override
            public void run() {
                dp.insert(obj);
            }
        });
    }

    @Override
    public void update(final Object obj) {
        opQueue.add(new Runnable() {
            @Override
            public void run() {
                dp.update(obj);
            }
        });
    }

    @Override
    public void delete(final Object id) {
        cache.remove(id);
        opQueue.add(new Runnable() {
            @Override
            public void run() {
                dp.delete(id);
            }
        });
    }

    public void flush() {
        while (true) {
            Runnable op = opQueue.remove(0);
            if (op == null) break;
            op.run();
        }
    }

    public void commit() {
        flush();
        cache.clear();
        opQueue.clear();
    }

    @Override
    public Object getIdentity(Object obj) {
        return dp.getIdentity(obj);
    }

    @Override
    public void close() throws IOException {
        cache.clear();
        opQueue.clear();
        dp.close();
    }
}
