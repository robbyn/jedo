package org.tastfuljava.jedo.mapping;

import java.beans.Expression;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.tastfuljava.jedo.transaction.WeakCache;

public class Statement {
    private final String sql;
    private final Expression[] params;
    private final boolean generatedKeys;

    public static Statement parse(String sql, Class<?> clazz,
            String[] paramNames) {
        throw new UnsupportedOperationException("parse");
    }

    private Statement(String sql, Expression[] params, boolean generatedKeys) {
        this.sql = sql;
        this.params = params;
        this.generatedKeys = generatedKeys;
    }

    public List<Object> query(Connection cnt, ClassMapper cm,
            WeakCache<Object,Object> cache, Object self, Object[] parms)
            throws SQLException {
        List<Object> result = new ArrayList<>();
        try (PreparedStatement stmt = cnt.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(cm.getInstance(cache, rs));
            }
        }
        return result;
    }

    public Object queryOne(Connection cnt, ClassMapper cm,
            WeakCache<Object,Object> cache, Object self, Object[] parms)
            throws SQLException {
        Object result = null;
        try (PreparedStatement stmt = cnt.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                result = cm.getInstance(cache, rs);
                if (rs.next()) {
                    throw new SQLException("Only one result allowed");
                }
            }
        }
        return result;
    }
}
