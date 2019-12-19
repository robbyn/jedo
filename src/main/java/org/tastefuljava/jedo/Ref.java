package org.tastefuljava.jedo;

public class Ref<T> {
    private T referee;

    public Ref() {
    }

    public Ref(T referee) {
        this.referee = referee;
    }

    public T get() {
        return referee;
    }

    public void set(T referee) {
        this.referee = referee;
    }
}
