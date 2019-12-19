package org.tastefuljava.jedo.mapping;

public enum FetchMode {
    EAGER, LAZY;

    public static FetchMode fromString(String s) {
        return valueOf(s.toUpperCase());
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
