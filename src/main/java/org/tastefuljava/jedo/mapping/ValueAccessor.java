package org.tastefuljava.jedo.mapping;

public interface ValueAccessor {
    public Object getValue(Object object);
    public void setValue(Object obj, Object value);
}
