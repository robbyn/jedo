package org.tastefuljava.jedo.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.conversion.Conversion;
import org.tastefuljava.jedo.util.Reflection;

public class Parameter {
    private static final Logger LOG
            = Logger.getLogger(Parameter.class.getName());

    private static final String ID = "[A-Za-z_][A-Za-z_0-9]*";
    private static final Pattern PATTERN = Pattern.compile(
            "^((?:#[1-9][0-9]*|" + ID + ")(?:\\." + ID +")*)"
                + "(?:\\:(" + ID + "(?:\\." + ID +")*)?(?:\\:(" + ID + "))?)?"
                + "(?:\\:([1-9][0-9]*))?$");

    private final Expression expr;
    private final Class<?> javaType;
    private final Integer type;
    private final Integer length;

    public static Parameter parse(Scope scope, String s) {
        Matcher matcher = PATTERN.matcher(s);
        if (!matcher.matches()) {
            throw new JedoException("Bad parameter syntax: " + s);
        }
        final Expression expr = Expression.parse(scope, matcher.group(1));
        Class<?> javaType = null;
        final String jtype = matcher.group(2);
        if (jtype != null) {
            javaType = Reflection.loadClass(jtype, "java.lang");
        }
        Integer type = null;
        final String stype = matcher.group(3);
        if (stype != null) {
            type = Reflection.getConstant(Types.class, stype, int.class);
        }
        Integer length = null;
        final String slength = matcher.group(4);
        if (slength != null) {
            length = Integer.parseInt(slength);
        }
        return new Parameter(expr, javaType, type, length);
    }

    private Parameter(Expression expr, Class<?> javaType, Integer type,
            Integer length) {
        this.expr = expr;
        this.javaType = javaType;
        this.type = type;
        this.length = length;
    }

    public void set(PreparedStatement stmt, int ix, Object self,
            Object[] parms) {
        try {
            Object value = expr.evaluate(self, parms);
            if (value != null && javaType != null) {
                value = Conversion.convert(value, javaType);
            }
            if (type == null) {
                stmt.setObject(ix, value);
            } else if (length == null) {
                stmt.setObject(ix, value, type);
            } else {
                stmt.setObject(ix, value, type, length);
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(expr);
        if (javaType != null) {
            buf.append(':');
            buf.append(javaType.getName());
        } else if (type != null) {
            buf.append(':');
        }
        if (type != null) {
            buf.append(':');
            buf.append(type);
        }
        if (length != null) {
            buf.append(':');
            buf.append(length);
        }
        return buf.toString();
    }
}
