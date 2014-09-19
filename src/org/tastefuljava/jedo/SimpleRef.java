package org.tastefuljava.jedo;

public class SimpleRef<T> implements Ref<T> {
    private T referee;

    public SimpleRef() {
    }

    public SimpleRef(T referee) {
        this.referee = referee;
    }

    @Override
    public T get() {
        return referee;
    }

    @Override
    public void set(T referee) {
        this.referee = referee;
    }
}
