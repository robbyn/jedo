package org.tastefuljava.jedo.conf;

import java.sql.Connection;
import java.sql.SQLException;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.SessionFactory;
import org.tastefuljava.jedo.mapping.Mapper;

public abstract class SessionFactoryImpl implements SessionFactory {
    private final Mapper mapper;

    protected abstract Connection getConnection() throws SQLException;

    protected SessionFactoryImpl(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SessionImpl openSession() {
        try {
            Connection cnt = getConnection();
            return new SessionImpl(cnt, mapper);
        } catch (SQLException ex) {
            throw new JedoException(ex.getMessage(), ex);
        }
    }
}
