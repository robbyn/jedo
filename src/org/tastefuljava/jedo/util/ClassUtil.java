package org.tastefuljava.jedo.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public class ClassUtil {
    private static final Logger LOG
            = Logger.getLogger(ClassUtil.class.getName());

    private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

    private ClassUtil() {
    }

    public static Class<? extends Object> loadClass(String packageName,
            String className) throws ClassNotFoundException {
        String fullName = packageName == null
                ? className : packageName + "." + className;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl.loadClass(fullName);
    }

    public static Field getInstanceField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            Field f;
            try {
                f = c.getDeclaredField(name);
            } catch (NoSuchFieldException | SecurityException ex) {
                LOG.log(Level.SEVERE, null, ex);
                f = null;
            }
            if (f != null) {
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods) && !Modifier.isTransient(mods)) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    public static <T> T getConstant(Class<?> clazz, String name,
            Class<T> type) {
        try {
            Field field = clazz.getField(name);
            int mods = field.getModifiers();
            if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods)) {
                throw new JedoException("Field " + clazz.getName() + "." + name
                        + " is not a constant");
            }
            if (!type.isAssignableFrom(field.getType())) {
                throw new JedoException("Wrong constant type "
                        + clazz.getName() + "." + name);
            }
            @SuppressWarnings("unchecked")
            T result = (T)field.get(null);
            return result;
        } catch (NoSuchFieldException | SecurityException 
                | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }
 
    public static Method getPropGetter(Class<?> clazz, String name) {
        try {
            String cname = Character.toUpperCase(name.charAt(0))
                    + name.substring(1);
            Method method = clazz.getMethod("get" + cname, EMPTY_CLASS_ARRAY);
            if (method != null && !Modifier.isStatic(method.getModifiers())) {
                return method;
            }
            method = clazz.getMethod("is" + cname, EMPTY_CLASS_ARRAY);
            if (method != null && !Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == boolean.class) {
                return method;
            }
            return null;
        } catch (NoSuchMethodException | SecurityException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
