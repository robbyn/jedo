package org.tastefuljava.jedo.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.tastefuljava.jedo.rel.ObjectId;

public class Cache {
    private static final Logger LOG = Logger.getLogger(Cache.class.getName());

    private final Map<ObjectId,Ref> map = new HashMap<>();
    private ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    public void put(ObjectId key, Object obj) {
        cleanup();
        map.put(key, new Ref(key, obj));
    }

    public Object get(ObjectId key) {
        cleanup();
        Ref ref = map.get(key);
        return ref == null ? null : ref.get();
    }

    public Object getOrPut(ObjectId key, Object obj) {
        cleanup();
        Ref ref = map.get(key);
        if (ref != null) {
            Object result = ref.get();
            if (result != null) {
                return result;
            }
        }
        map.put(key, new Ref(key, obj));
        return obj;
    }

    public Object remove(ObjectId key) {
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
            @SuppressWarnings("unchecked")
            Ref ref = (Ref) refQueue.poll();
            if (ref == null) {
                break;
            }
            map.remove(ref.key);
        }
    }

    private class Ref extends WeakReference<Object> {
        private final ObjectId key;

        public Ref(ObjectId key, Object obj) {
            super(obj, refQueue);
            this.key = key;
        }
    }
}
