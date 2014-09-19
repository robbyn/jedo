package org.tastefuljava.jedo.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.util.Reflection;

public abstract class Scope {
    private static final Logger LOG = Logger.getLogger(Scope.class.getName());

    protected Scope() {
    }

    public abstract Expression resolve(String name);

    public static class ParameterScope extends Scope {
        private final Map<String,Expression> map = new HashMap<>();

        public ParameterScope(String[] names) {
            for (int i = 0; i < names.length; ++i) {
                Expression expr = new Expression.ParameterExpr(i);
                map.put(names[i], expr);
                map.put("#" + i, expr);
            }
        }

        @Override
        public Expression resolve(String name) {
            return map.get(name);
        }
    }

    public static class FieldScope extends Scope {
        private final Class<?> clazz;
        private final Expression self;

        public FieldScope(Class<?> clazz, Expression object) {
            this.clazz = clazz;
            this.self = object;
        }

        @Override
        public Expression resolve(String name) {
            Field f = Reflection.getInstanceField(clazz, name);
            if (f == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            Expression result = new Expression.FieldExpr(self, f);
            if (f.getType() == Ref.class) {
                result = new Expression.Deref(result,
                        Reflection.getReferencedType(f));
            }
            return result;
        }
    }

    public static class PropertyScope extends Scope {
        private final Class<?> clazz;
        private final Expression self;

        public PropertyScope(Class<?> clazz, Expression object) {
            this.clazz = clazz;
            this.self = object;
        }

        @Override
        public Expression resolve(String name) {
            if (clazz == null) {
                return new Expression.RuntimeExpr(self, name);
            } else {
                Method g = Reflection.getPropGetter(clazz, name);
                if (g == null) {
                    throw new JedoException("No getter found for " + name
                            + " in class " + clazz.getName());
                }
                return new Expression.GetterExpr(self, g);
            }
        }
    }
}
