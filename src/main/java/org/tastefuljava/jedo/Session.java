package org.tastefuljava.jedo;

import java.util.List;

public interface Session extends AutoCloseable {
    @Override
    void close();
    void commit();

    <T> T load(Class<T> clazz, Object... parms);
    <T> T loadA(Class<T> clazz, Object[] parms);
    <T> T reload(T obj);
    void insert(Object obj);
    void update(Object obj);
    void delete(Object obj);
    void markDirty(Object obj);

    void apply(Object obj, String name, Object... parms);
    void applyA(Object obj, String name, Object[] parms);

    void invoke(Class<?> clazz, String name, Object... parms);
    void invokeA(Class<?> clazz, String name, Object[] parms);

    <T> List<T> query(Class<T> clazz, String name, Object... parms);
    <T> List<T> queryA(Class<T> clazz, String name, Object[] parms);

    <T> T queryOne(Class<T> clazz, String name, Object... parms);
    <T> T queryOneA(Class<T> clazz, String name, Object[] parms);    
}
