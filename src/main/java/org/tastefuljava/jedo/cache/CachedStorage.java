package org.tastefuljava.jedo.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Flushable;
import org.tastefuljava.jedo.mapping.Statement;
import org.tastefuljava.jedo.mapping.Storage;
import org.tastefuljava.jedo.mapping.ValueMapper;

public class CachedStorage implements Storage {
    private static final Logger LOG
            = Logger.getLogger(CachedStorage.class.getName());
    private final Connection cnt;
    private final Cache<ObjectId,Object> cache = new Cache<>();
    private final Set<Flushable> dirtyObjects = new LinkedHashSet<>();

    public CachedStorage(Connection cnt) {
        this.cnt = cnt;
    }

    public Connection getConnection() {
        return cnt;
    }

    public void commit() {
        try {
            flush();
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
            dirtyObjects.clear();
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

    @Override
    public void query(ValueMapper cm, Statement stmt, Object self, Object[] parms,
            Collection<Object> result) {
        flush();
        try (PreparedStatement pstmt = prepareStatement(stmt, self, parms);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(cm.fromResultSet(this, self, rs, null));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }        
    }

    @Override
    public void query(ValueMapper km, ValueMapper cm, Statement stmt,
            Object self, Object[] parms, Map<Object, Object> map) {
        flush();
        try (PreparedStatement pstmt = prepareStatement(stmt, self, parms);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Object key = km.fromResultSet(this, self, rs, null);
                Object value = cm.fromResultSet(this, self, rs, null);
                map.put(key, value);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }        
    }

    @Override
    public Object queryOne(ValueMapper cm, Statement stmt, Object[] parms) {
        flush();
        Object result = null;
        try (PreparedStatement pstmt = prepareStatement(stmt, null, parms);
                ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                result = cm.fromResultSet(this, null, rs, null);
                if (rs.next()) {
                    throw new JedoException("Only one result allowed");
                }
            }
        } catch (final SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
        return result;
    }

    @Override
    public void delete(ClassMapper cm, Statement stmt, Object self) {
        cm.beforeDelete(this, self);
        dispose(new TypedRef(cm, self));
        try (PreparedStatement pstmt = prepareStatement(stmt, self, null)) {
            pstmt.executeUpdate();
            cache.remove(getObjectId(cm, self));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    public void execute(Statement stmt, Object self, Object[] parms) {
        try (final PreparedStatement pstmt = stmt.prepare(cnt, self, parms)) {
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    public void update(ClassMapper cm, Statement stmt, Object self,
            Object[] parms) {
        cm.beforeUpdate(this, self);
        try (PreparedStatement pstmt = prepareStatement(stmt, self, parms)) {
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
        cm.afterUpdate(this, self);
    }

    @Override
    public void insert(ClassMapper cm, Statement stmt, Object self,
            Object[] parms) {
        try (PreparedStatement pstmt = prepareStatement(stmt, self, parms)) {
            pstmt.executeUpdate();
            if (stmt.hasGeneratedKeys()) {
                try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new JedoException("Could not get generated keys");
                    }
                    cm.collectKeys(this, self, rs);
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    throw new JedoException(ex.getMessage());
                }
            }
            cache.put(getObjectId(cm, self), self);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
        cm.afterInsert(this, self);
    }

    @Override
    public Object loadFromResultSet(ClassMapper cm, ResultSet rs) {
        ObjectId oid = makeObjectId(
                cm.getMappedClass(), cm.getIdValuesFromResultSet(rs));
        Object obj;
        if (oid != null) {
            obj = cache.get(oid);
            if (obj != null) {
                return obj;
            }
        }
        obj = cm.newInstance();
        if (oid != null) {
            cache.put(oid, obj);
        }
        cm.setFieldsFromResultSet(this, obj, rs);
        return obj;
    }

    @Override
    public Object loadFromId(ClassMapper cm, Object[] values) {
        ObjectId oid = makeObjectId(cm.getMappedClass(), values);
        Object obj;
        if (oid != null) {
            obj = cache.get(oid);
            if (obj != null) {
                return obj;
            }
        }
        return cm.load(this, values);
    }

    @Override
    public void markDirty(Flushable obj) {
        dirtyObjects.add(obj);
    }

    @Override
    public void dispose(Flushable obj) {
        dirtyObjects.remove(obj);
    }

    public void markDirty(ClassMapper cm, Object obj) {
        markDirty(new TypedRef(cm, obj));
    }

    public void dispose(ClassMapper cm, Object obj) {
        dispose(new TypedRef(cm, obj));
    }

    private void flush() {
        for (Flushable f: dirtyObjects) {
            f.flush(this);
        }
        dirtyObjects.clear();
    }

    private static ObjectId getObjectId(ClassMapper cm, Object obj) {
        return makeObjectId(cm.getMappedClass(), cm.getIdValues(obj));
    }

    private static ObjectId makeObjectId(Class<?> clazz, Object[] values) {
        for (Object value: values) {
            if (value != null) {
                return new ObjectId(clazz, values);
            }
        }
        return null; // all values are null
    }

    private PreparedStatement prepareStatement(
            Statement stmt, Object self, Object[] parms) {
        return stmt.prepare(cnt, self, parms);
    }
}
