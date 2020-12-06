package org.tastefuljava.jedo.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.tastefuljava.jedo.util.Util;

public class When {
    private static final Logger LOG = Logger.getLogger(When.class.getName());

    enum Condition {
        NULL {
            @Override
            boolean test(ResultSet rs, String column, String value)
                    throws SQLException {
                return rs.getObject(column) == null;
            }
        },
        NOT_NULL {
            @Override
            boolean test(ResultSet rs, String column, String value)
                    throws SQLException {
                return rs.getObject(column) != null;
            }
        },
        EQUAL {
            @Override
            boolean test(ResultSet rs, String column, String value)
                    throws SQLException {
                return value.equals(rs.getString(column));
            }
        },
        NOT_EQUAL {
            @Override
            boolean test(ResultSet rs, String column, String value)
                    throws SQLException {
                return !value.equals(rs.getString(column));
            }
        };

        abstract boolean test(ResultSet rs, String column, String value)
                throws SQLException;
    };

    private final Condition cond;
    private final String column;
    private final String value;
    private ClassMapper cm;

    private When(BuildContext context, Builder builder) {
        cond = builder.cond;
        column = builder.column;
        value = builder.value;
        context.addForward((mapper)->{
            When.this.cm = mapper.getClassMapper(builder.clazz);
        });
    }

    public ClassMapper ifTrue(ResultSet rs) throws SQLException {
        if (cond.test(rs, column, value)) {
            return cm;
        } else {
            return null;
        }
    }

    public static class Builder {
        private final Condition cond;
        private final String column;
        private final String value;
        private Class<?> clazz;

        public Builder(String condition, String column, String value) {
            this.cond = Util.toEnum(Condition.class, condition);
            this.column = column;
            this.value = value;
        }

        public void setClass(Class<?> clazz) {
            this.clazz = clazz;
        }

        public When build(BuildContext context) {
            return new When(context, this);
        }
    }
}
