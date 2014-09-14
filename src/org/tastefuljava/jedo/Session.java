package org.tastefuljava.jedo;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.cache.Cache;

public class Session implements Closeable {
    private static final Logger LOG = Logger.getLogger(Session.class.getName());

    private final Connection cnt;
    private final Mapper mapper;
    private final Cache cache = new Cache();

    public Session(Connection cnt, Mapper mapper) {
        this.cnt = cnt;
        this.mapper = mapper;
    }

    public <T> T load(Class<T> clazz, Object... parms) {
        return loadA(clazz, parms);
    }

    public <T> T loadA(Class<T> clazz, Object[] parms) {
        try {
            ClassMapper cm = mapper.getClassMapper(clazz);
            if (cm == null) {
                throw new JedoException(
                        "Class is not mapped " + clazz.getName());
            }
            return clazz.cast(cm.load(cnt, cache, parms));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public <T> T queryOne(Class<T> clazz, String name, Object... parms) {
        return queryOneA(clazz, name, parms);
    }

    public <T> T queryOneA(Class<T> clazz, String name, Object[] parms) {
        try {
            ClassMapper cm = mapper.getClassMapper(clazz);
            if (cm == null) {
                throw new JedoException(
                        "Class is not mapped " + clazz.getName());
            }
            return clazz.cast(cm.queryOne(cnt, cache, name, parms));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public <T> List<T> query(Class<T> clazz, String name, Object... parms) {
        return queryA(clazz, name, parms);
    }

    public <T> List<T> queryA(Class<T> clazz, String name, Object[] parms) {
        try {
            ClassMapper cm = mapper.getClassMapper(clazz);
            if (cm == null) {
                throw new JedoException(
                        "Class is not mapped " + clazz.getName());
            }
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) cm.query(cnt, cache, name, parms);
            return result;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public void commit() {
        try {
            cache.clear();
            cnt.commit();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            cache.clear();
            if (!cnt.isClosed()) {
                try {
                    cnt.rollback();
                } finally {
                    cnt.close();
                }
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }
}
