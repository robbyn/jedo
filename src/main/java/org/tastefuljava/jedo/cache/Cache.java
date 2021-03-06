package org.tastefuljava.jedo.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class Cache<K,T> {
    private final Map<K,Ref> map = new HashMap<>();
    private ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    public void put(K key, T obj) {
        cleanup();
        map.put(key, new Ref(key, obj));
    }

    public T get(K key) {
        cleanup();
        Ref ref = map.get(key);
        return ref == null ? null : ref.get();
    }

    public T remove(K key) {
        cleanup();
        Ref ref = map.remove(key);
        return ref == null ? null : ref.get();
    }

    public void clear() {
        map.clear();
        refQueue = new ReferenceQueue<>();
    }

    private void cleanup() {
        while (true) {
            Ref ref = (Ref) refQueue.poll();
            if (ref == null) {
                break;
            }
            map.remove(ref.key);
        }
    }

    private class Ref extends WeakReference<T> {
        private final K key;

        public Ref(K key, T obj) {
            super(obj, refQueue);
            this.key = key;
        }
    }
}
