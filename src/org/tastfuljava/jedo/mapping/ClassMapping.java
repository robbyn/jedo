package org.tastfuljava.jedo.mapping;

import java.sql.PreparedStatement;

class ClassMapping {
    private final PropertyMapping[] id;
    private final PropertyMapping[] props;

    ClassMapping(PropertyMapping[] id, PropertyMapping[] props) {
        this.id = id;
        this.props = props;
    }

    public void setParams(Object obj, PreparedStatement stmt) {
        
    }
}
