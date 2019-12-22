package org.tastefuljava.jedo.mapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.expression.Expression;
import org.tastefuljava.jedo.expression.Parameter;
import org.tastefuljava.jedo.expression.Scope;
import org.tastefuljava.jedo.util.XMLWriter;

public class Statement {
    private static final Logger LOG
            = Logger.getLogger(Statement.class.getName());
    private final String sql;
    private final Parameter[] params;
    private final String[] generatedKeys;

    private Statement(Builder builder) {
        this.sql = builder.buf.toString();
        this.params = builder.params.toArray(
                new Parameter[builder.params.size()]);
        this.generatedKeys = builder.generatedKeys;
    }

    public PreparedStatement prepare(Connection cnt, Object self,
            Object[] parms) {
        try {
            boolean ok = false;
            PreparedStatement stmt = generatedKeys == null
                    ? cnt.prepareStatement(sql)
                    : cnt.prepareStatement(sql, generatedKeys);
            try {
                for (int i = 0; i < params.length; ++i) {
                    params[i].set(stmt, i+1, self, parms);
                }
                ok = true;
                return stmt;
            } finally {
                if (!ok) {
                    stmt.close();
                }
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    public void collectKeys(PreparedStatement stmt, SimpleFieldMapper[] props,
            Object obj) throws JedoException, SQLException {
        if (generatedKeys != null) {
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new JedoException(
                            "Could not get generated keys");
                }
                int ix = 0;
                for (SimpleFieldMapper prop : props) {
                    prop.setValue(obj, prop.fromResultSet(rs, ++ix));
                }
            }
        }
    }

    public void executeUpdate(Connection cnt, Object self, Object[] parms) {
        try (PreparedStatement pstmt = prepare(cnt, self, parms)) {
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }        
    }

    public void writeTo(XMLWriter out, String type, String name) {
        out.startTag(type);
        out.attribute("name", name);
        for (Parameter param: params) {
            param.writeTo(out);
        }
        out.data(sql);
        out.endTag();
    }

    public static class Builder {
        private final Scope scope;
        private String[] generatedKeys;
        private final List<Parameter> params = new ArrayList<>();
        private final StringBuilder buf = new StringBuilder();
        private final StringBuilder expr = new StringBuilder();

        private int st = 1;

        public Builder(Class<?> clazz, String[] paramNames) {
            this.scope = paramNames == null
                ? new Scope.FieldScope(clazz, Expression.THIS)
                : new Scope.ParameterScope(paramNames);
        }

        public void setGeneratedKeys(String[] keyNames) {
            generatedKeys = keyNames;
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
                            params.add(Parameter.parse(
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

        public Statement build() {
            if (buf.length() > 0 && st == 1) {
                buf.setLength(buf.length()-1);
            }
            return new Statement(this);
        }
    }
}
