package org.tastefuljava.jedo.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.util.ClassUtil;

public abstract class Scope {
    private static final Logger LOG = Logger.getLogger(Scope.class.getName());

    protected Scope() {
    }

    public abstract Expression resolve(String name);

    public static class ParameterScope extends Scope {
        private final Map<String,Expression> map = new HashMap<>();

        public ParameterScope(String[] names) {
            for (int i = 0; i < names.length; ++i) {
                map.put(names[i], new Expression.ParameterExpr(i));
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
            Field f = ClassUtil.getInstanceField(clazz, name);
            if (f == null) {
                throw new JedoException("Field " + name
                        + " not found in class " + clazz.getName());
            }
            return new Expression.FieldExpr(self, f);
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
                Method g = ClassUtil.getPropGetter(clazz, name);
                return new Expression.GetterExpr(self, g);
            }
        }
    }
}
