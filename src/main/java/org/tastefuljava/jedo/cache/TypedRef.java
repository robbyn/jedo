package org.tastefuljava.jedo.cache;

import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Flushable;
import org.tastefuljava.jedo.mapping.Storage;

class TypedRef implements Flushable {
    final ClassMapper cm;
    final Object obj;

    TypedRef(ClassMapper cm, Object obj) {
        this.cm = cm;
        this.obj = obj;
    }

    @Override
    public void flush(Storage pm) {
        cm.update(pm, obj);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || other.getClass() != getClass()) {
            return false;
        }
        TypedRef ref = (TypedRef)other;
        return cm == ref.cm && obj == ref.obj;
    }

    @Override
    public int hashCode() {
        return obj.hashCode();
    }
}
