package org.tastefuljava.jedo.factory;

import org.tastefuljava.jedo.Session;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.cache.CachedStorage;

public class SessionImpl implements AutoCloseable, Session {
    private static final Logger LOG = Logger.getLogger(SessionImpl.class.getName());

    private final CachedStorage pm;
    private final Mapper mapper;

    public SessionImpl(Connection cnt, Mapper mapper) {
        this.pm = new CachedStorage(cnt);
        this.mapper = mapper;
    }

    public Connection getConnection() {
        return pm.getConnection();
    }

    @Override
    public void close() {
        try {
            pm.close();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    public void commit() {
        pm.commit();
    }

    @Override
    public <T> T load(Class<T> clazz, Object... parms) {
        return loadA(clazz, parms);
    }

    @Override
    public <T> T loadA(Class<T> clazz, Object[] parms) {
        ClassMapper cm = classMapper(clazz);
        return clazz.cast(pm.loadFromId(cm, parms));
    }

    @Override
    public <T> T queryOne(Class<T> clazz, String name, Object... parms) {
        return queryOneA(clazz, name, parms);
    }

    @Override
    public <T> T queryOneA(Class<T> clazz, String name, Object[] parms) {
        ClassMapper cm = classMapper(clazz);
        return clazz.cast(cm.queryOne(pm, name, parms));
    }

    @Override
    public <T> List<T> query(Class<T> clazz, String name, Object... parms) {
        return queryA(clazz, name, parms);
    }

    @Override
    public <T> List<T> queryA(Class<T> clazz, String name, Object[] parms) {
        ClassMapper cm = classMapper(clazz);
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) cm.query(pm, name, null, parms);
        return result;
    }

    @Override
    public void invoke(Class<?> clazz, String name, Object... parms) {
        invokeA(clazz, name, parms);
    }

    @Override
    public void invokeA(Class<?> clazz, String name, Object[] parms) {
        ClassMapper cm = classMapper(clazz);
        cm.invoke(pm, name, parms);
    }

    @Override
    public void apply(Object obj, String name, Object... parms) {
        applyA(obj, name, parms);
    }

    @Override
    public void applyA(Object obj, String name, Object[] parms) {
        ClassMapper cm = classMapper(obj.getClass());
        Object[] p = new Object[parms.length+1];
        p[0] = obj;
        for (int i = 0; i < parms.length; ++i) {
            p[i+1] = parms[i];
        }
        cm.invoke(pm, name, p);
    }

    @Override
    public void insert(Object obj) {
        ClassMapper cm = classMapper(obj.getClass());
        cm.insert(pm, obj);
    }

    @Override
    public void update(Object obj) {
        ClassMapper cm = classMapper(obj.getClass());
        cm.update(pm, obj);
    }

    @Override
    public void delete(Object obj) {
        ClassMapper cm = classMapper(obj.getClass());
        cm.delete(pm, obj);
    }

    @Override
    public void markDirty(Object obj) {
        ClassMapper cm = classMapper(obj.getClass());
        pm.markDirty(cm, obj);
    }

    private ClassMapper classMapper(Class<?> clazz) throws JedoException {
        ClassMapper cm = mapper.getClassMapper(clazz);
        if (cm == null) {
            throw new JedoException(
                    "Class is not mapped " + clazz.getName());
        }
        return cm;
    }
}
