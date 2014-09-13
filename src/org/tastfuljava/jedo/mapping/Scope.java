package org.tastfuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class Scope {
    private static final Logger LOG = Logger.getLogger(Scope.class.getName());

    protected Scope() {
    }

    public abstract Expression resolve(String name);

    public static class ParameterScope extends Scope {
        private final Map<String,Expression> map = new HashMap<>();

        ParameterScope(String[] names) {
            for (int i = 0; i < names.length; ++i) {
                map.put(names[i], new Expression.ParameterExpr(i));
            }
        }

        @Override
        public Expression resolve(String name) {
            return map.get(name);
        }
    }

    public static class ObjectScope extends Scope {
        private final Class<?> clazz;
        private final Expression self;

        public ObjectScope(Class<?> clazz, Expression object) {
            this.clazz = clazz;
            this.self = object;
        }

        @Override
        public Expression resolve(String name) {
            Field f = ClassUtil.getInstanceField(clazz, name);
            return new Expression.FieldExpr(self, f);
        }
    }
}
