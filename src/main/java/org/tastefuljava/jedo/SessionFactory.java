package org.tastefuljava.jedo;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.tastefuljava.jedo.mapping.Mapper;

public class SessionFactory {
    public static final Logger LOG
            = Logger.getLogger(SessionFactory.class.getName());

    private final DataSource ds;
    private final Mapper mapper;

    public SessionFactory(Mapper mapper, DataSource ds) {
        this.ds = ds;
        this.mapper = mapper;
    }

    public Session openSession() {
        try {
            return new Session(ds.getConnection(), mapper);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException("Cannot connect to database: "
                    + ex.getMessage());
        }
    }
}
