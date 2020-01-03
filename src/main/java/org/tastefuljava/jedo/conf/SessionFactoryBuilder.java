package org.tastefuljava.jedo.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.tastefuljava.jedo.JedoException;
import org.tastefuljava.jedo.SessionFactory;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.mapping.MappingFileReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            ParserHandler handler = new ParserHandler();
            parser.parse(new InputSource(in), handler);
            return handler.builder;
        } catch (SAXException | ParserConfigurationException e) {
            LOG.log(Level.SEVERE, "Error reading project", e);
            throw new JedoException(e.getMessage(), e);
        }
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

    public SessionFactoryBuilder setProperty(String key, String value) {
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

    private static class ParserHandler extends DefaultHandler {
        private static final String DTD_SYSTEM_ID = "jedo-conf.dtd";
        private static final String DTD_PUBLIC_ID
                = "-//tastefuljava.org//Jedo Configuration File 1.0//EN";

        private final SessionFactoryBuilder builder = new SessionFactoryBuilder();
        private boolean inDataSource = false;
        private final Properties props = new Properties();

        private ParserHandler() {
            props.putAll(System.getProperties());
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            if (DTD_PUBLIC_ID.equals(publicId)
                    || DTD_SYSTEM_ID.equals(systemId)) {
                InputSource source = new InputSource(
                        getClass().getResourceAsStream("jedo-conf.dtd"));
                source.setPublicId(publicId);
                source.setSystemId(systemId);
                return source;
            }
            return super.resolveEntity(publicId, systemId);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new SAXException(e.getMessage());
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new SAXException(e.getMessage());
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attrs) throws SAXException {
            switch (qName) {
                case "data-source":
                    inDataSource = true;
                    break;
                case "property": {
                    String name = attrs.getValue("name");
                    String value = resolve(attrs.getValue("value"));
                    Properties p = inDataSource ? builder.props : props;
                    p.setProperty(name, value == null ? "" : value);
                    break;
                }
                case "mapping": {
                    String resource = resolve(attrs.getValue("resource"));
                    String file = resolve(attrs.getValue("file"));
                    String url = resolve(attrs.getValue("url"));
                    if (resource != null) {
                        ClassLoader cl
                                = Thread.currentThread().getContextClassLoader();
                        builder.loadMapping(cl.getResource(resource));
                    }
                    if (file != null) {
                        builder.loadMapping(new File(file));
                    }
                    if (url != null) {
                        try {
                            builder.loadMapping(new URL(url));
                        } catch (MalformedURLException ex) {
                            throw new JedoException(ex.getMessage(), ex);
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            switch (qName) {
                case "data-source":
                    inDataSource = false;
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
        }

        private String resolve(String value) {
            if (value == null) {
                return null;
            }
            StringBuilder buf = new StringBuilder();
            int st = 0;
            int mark = -1;
            for (char c: value.toCharArray()) {
                switch (st) {
                    case 0:
                        switch (c) {
                            case '$':
                                st = 1;
                                break;
                            default:
                                buf.append(c);
                                break;
                        }
                        break;
                    case 1:
                        switch (c) {
                            case '{':
                                st = 2;
                                mark = buf.length();
                                break;
                            case '$':
                                buf.append('$');
                                break;
                            default:
                                st = 0;
                                buf.append('$');
                                buf.append(c);
                                break;
                        }
                        break;
                    case 2:
                        switch (c) {
                            case '}': {
                                st = 0;
                                String name = buf.substring(mark);
                                String repl = props.getProperty(name, "");
                                LOG.fine("Replacing property " + name
                                        + " with " + repl);
                                buf.setLength(mark);
                                buf.append(repl);
                                break;
                            }
                            default:
                                buf.append(c);
                                break;
                        }
                        break;
                }
            }
            return buf.toString();
        }
    }
}
