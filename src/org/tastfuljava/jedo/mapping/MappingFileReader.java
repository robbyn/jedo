package org.tastfuljava.jedo.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class MappingFileReader {
    private static final Logger LOG
            = Logger.getLogger(MappingFileReader.class.getName());

    private static final String[] EMPTY_STRING_ARRAY = {};

    private final Mapper.Builder builder = new Mapper.Builder();

    public Mapper getMapper() {
        return builder.getMapper();
    }

    public void load(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            load(in);
        }
    }

    public void load(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            load(in);
        }
    }

    public void load(InputStream in)
            throws IOException {
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            try {
                reader.setFeature(
                        "http://xml.org/sax/features/validation", true);
            } catch (SAXException e) {
            }

            ParserHandler handler = new ParserHandler();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(in));
        } catch (SAXException e) {
            LOG.log(Level.SEVERE, "Error reading project", e);
            throw new IOException(e.getMessage());
        }
    }

    private class ParserHandler extends DefaultHandler {
        private String packageName;
        private ClassMapper.Builder classBuilder;
        private boolean inId;
        private String queryName;
        private String[] paramNames;
        private StringBuilder buf;

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            if ("-//tastefuljava.org//Jedo Mapping File 1.0//EN".equals(publicId)
                    || "jedo.dtd".equals(systemId)) {
                return new InputSource(
                        getClass().getResourceAsStream("jedo.dtd"));
            }
            return null;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attrs) throws SAXException {
            switch (qName) {
                case "mapping":
                    packageName = attrs.getValue("package");
                    break;
                case "class":
                    try {
                        String className = attrs.getValue("name");
                        classBuilder = new ClassMapper.Builder(
                                packageName, className);
                    } catch (ClassNotFoundException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        throw new SAXException(ex.getMessage());
                    }
                    break;
                case "id":
                    inId = true;
                    break;
                case "property":
                    if (inId) {
                        classBuilder.addIdProp(attrs.getValue("name"),
                                attrs.getValue("column"));
                    } else {
                        classBuilder.addProp(attrs.getValue("name"),
                                attrs.getValue("column"));
                    }
                    break;
                case "query":
                    queryName = attrs.getValue("name");
                    String s = attrs.getValue("parameters");
                    paramNames = s == null
                            ? EMPTY_STRING_ARRAY : s.split("[ ,]+");
                case "load":
                case "insert":
                case "update":
                case "delete":
                    buf = new StringBuilder();
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            switch (qName) {
                case "mapping":
                    packageName = null;
                    break;
                case "class":
                    builder.addClassMapper(classBuilder.getMapper());
                    classBuilder = null;
                    break;
                case "id":
                    inId = false;
                    break;
                case "query":
                case "load":
                case "insert":
                case "update":
                case "delete":
                    classBuilder.addStatement(
                            qName, queryName, paramNames, buf.toString());
                    queryName = null;
                    paramNames = null;
                    buf = null;
                    break;
            }
        }
    }
}
