package org.tastefuljava.jedo.rel;

import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Storage;

public class JedoRef<T> extends Ref<T> {
    private final Storage pm;
    private final ClassMapper cm;
    private final Object[] values;
    private boolean isSet;

    public JedoRef(Storage pm, ClassMapper cm,
            Object[] values) {
        this.pm = pm;
        this.cm = cm;
        this.values = values;
    }

    @Override
    public T get() {
        if (isSet) {
            return super.get();
        }
        @SuppressWarnings("unchecked")
        T result = (T) pm.loadFromId(cm, values);
        set(result);
        return result;
    }

    @Override
    public void set(T referee) {
        super.set(referee);
        this.isSet = true;
    }
}
