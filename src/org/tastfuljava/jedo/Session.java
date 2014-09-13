package org.tastfuljava.jedo;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastfuljava.jedo.mapping.ClassMapper;
import org.tastfuljava.jedo.mapping.Mapper;
import org.tastfuljava.jedo.transaction.Cache;

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
                throw new IllegalArgumentException(
                        "Class is not mapped " + clazz.getName());
            }
            return clazz.cast(cm.load(cnt, cache, parms));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    public <T> T queryOne(Class<T> clazz, String name, Object... parms) {
        return queryOneA(clazz, name, parms);
    }

    public <T> T queryOneA(Class<T> clazz, String name, Object[] parms) {
        try {
            ClassMapper cm = mapper.getClassMapper(clazz);
            if (cm == null) {
                throw new IllegalArgumentException(
                        "Class is not mapped " + clazz.getName());
            }
            return clazz.cast(cm.queryOne(cnt, cache, name, parms));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    public void commit() {
        try {
            cache.clear();
            cnt.commit();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
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
            throw new IOException(ex.getMessage());
        }
    }
}
