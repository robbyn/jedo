package org.tastefuljava.jedo.mapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.expression.Expression;
import org.tastefuljava.jedo.expression.Parameter;
import org.tastefuljava.jedo.expression.Scope;

public class Statement {
    private static final Logger LOG
            = Logger.getLogger(Statement.class.getName());
    private final String sql;
    private final Parameter[] params;
    private final String[] generatedKeys;

    private Statement(Builder builder) {
        SqlParser parser = builder.createParser();
        this.sql = parser.parse(builder.sql);
        this.params = parser.getParameters();
        this.generatedKeys = builder.generatedKeys;
    }

    public boolean hasGeneratedKeys() {
        return generatedKeys != null;
    }

    public PreparedStatement prepare(Connection cnt, Object self,
            Object[] parms) {
        try {
            LOG.fine(()->"Statement.prepare " + sql);
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

    public static class Builder {
        private final Class<?> clazz;
        private String[] generatedKeys;
        private String sql;
        private String[] paramNames;

        public Builder(Class<?> clazz, String[] paramNames) {
            this.clazz = clazz;
            this.paramNames = paramNames;
        }

        public boolean hasGeneratedKeys() {
            return generatedKeys != null;
        }

        public void setGeneratedKeys(String[] keyNames) {
            generatedKeys = keyNames;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public void setParamNames(String[] paramNames) {
            this.paramNames = paramNames;
        }

        private SqlParser createParser() {
            return new SqlParser(clazz, paramNames);
        }

        public Statement build() {
            return new Statement(this);
        }
    }

    private static class SqlParser {
        private final Scope scope;
        private final StringBuilder buf = new StringBuilder();
        private final StringBuilder expr = new StringBuilder();
        private final List<Parameter> params = new ArrayList<>();
        private int st = 1;

        public SqlParser(Class<?> clazz, String[] paramNames) {
            Scope local = null;
            if (clazz != null) {
                local = new Scope.FieldScope(clazz, Expression.THIS, local);
            }
            if (paramNames != null) {
                local = new Scope.ParameterScope(local, paramNames);
            }
            this.scope = local;
        }

        public String parse(String sql) {
            for (char c: sql.toCharArray()) {
                processChar(c);
            }
            return buf.toString();
        }

        public Parameter[] getParameters() {
            return params.toArray(new Parameter[params.size()]);
        }

        private void processChar(char c) {
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
                        buf.append('-');
                        st = 0;
                        processChar(c);
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
                        st = 0;
                        processChar(c);
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
}
