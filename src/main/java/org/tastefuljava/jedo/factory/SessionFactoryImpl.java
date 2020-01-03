package org.tastefuljava.jedo.factory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import org.tastefuljava.jedo.SessionFactory;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.Mapper;

public abstract class SessionFactoryImpl implements SessionFactory {
    private static final Logger LOG
            = Logger.getLogger(SessionFactoryImpl.class.getName());

    private final Mapper mapper;

    protected abstract Connection getConnection();

    protected SessionFactoryImpl(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SessionImpl openSession() {
        Connection cnt = getConnection();
        return new SessionImpl(cnt, mapper);
    }
}
