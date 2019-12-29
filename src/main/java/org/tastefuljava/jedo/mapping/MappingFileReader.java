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
import org.xml.sax.SAXParseException;
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
            factory.setValidating(true);
            factory.setNamespaceAware(true);
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
        private CollectionMapper.Builder collectionBuilder;
        private ListMapper.Builder listBuilder;
        private boolean inId;
        private ComponentMapper.Builder compBuilder;
        private Statement.Builder stmtBuilder;
        private String queryName;

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            if (DTD_PUBLIC_ID.equals(publicId)
                    || DTD_SYSTEM_ID.equals(systemId)) {
                InputSource source = new InputSource(
                        getClass().getResourceAsStream("jedo.dtd"));
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
                case "mapping":
                    packageName = attrs.getValue("package");
                    break;
                case "class":
                    String className = attrs.getValue("name");
                    classBuilder = builder.newClass(packageName, className);
                    break;
                case "id":
                    inId = true;
                    break;
                case "field": {
                        String name = attrs.getValue("name");
                        String column = attrs.getValue("column");
                        String type = attrs.getValue("type");
                        if (inId) {
                            classBuilder.addIdField(name, column);
                        } else if (compBuilder != null) {
                            compBuilder.addProp(name, column);
                        } else {
                            classBuilder.addField(name, column);
                        }
                    }
                    break;
                case "reference": {
                        String name = attrs.getValue("name");
                        String column = attrs.getValue("column");
                        String fetchMode = attrs.getValue("fetch-mode");
                        classBuilder.addReference(name, column.split("[,]"),
                                fetchMode);
                    }
                    break;
                case "set":
                    collectionBuilder = classBuilder.newSet(
                            attrs.getValue("name"),
                            attrs.getValue("fetch-mode"),
                            attrs.getValue("order"));
                    break;
                case "list":
                    listBuilder = classBuilder.newList(
                            attrs.getValue("name"),
                            attrs.getValue("fetch-mode"));
                    collectionBuilder = listBuilder;
                    break;
                case "component":
                    compBuilder = classBuilder.newComponent(
                            attrs.getValue("name"));
                    break;
                case "query":
                case "statement": {
                    String name = attrs.getValue("name");
                    String s = attrs.getValue("parameters");
                    String[] paramNames = s == null
                            ? EMPTY_STRING_ARRAY : s.split("[ ,]+");
                    boolean keys = "true".equals(attrs.getValue(
                                    "get-generated-keys"));
                    stmtBuilder = classBuilder.newStatement(paramNames, keys);
                    switch (qName) {
                        case "query":
                            classBuilder.addQuery(name, stmtBuilder);
                            break;
                        case "statement":
                            classBuilder.addStatement(name, stmtBuilder);
                            break;
                    }
                    break;
                }
                case "load":
                    stmtBuilder = classBuilder.newLoadStatement();
                    break;
                case "insert":
                    stmtBuilder = classBuilder.newStatement(
                            "true".equals(attrs.getValue(
                                    "get-generated-keys")));
                    break;
                case "update":
                case "delete":
                    stmtBuilder = classBuilder.newStatement(null);
                    break;
                case "fetch":
                    stmtBuilder = collectionBuilder.newFetchStatement(
                            attrs.getValue("parent"));
                    break;
                case "clear":
                    stmtBuilder = collectionBuilder.newClearStatement(
                            attrs.getValue("parent"));
                    break;
                case "add": {
                    boolean keys = "true".equals(attrs.getValue(
                                    "get-generated-keys"));
                    stmtBuilder = collectionBuilder.newAddStatement(keys,
                            attrs.getValue("parent"), attrs.getValue("element"));
                    break;
                }
                case "remove":
                    stmtBuilder = collectionBuilder.newRemove(
                            attrs.getValue("parent"), attrs.getValue("element"));
                    break;
                case "set-at":
                    stmtBuilder = listBuilder.newSetAt(attrs.getValue("parent"),
                            attrs.getValue("element"), attrs.getValue("index"));
                    break;
                case "add-at": {
                    boolean keys = "true".equals(attrs.getValue(
                                    "get-generated-keys"));
                    stmtBuilder = listBuilder.newAddAtStatement(keys,
                            attrs.getValue("parent"), attrs.getValue("element"),
                            attrs.getValue("index"));
                    break;
                }
                case "remove-at":
                    stmtBuilder = listBuilder.newRemoveAt(
                            attrs.getValue("parent"),
                            attrs.getValue("index"));
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
                    classBuilder = null;
                    break;
                case "id":
                    inId = false;
                    break;
                case "component":
                    compBuilder = null;
                    break;
                case "list":
                    listBuilder = null; // no break
                case "set":
                    collectionBuilder = null;
                    break;
                case "query":
                    queryName = null;
                    stmtBuilder = null;
                    break;
                case "statement":
                    queryName = null;
                    stmtBuilder = null;
                    break;
                case "load":
                    classBuilder.setLoad(stmtBuilder);
                    stmtBuilder = null;
                    break;
                case "insert":
                    classBuilder.setInsert(stmtBuilder);
                    stmtBuilder = null;
                    break;
                case "update":
                    classBuilder.setUpdate(stmtBuilder);
                    stmtBuilder = null;
                    break;
                case "delete":
                    classBuilder.setDelete(stmtBuilder);
                    stmtBuilder = null;
                    break;
                case "fetch":
                    stmtBuilder = null;
                    break;
                case "clear":
                    stmtBuilder = null;
                    break;
                case "add":
                    stmtBuilder = null;
                    break;
                case "remove":
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
