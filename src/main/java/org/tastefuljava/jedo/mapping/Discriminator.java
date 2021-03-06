package org.tastefuljava.jedo.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Discriminator {
    private final When[] whens;
    private ClassMapper otherwise;

    private Discriminator(BuildContext context, Builder builder) {
        whens = builder.buildWhens(context);
        if (builder.otherwise != null) {
            context.addForward((mapper)->{
                Discriminator.this.otherwise
                        = mapper.getClassMapper(builder.otherwise);
            });
        }
    }

    public ClassMapper resolve(ResultSet rs)
            throws SQLException {
        for (When when: whens) {
            ClassMapper cm = when.ifTrue(rs);
            if (cm != null) {
                return cm;
            }
        }
        return otherwise;
    }

    public static class Builder {
        private List<When.Builder> whens = new ArrayList<>();
        private Class<?> otherwise;

        public Builder() {
        }

        public void addWhen(When.Builder when) {
            whens.add(when);
        }

        public void setOtherwise(Class<?> clazz) {
            this.otherwise = clazz;
        }

        private When[] buildWhens(BuildContext context) {
            When[] result = new When[whens.size()];
            int i = 0;
            for (When.Builder when: whens) {
                result[i++] = when.build(context);
            }
            return result;
        }

        public Discriminator build(BuildContext context) {
            return new Discriminator(context, this);
        }
    }
}
