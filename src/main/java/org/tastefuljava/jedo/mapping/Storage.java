package org.tastefuljava.jedo.mapping;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

public interface Storage extends AutoCloseable {
    public void query(ValueMapper cm, Statement stmt, Object self, Object[] parms,
            Collection<Object> result);
    public void query(ValueMapper km, ValueMapper em, Statement fetch,
            Object parent, Object[] object, Map<Object, Object> map);
    public Object queryOne(ValueMapper cm, Statement stmt, Object[] parms);
    public int execute(Statement stmt, Object self, Object[] parms);
    public void insert(ClassMapper cm, Statement stmt, Object self,
            Object[] parms);
    public void update(ClassMapper cm, Statement stmt, Object self,
            Object[] parms);
    public void delete(ClassMapper cm, Statement stmt, Object self);
    public Object loadFromResultSet(ClassMapper cm, ResultSet rs);
    public Object loadFromId(ClassMapper cm, Object[] values);
    public void markDirty(Flushable obj);
    public void dispose(Flushable obj);
}
