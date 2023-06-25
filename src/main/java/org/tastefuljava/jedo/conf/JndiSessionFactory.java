package org.tastefuljava.jedo.conf;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.Mapper;

public class JndiSessionFactory extends SessionFactoryImpl {
    private static final Logger LOG
            = Logger.getLogger(JndiSessionFactory.class.getName());

    private final String jndiName;

    JndiSessionFactory(Mapper mapper, String jndiName) {
        super(mapper);
        this.jndiName = jndiName;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        try {
            Context context = new InitialContext();
            DataSource ds = (DataSource)context.lookup(jndiName);
            return ds.getConnection();
        } catch (NamingException ex) {
            throw new JedoException(ex.getMessage());
        }
    }
}
