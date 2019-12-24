package org.tastefuljava.jedo.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.Ref;
import org.tastefuljava.jedo.util.Reflection;

public abstract class Scope {
    private final Scope link;

    protected Scope(Scope link) {
        this.link = link;
    }

    public Expression resolve(String name) {
        if (link == null) {
            throw new JedoException("Unresolved name " + name);
        }
        return link.resolve(name);
    }

    public static class ParameterScope extends Scope {
        private final Map<String,Expression> map = new HashMap<>();

        public ParameterScope(String[] names, Scope link) {
            super(link);
            for (int i = 0; i < names.length; ++i) {
                Expression expr = new Expression.ParameterExpr(i);
                map.put(names[i], expr);
                map.put("#" + i, expr);
            }
        }

        @Override
        public Expression resolve(String name) {
            Expression result = map.get(name);
            if (result != null) {
                return result;
            }
            return super.resolve(name);
        }
    }

    public static class FieldScope extends Scope {
        private final Class<?> clazz;
        private final Expression self;

        public FieldScope(Class<?> clazz, Expression object, Scope link) {
            super(link);
            this.clazz = clazz;
            this.self = object;
        }

        @Override
        public Expression resolve(String name) {
            Field f = Reflection.getInstanceField(clazz, name);
            if (f == null) {
                return super.resolve(name);
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

        public PropertyScope(Class<?> clazz, Expression object, Scope link) {
            super(link);
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
