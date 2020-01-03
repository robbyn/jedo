package org.tastefuljava.jedo.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.SessionFactory;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.mapping.MappingFileReader;

public class SessionFactoryBuilder {
    private static final Logger LOG
            = Logger.getLogger(SessionFactoryBuilder.class.getName());

    private final Properties props = new Properties();
    private final MappingFileReader mapperBuilder = new MappingFileReader();

    public static SessionFactoryBuilder loadFrom(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return loadFrom(in);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage(), ex);
        }
    }

    public static SessionFactoryBuilder loadFrom(URL url) {
        try (InputStream in = url.openStream()) {
            return loadFrom(in);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage(), ex);
        }
    }

    public static SessionFactoryBuilder loadFrom(String resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return loadFrom(cl.getResource(resource));
    }

    public static SessionFactoryBuilder loadFrom(InputStream in)
            throws IOException {
        Properties props = new Properties();
        props.load(in);
        return new SessionFactoryBuilder(props);
    }

    public SessionFactoryBuilder() {
    }

    public SessionFactoryBuilder(Properties props) {
        this.props.putAll(props);
    }

    public SessionFactory build() {
        Mapper mapper = mapperBuilder.getMapper();
        String jndiName = props.getProperty("jndi-name");
        if (jndiName != null) {
            return new JndiSessionFactory(mapper, jndiName);
        } else {
            return new DriverManagerSessionFactory(mapper, props);
        }
    }

    public SessionFactoryBuilder setProp(String key, String value) {
        props.put(key, value);
        return this;
    }

    public SessionFactoryBuilder loadMapping(URL url) {
        try {
            mapperBuilder.load(url);
            return this;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage(), ex);
        }
    }

    public SessionFactoryBuilder loadMapping(File file) {
        try {
            mapperBuilder.load(file);
            return this;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage(), ex);
        }
    }

    public SessionFactoryBuilder loadMapping(InputStream in) {
        try {
            mapperBuilder.load(in);
            return this;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new JedoException(ex.getMessage(), ex);
        }
    }
}
