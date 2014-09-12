package org.tastfuljava.jedo.mapping;

import java.util.Map;

public class ClassMapper {
    private final Class<?> clazz;
    private final PropertyMapper[] id;
    private final Map<String,PropertyMapper> props;

    public ClassMapper(Class<?> clazz, Map<String,PropertyMapper> props,
            PropertyMapper[] id) {
        this.clazz = clazz;
        this.id = id;
        this.props = props;
    }
}
