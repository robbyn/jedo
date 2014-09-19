package org.tastefuljava.jedo;

public interface Ref<T> {
    T get();
    void set(T referee);
}
