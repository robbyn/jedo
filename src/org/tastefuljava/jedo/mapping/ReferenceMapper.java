package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.cache.Cache;
import org.tastefuljava.jedo.cache.ObjectId;
import org.tastefuljava.jedo.util.XMLWriter;

public class ReferenceMapper extends FieldMapper {
    private static final Logger LOG
            = Logger.getLogger(ReferenceMapper.class.getName());

    private ClassMapper targetClass;
    private final String[] columns;

    public ReferenceMapper(Field field, String[] columns) {
        super(field);
        this.columns = columns;
    }

    @Override
    public Object fromResultSet(Connection cnt, Cache<Object,Object> cache, ResultSet rs) {
        try {
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; ++i) {
                values[i] = rs.getObject(columns[i]);
            }
            return targetClass.load(cnt, cache, values);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage());
        }
    }

    @Override
    void fixReferences(Map<Class<?>, ClassMapper> map) {
        targetClass = map.get(field.getType());
        if (targetClass == null) {
            throw new JedoException("Unresolved reference target class: "
                    + field.getType().getName());
        }
    }

    @Override
    public void writeTo(XMLWriter out) {
        out.startTag("reference");
        out.attribute("name", getFieldName());
        out.attribute("column", columnList());
        out.endTag();
    }

    private String columnList() {
        StringBuilder buf = new StringBuilder();
        if (columns.length > 0) {
            buf.append(columns[0]);
            for (int i = 1; i < columns.length; ++i) {
                buf.append(',');
                buf.append(columns[i]);
            }
        }
        return buf.toString();
    }
}
