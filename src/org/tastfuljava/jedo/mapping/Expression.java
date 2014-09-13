package org.tastfuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Expression {
    private static final Logger LOG
            = Logger.getLogger(Expression.class.getName());

    public static final Expression THIS;

    public static Expression parse(Scope scope, String expr) {
        Expression result = null;
        for (String name: expr.split("\\.")) {
            result = scope.resolve(name);
            if (result == null) {
                throw new IllegalArgumentException(
                        "Could not resolve expression " + expr);
            }
            scope = new Scope.ObjectScope(result.getType(), result);
        }
        return result;
    }

    public abstract Class<?> getType();
    public abstract Object evaluate(Object self, Object[] parms);

    public static class ParameterExpr extends Expression {
        private final int index;

        public ParameterExpr(int index) {
            this.index = index;
        }

        @Override
        public Class<?> getType() {
            return Object.class;
        }

        @Override
        public Object evaluate(Object self, Object[] parms) {
            return parms[index];
        }
    }

    public static class FieldExpr extends Expression {
        private final Expression object;
        private final Field field;

        public FieldExpr(Expression object, Field field) {
            this.object = object;
            this.field = field;
        }

        @Override
        public Class<?> getType() {
            return field.getType();
        }

        @Override
        public Object evaluate(Object self, Object[] parms) {
            try {
                Object obj = object.evaluate(self, parms);
                return obj == null ? null : field.get(obj);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOG.log(Level.SEVERE, null, ex);
                throw new RuntimeException(
                        "Cannot evaluate field " + field.getName());
            }
        }
    }

    static {
        THIS = new Expression() {
            @Override
            public Object evaluate(Object self, Object[] parms) {
                return self;
            }

            @Override
            public Class<?> getType() {
                return Object.class;
            }
        };
    }
}
