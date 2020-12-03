package org.tastefuljava.jedo.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    public static class Builder {
        private final Map<Class<?>,ClassMapper.Builder> classMappers
                = new LinkedHashMap<>();
        private String[] packagePath = {"java.lang"};

        public Builder() {
        }

        public void setPackagePath(String packageNames) {
            List<String> path = new ArrayList<>();
            if (packageNames != null) {
                for (String name: packageNames.split("\\s*[,;:\\s]\\s*")) {
                    path.add(name);
                }
            }
            path.add("java.lang");
            packagePath = path.toArray(new String[path.size()]);
        }

        public Class<?> findClass(String className) {
            return Reflection.loadClass(className, packagePath);
        }

        public ClassMapper.Builder newClass(Class<?> clazz, String tableName) {
            ClassMapper.Builder builder = classMappers.get(clazz);
            if (builder == null) {
                builder = new ClassMapper.Builder(clazz, tableName);
                classMappers.put(builder.getType(), builder);
            }
            return builder;
        }

        public Mapper build() {
            BuildContext context = new BuildContext();
            Map<Class<?>,ClassMapper> map = new LinkedHashMap<>();
            for (ClassMapper.Builder cmb: classMappers.values()) {
                ClassMapper mapper = cmb.build(context);
                map.put(cmb.getType(), mapper);
            }
            for (ClassMapper cm: map.values()) {
                context.fixall(cm);
            }
            context.complete();
            return new Mapper(map);
        }
    }
}
