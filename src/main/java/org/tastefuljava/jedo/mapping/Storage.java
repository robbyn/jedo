package org.tastefuljava.jedo.mapping;

import java.util.Collection;

public interface Storage extends AutoCloseable {
    public void query(ClassMapper cm, Statement stmt, Object self, Object[] parms,
            Collection<Object> result);
    public Object queryOne(ClassMapper cm, Statement stmt, Object[] parms);
    public void execute(Statement stmt, Object self, Object[] parms);
    public void insert(ClassMapper cm, Statement stmt, Object self,
            Object[] parms);
    public void update(ClassMapper cm, Statement stmt, Object self,
            Object[] parms);
    public void delete(ClassMapper cm, Statement stmt, Object self);
    public Object loadFromId(ClassMapper cm, Object[] values);
    public void markDirty(Flushable obj);
    public void dispose(Flushable obj);
}
