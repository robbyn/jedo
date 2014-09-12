package org.tastfuljava.jedo.transaction;

import java.io.Closeable;

public interface DataProvider extends Closeable {
    Object getIdentity(Object obj);
    Object load(Object id);
    void insert(Object obj);
    void update(Object obj);
    void delete(Object obj);
}
