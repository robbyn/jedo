package org.tastefuljava.jedo.mapping;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.util.Reflection;

public abstract class ValueMapper {
    private static final Logger LOG
            = Logger.getLogger(ValueMapper.class.getName());

    protected final Class<?> type;

    protected ValueMapper(Builder<?> builder) {
        this.type = builder.type;
    }

    public Class<?> getType() {
        return type;
    }

    public abstract Object fromResultSet(Storage pm, Object obj,
            ResultSet rs, ValueAccessor fm);

    void afterInsert(Storage pm, Object self, ValueAccessor fm) {
    }
    void beforeDelete(Storage pm, Object self, ValueAccessor fm) {
    }

    public static abstract class Builder<T extends ValueMapper> {
        protected final BuildContext context;
        protected final Class<?> type;
        private T creation;

        protected Builder(BuildContext context, Class<?> type) {
            this.context = context;
            this.type = type;
        }

        public Class<?> getType() {
            return type;
        }

        public final T build() {
            if (creation == null) {
                creation = create();
                initialize(creation);
            }
            return creation;
        }

        protected abstract T create();
        protected void initialize(T vm) {
        }

        // helper classes
        protected Field getField(String name) throws JedoException {
            Field field = Reflection.getInstanceField(type, name);
            if (field == null) {
                throw new JedoException("Field " + name + " not found in class "
                        + type.getName());
            }
            return field;
        }
    }
}
