package org.tastefuljava.jedo.conversion;

public interface Converter<S, T> {
    T convert(S value);
}
