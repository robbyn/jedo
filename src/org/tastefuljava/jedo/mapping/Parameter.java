package org.tastefuljava.jedo.mapping;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.expression.Expression;
import org.tastefuljava.jedo.expression.Scope;
import org.tastefuljava.jedo.util.ClassUtil;

public class Parameter {
    private static final Logger LOG
            = Logger.getLogger(Parameter.class.getName());

    private static final String ID = "[A-Za-z_][A-Za-z_0-9]*";
    private static final Pattern PATTERN = Pattern.compile(
            "^((?:#[1-9][0-9]*|" + ID + ")(?:\\." + ID +")*)"
                + "(?:\\:(" + ID + "(?:\\:([1-9][0-9]*))?))?$");

    private final Expression expr;
    private final Integer type;
    private final Integer length;

    public static Parameter parse(Scope scope, String s) {
        Matcher matcher = PATTERN.matcher(s);
        if (!matcher.matches()) {
            throw new JedoException("Bad parameter syntax: " + s);
        }
        final Expression expr = Expression.parse(scope, matcher.group(1));
        final String stype = matcher.group(2);
        if (stype == null) {
            return new Parameter(expr);
        }
        int type = ClassUtil.getConstant(Types.class, stype, int.class);
        final String slength = matcher.group(3);
        if (slength == null) {
            return new Parameter(expr, type);
        }
        return new Parameter(expr, type, Integer.parseInt(slength));
    }

    private Parameter(Expression expr) {
        this(expr, null);
    }

    private Parameter(Expression expr, Integer type) {
        this(expr, type, null);
    }

    private Parameter(Expression expr, Integer type, Integer length) {
        this.expr = expr;
        this.type = type;
        this.length = length;
    }

    public void set(PreparedStatement stmt, int ix, Object self,
            Object[] parms) {
        try {
            Object value = expr.evaluate(self, parms);
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

    void writeTo(XMLWriter out) {
        out.startTag("parameter");
        out.attribute("value", toString());
        out.endTag();
    }
}
