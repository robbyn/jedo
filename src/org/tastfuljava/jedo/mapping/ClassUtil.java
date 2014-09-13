package org.tastfuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassUtil {
    private static final Logger LOG
            = Logger.getLogger(ClassUtil.class.getName());
 
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
        Class<?> c = clazz;
        while (c != null) {
            Field f;
            try {
                f = c.getDeclaredField(name);
            } catch (NoSuchFieldException | SecurityException ex) {
                LOG.log(Level.SEVERE, null, ex);
                f = null;
            }
            if (f == null || Modifier.isStatic(f.getModifiers())) {
                c = c.getSuperclass();
            } else {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }
}
