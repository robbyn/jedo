package org.tastefuljava.jedo.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.Session;
import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Statement;
import org.tastefuljava.jedo.mapping.Storage;

public class CachedStorage implements Storage {
    private static final Logger LOG = Logger.getLogger(Session.class.getName());
    private final Connection cnt;
    private final Cache<ObjectId,Object> cache = new Cache<>();
    private boolean closeConnection = true;

    public CachedStorage(Connection cnt) {
        this.cnt = cnt;
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
                    if (closeConnection) {
                        cnt.close();
                    }
                }
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public boolean getCloseConnection() {
        return closeConnection;
    }

    public void setCloseConnection(boolean closeConnection) {
        this.closeConnection = closeConnection;
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
    public void query(ClassMapper cm, Statement stmt, Object self, Object[] parms,
            Collection<Object> result) {
         try (PreparedStatement pstmt = prepareStatement(stmt, self, parms);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(loadFromResultSet(cm, rs));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }        
    }

    @Override
    public Object queryOne(ClassMapper cm, Statement stmt, Object[] parms) {
        Object result = null;
        try (PreparedStatement pstmt = prepareStatement(stmt, null, parms);
                ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                result = loadFromResultSet(cm, rs);
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
        try (PreparedStatement pstmt = prepareStatement(stmt, self, null)) {
            pstmt.executeUpdate();
            cache.remove(getObjectId(cm, self));
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    public void executeUpdate(Statement stmt, Object self, Object[] parms) {
        stmt.executeUpdate(cnt, self, parms);
    }

    @Override
    public void insert(ClassMapper cm, Statement stmt, Object self,
            Object[] parms) {
        try (PreparedStatement pstmt = prepareStatement(stmt, self, parms)) {
            pstmt.executeUpdate();
            cm.collectKeys(stmt, pstmt, self);
            cache.put(getObjectId(cm, self), self);
            cm.afterInsert(this, self);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    private ObjectId getObjectId(ClassMapper cm, Object obj) {
        return new ObjectId(cm.getMappedClass(), cm.getIdValues(obj));
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
