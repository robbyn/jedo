package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.expression.Scope;
import org.tastefuljava.jedo.expression.Expression;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;

public class Statement {
    private final String sql;
    private final Expression[] params;
    private final boolean generatedKeys;

    private Statement(Builder builder) {
        this.sql = builder.buf.toString();
        this.params = builder.params.toArray(
                new Expression[builder.params.size()]);
        this.generatedKeys = builder.generatedKeys;
    }

    public List<Object> query(Connection cnt, ClassMapper cm,
            Cache<?,?> ucache, Object[] parms)
            throws SQLException {
        @SuppressWarnings("unchecked")
        Cache<Object,Object> cache = (Cache<Object,Object>)ucache;
        List<Object> result = new ArrayList<>();
        try (PreparedStatement stmt = cnt.prepareStatement(sql)) {
            bindParams(stmt, null, parms);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(cm.getInstance(cache, rs));
                }
            }
        }
        return result;
    }

    public Object queryOne(Connection cnt, ClassMapper cm,
            Cache<?,?> cache, Object[] parms)
            throws SQLException {
        Object result = null;
        try (PreparedStatement stmt = cnt.prepareStatement(sql)) {
            bindParams(stmt, null, parms);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result = cm.getInstance(cache, rs);
                    if (rs.next()) {
                        throw new JedoException("Only one result allowed");
                    }
                }
            }
        }
        return result;
    }

    public void update(Connection cnt, ClassMapper cm, Object obj)
            throws SQLException {
        int gk = generatedKeys
                ? PreparedStatement.RETURN_GENERATED_KEYS
                : PreparedStatement.NO_GENERATED_KEYS;
        try (PreparedStatement stmt = cnt.prepareStatement(sql, gk)) {
            bindParams(stmt, obj, null);
            stmt.executeUpdate();
            if (generatedKeys) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new JedoException(
                                "Could not get generated keys");
                    }
                    cm.getGeneratedKeys(rs, obj);
                }
            }
        }
    }

    void writeTo(XMLWriter out, String type, String name) {
        out.startTag(type);
        out.attribute("name", name);
        for (Expression expr: params) {
            out.startTag("parameter");
            out.attribute("value", expr.toString());
            out.endTag();
        }
        out.data(sql);
        out.endTag();
    }

    private void bindParams(PreparedStatement stmt, Object self, Object[] parms)
            throws SQLException {
        for (int i = 0; i < params.length; ++i) {
            stmt.setObject(i+1, params[i].evaluate(self, parms));
        }
    }

    public static class Builder {
        private final Scope scope;
        private boolean generatedKeys;
        private final List<Expression> params = new ArrayList<>();
        private final StringBuilder buf = new StringBuilder();
        private final StringBuilder expr = new StringBuilder();
        int st = 1;

        public Builder(Class<?> clazz, String[] paramNames) {
            this.scope = paramNames == null
                ? new Scope.FieldScope(clazz, Expression.THIS)
                : new Scope.ParameterScope(paramNames);
        }

        public void setGeneratedKeys(boolean newValue) {
            generatedKeys = newValue;
        }

        public void addChars(char[] chars, int start, int length) {
            int end = start+length;
            for (int i = start; i < end; ++i) {
                char c = chars[i];
                switch (st) {
                    case 0:
                        if (Character.isWhitespace(c)) {
                            buf.append(' ');
                            st = 1;
                        } else if (c == '-') {
                            st = 2;
                        } else if (c == '$') {
                            st = 4;
                        } else if (c == '"') {
                            buf.append(c);
                            st = 6;
                        } else if (c == '\'') {
                            buf.append(c);
                            st = 7;
                        } else {
                            buf.append(c);
                        }
                        break;
                    case 1:
                        if (Character.isWhitespace(c)) {
                            // do nothing
                        } else if (c == '-') {
                            st = 2;
                        } else if (c == '$') {
                            st = 4;
                        } else if (c == '"') {
                            st = 6;
                        } else if (c == '\'') {
                            st = 7;
                        } else {
                            buf.append(c);
                            st = 0;
                        }
                        break;
                    case 2:
                        if (c == '-') {
                            st = 3;
                        } else {
                            buf.append(' ');
                        }
                        break;
                    case 3:
                        if (c == '\r' || c == '\n') {
                            st = 1;
                        }
                        break;
                    case 4:
                        if (c == '{') {
                            st = 5;
                            expr.setLength(0);
                        } else {
                            buf.append('$');
                            buf.append(c);
                            st = 0;
                        }
                        break;
                    case 5:
                        if (c == '}') {
                            buf.append('?');
                            params.add(Expression.parse(
                                    scope, expr.toString()));
                            st = 0;
                        } else {
                            expr.append(c);
                        }
                        break;
                    case 6:
                        buf.append(c);
                        if (c == '"') {
                            st = 0;
                        }
                        break;
                    case 7:
                        buf.append(c);
                        if (c == '\'') {
                            st = 0;
                        }
                        break;
                }
            }
        }

        public Statement getStatement() {
            if (buf.length() > 0 && st == 1) {
                buf.setLength(buf.length()-1);
            }
            return new Statement(this);
        }
    }
}
