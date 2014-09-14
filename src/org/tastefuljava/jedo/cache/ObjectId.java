package org.tastefuljava.jedo.cache;

public class ObjectId {
    private final Class<?> clazz;
    private final Object[] values;
    private transient int hash = -1;

    public ObjectId(Class<?> clazz, Object[] values) {
        if (values == null || values.length < 1) {
            throw new IllegalArgumentException("At least one value is needed");
        }
        this.clazz = clazz;
        this.values = values.clone();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(clazz.getName());
        buf.append('[');
        buf.append(values[0]);
        for (int i = 1; i < values.length; ++i) {
            buf.append(',');
            buf.append(values[i]);
        }
        buf.append(']');
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ObjectId)) {
            return false;
        }
        ObjectId other = (ObjectId) obj;
        if (other.clazz != clazz) {
            return false;
        }
        if (other.values.length != values.length) {
            return false;
        }
        for (int i = 0; i < values.length; ++i) {
            if (!values[i].equals(other.values[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (hash == -1) {
            int h = clazz.hashCode();
            for (Object value : values) {
                h = 37 * h + value.hashCode();
            }
            hash = h;
        }
        return hash;
    }
}
