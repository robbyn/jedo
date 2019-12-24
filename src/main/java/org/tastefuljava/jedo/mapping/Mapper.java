package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.util.XMLWriter;
import java.util.HashMap;
import java.util.Map;
import org.tastefuljava.jedo.util.Reflection;

public class Mapper {
    private final Map<Class<?>,ClassMapper> classMappers;

    private Mapper(Map<Class<?>,ClassMapper> classMappers) {
        this.classMappers = classMappers;
    }

    public ClassMapper getClassMapper(Class<?> clazz) {
        return classMappers.get(clazz);
    }

    public void writeTo(XMLWriter out) {
        out.startTag("mapping");
        for (ClassMapper cm: classMappers.values()) {
            cm.writeTo(out);
        }
        out.endTag();
    }

    public static class Builder {
        private final Map<Class<?>,ClassMapper.Builder> classMappers
                = new HashMap<>();

        public Builder() {
        }

        public ClassMapper.Builder newClass(String packageName,
                String className) {
            Class<?> clazz = Reflection.loadClass(packageName, className);
            return newClass(clazz);
        }

        private ClassMapper.Builder newClass(Class<?> clazz) {
            ClassMapper.Builder builder = classMappers.get(clazz);
            if (builder == null) {
                builder = new ClassMapper.Builder(clazz);
                classMappers.put(builder.getMappedClass(), builder);
            }
            return builder;
        }

        public Mapper getMapper() {
            Map<Class<?>,ClassMapper> map = new HashMap<>();
            for (ClassMapper.Builder cmb: classMappers.values()) {
                cmb.fixForwards(classMappers);
                ClassMapper mapper = cmb.build();
                map.put(cmb.getMappedClass(), mapper);
            }
            for (ClassMapper cm: map.values()) {
                cm.fixForwards(map);
            }
            return new Mapper(map);
        }
    }
}
