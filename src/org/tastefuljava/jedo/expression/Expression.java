package org.tastefuljava.jedo.expression;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.util.Reflection;

public abstract class Expression {
    private static final Logger LOG
            = Logger.getLogger(Expression.class.getName());

    public static final Expression THIS;

    public static Expression parse(Scope scope, String expr) {
        Expression result = null;
        for (String name: expr.split("\\.")) {
            result = scope.resolve(name);
            if (result == null) {
                throw new JedoException(
                        "Could not resolve expression " + expr);
            }
            scope = new Scope.PropertyScope(result.getType(), result);
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
            return null;
        }

        @Override
        public Object evaluate(Object self, Object[] parms) {
            return parms[index];
        }

        @Override
        public String toString() {
            return "#" + index;
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
                throw new JedoException(
                        "Cannot evaluate field " + field.getName());
            }
        }

        @Override
        public String toString() {
            return object == THIS
                    ? field.getName() : object + "." + field.getName();
        }
    }

    public static class RuntimeExpr extends Expression {
        private final Expression object;
        private final String propName;

        public RuntimeExpr(Expression object, String propName) {
            this.object = object;
            this.propName = propName;
        }

        @Override
        public Class<?> getType() {
            return null;
        }

        @Override
        public Object evaluate(Object self, Object[] parms) {
            try {
                Object obj = object.evaluate(self, parms);
                if (obj == null) {
                    return null;
                }
                Method getter = Reflection.getPropGetter(
                        obj.getClass(), propName);
                return getter.invoke(obj);
            } catch (IllegalArgumentException | IllegalAccessException
                    | InvocationTargetException ex) {
                LOG.log(Level.SEVERE, null, ex);
                throw new JedoException(
                        "Cannot evaluate property " + propName);
            }
        }

        @Override
        public String toString() {
            return object == THIS ? propName : object + "." + propName;
        }
    }

    public static class GetterExpr extends Expression {
        private final Expression object;
        private final Method getter;

        public GetterExpr(Expression object, Method getter) {
            this.object = object;
            this.getter = getter;
        }

        @Override
        public Class<?> getType() {
            return getter.getReturnType();
        }

        @Override
        public Object evaluate(Object self, Object[] parms) {
            try {
                Object obj = object.evaluate(self, parms);
                return obj == null ? null : getter.invoke(obj);
            } catch (IllegalArgumentException | IllegalAccessException
                    | InvocationTargetException ex) {
                LOG.log(Level.SEVERE, null, ex);
                throw new JedoException(
                        "Cannot evaluate getter " + getter.getName());
            }
        }

        @Override
        public String toString() {
            String exp = getter.getName() + "()";
            return object == THIS ? exp : object + "." + exp;
        }
    }

    static class Deref extends Expression {
        private Expression ref;
        private Class<?> type;

        public Deref(Expression ref, Class<?> type) {
            this.ref = ref;
            this.type = type;
        }

        @Override
        public Class<?> getType() {
            return type;
        }

        @Override
        public Object evaluate(Object self, Object[] parms) {
            Ref<?> r = (Ref<?>) ref.evaluate(self, parms);
            return r == null ? null : r.get();
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
