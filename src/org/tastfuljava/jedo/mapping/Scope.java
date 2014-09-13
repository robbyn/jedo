package org.tastfuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class Scope {
    private static final Logger LOG = Logger.getLogger(Scope.class.getName());
 
    private final Scope link;

    protected Scope(Scope link) {
        this.link = link;
    }

    public abstract Expression resolve(String name);

    private Expression resolve1(String name) {
        return link == null ? null : link.resolve(name);
    }

    public static class ParameterScope extends Scope {
        private final Map<String,Expression> map = new HashMap<>();

        ParameterScope(Scope link, String[] names) {
            super(link);
            for (int i = 0; i < names.length; ++i) {
                map.put(names[i], new Expression.ParameterExpr(i));
            }
        }

        @Override
        public Expression resolve(String name) {
            Expression result = map.get(name);
            return result != null ? result : super.resolve1(name);
        }
    }

    public static class ObjectScope extends Scope {
        private final Class<?> clazz;
        private final Expression self;

        public ObjectScope(Scope link, Class<?> clazz, Expression object) {
            super(link);
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
