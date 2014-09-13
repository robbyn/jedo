package org.tastfuljava.jedo.mapping;

import java.util.HashMap;
import java.util.Map;

public class Mapper {
    private final Map<Class<?>,ClassMapper> classMappers;
    
    private Mapper(Builder builder) {
        this.classMappers = builder.classMappers;
    }

    public static class Builder {
        private final Map<Class<?>,ClassMapper> classMappers = new HashMap<>();

        public Builder() {
        }

        public void addClassMapper(ClassMapper cm) {
            classMappers.put(cm.getMappedClass(), cm);
        }

        public Mapper getMapper() {
            return new Mapper(this);
        }
    }
}
