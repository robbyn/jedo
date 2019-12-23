package org.tastefuljava.jedo.mapping;

import java.sql.ResultSet;
import java.util.Collection;

public interface Storage extends AutoCloseable {
    public Object loadFromResultSet(ClassMapper cm, ResultSet rs);
    public void query(ClassMapper aThis, Statement stmt, Object[] parms,
            Collection<Object> result);
    public Object queryOne(ClassMapper cm, Statement stmt, Object[] parms);
    public void executeUpdate(Statement stmt, Object self, Object[] parms);
    public void delete(ClassMapper cm, Statement stmt, Object self);
    public void insert(ClassMapper cm, Statement stmt, Object self,
            Object[] parms);
    public Object loadFromId(ClassMapper cm, Object[] values);
}
