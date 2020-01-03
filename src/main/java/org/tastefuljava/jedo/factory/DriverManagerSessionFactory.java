package org.tastefuljava.jedo.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.mapping.Mapper;

public class DriverManagerSessionFactory extends SessionFactoryImpl {
    private static final Logger LOG
            = Logger.getLogger(DriverManagerSessionFactory.class.getName());

    private final Properties props = new Properties();

    DriverManagerSessionFactory(Mapper mapper, Properties props) {
        super(mapper);
        this.props.putAll(props);
    }

    @Override
    protected Connection getConnection() {
        try {
            Class.forName(props.getProperty("driver"));
            String url = props.getProperty("url");
            Connection cnt = DriverManager.getConnection(url, props);
            cnt.setAutoCommit(false);
            return cnt;
        } catch (ClassNotFoundException | SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage(), ex);
        }
    }
}
