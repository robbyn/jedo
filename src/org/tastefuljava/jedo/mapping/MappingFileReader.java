package org.tastefuljava.jedo.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.tastefuljava.jedo.JedoException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/validation", true);
            SAXParser parser = factory.newSAXParser();
            ParserHandler handler = new ParserHandler();
            parser.parse(new InputSource(in), handler);
        } catch (SAXException | ParserConfigurationException e) {
            LOG.log(Level.SEVERE, "Error reading project", e);
            throw new JedoException(e.getMessage());
        }
    }

    private class ParserHandler extends DefaultHandler {
        private static final String DTD_SYSTEM_ID = "jedo.dtd";
        private static final String DTD_PUBLIC_ID
                = "-//tastefuljava.org//Jedo Mapping File 1.0//EN";

        private String packageName;
        private ClassMapper.Builder classBuilder;
        private boolean inId;
        private ComponentMapper.Builder compBuilder;
        private Statement.Builder stmtBuilder;
        private String queryName;

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            if (DTD_PUBLIC_ID.equals(publicId)
                    || DTD_SYSTEM_ID.equals(systemId)) {
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
                        throw new JedoException(ex.getMessage());
                    }
                    break;
                case "id":
                    inId = true;
                    break;
                case "property":
                    String name = attrs.getValue("name");
                    String column = attrs.getValue("column");
                    if (inId) {
                        classBuilder.addIdProp(name, column);
                    } else if (compBuilder != null) {
                        compBuilder.addProp(name, column);
                    } else {
                        classBuilder.addProp(name, column);
                    }
                    break;
                case "component":
                    compBuilder = classBuilder.newComponent(
                            attrs.getValue("name"));
                    break;
                case "query":
                    queryName = attrs.getValue("name");
                    String s = attrs.getValue("parameters");
                    String[] paramNames = s == null
                            ? EMPTY_STRING_ARRAY : s.split("[ ,]+");
                    stmtBuilder = classBuilder.newStatement(paramNames);
                    break;
                case "load":
                    stmtBuilder = classBuilder.newLoadStatement();
                    break;
                case "insert":
                    stmtBuilder = classBuilder.newInsertStatement(
                            "true".equals(attrs.getValue(
                                    "get-generated-keys")));
                    break;
                case "update":
                case "delete":
                    stmtBuilder = classBuilder.newStatement(null);
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
                case "component":
                    classBuilder.addComponent(compBuilder.getMapper());
                    compBuilder = null;
                    break;
                case "query":
                    classBuilder.addQuery(
                            queryName, stmtBuilder.getStatement());
                    queryName = null;
                    stmtBuilder = null;
                    break;
                case "load":
                    classBuilder.setLoad(stmtBuilder.getStatement());
                    stmtBuilder = null;
                    break;
                case "insert":
                    classBuilder.setInsert(stmtBuilder.getStatement());
                    stmtBuilder = null;
                    break;
                case "update":
                    classBuilder.setUpdate(stmtBuilder.getStatement());
                    stmtBuilder = null;
                    break;
                case "delete":
                    classBuilder.setDelete(stmtBuilder.getStatement());
                    stmtBuilder = null;
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (stmtBuilder != null) {
                stmtBuilder.addChars(ch, start, length);
            }
        }
    }
}
