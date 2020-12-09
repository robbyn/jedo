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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class MappingFileReader {
    private static final Logger LOG
            = Logger.getLogger(MappingFileReader.class.getName());

    private static final String[] EMPTY_STRING_ARRAY = {};

    private final Mapper.Builder builder = new Mapper.Builder();

    public Mapper getMapper() {
        return builder.build();
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

        private Locator locator;
        private ClassMapper.Builder classBuilder;
        private CollectionMapper.Builder collectionBuilder;
        private ListMapper.Builder listBuilder;
        private MapMapper.Builder mapBuilder;
        private boolean inId;
        private ComponentMapper.Builder compBuilder;
        private Statement.Builder stmtBuilder;
        private Discriminator.Builder discBuilder;
        private When.Builder whenBuilder;
        private StringBuilder buf = new StringBuilder();

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
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        private String locationString() {
            if (locator == null) {
                return "";
            }
            StringBuilder buf = new StringBuilder();
            return "[systemId: " + locator.getSystemId()
                    + ", line: " + locator.getLineNumber()
                    + ", column: " + locator.getColumnNumber() + "] ";
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attrs) throws SAXException {
            try {
                switch (qName) {
                    case "mapping":
                        builder.setPackagePath(attrs.getValue("package"));
                        break;
                    case "class": {
                        String className = attrs.getValue("name");
                        String alias = attrs.getValue("alias");
                        String tableName = attrs.getValue("table");
                        classBuilder = builder.newClass(
                                builder.findClass(className), alias, tableName);
                        break;
                    }
                    case "inherits": {
                        String className = attrs.getValue("class");
                        classBuilder.setSuperClass(builder.findClass(className));
                        break;
                    }
                    case "discriminator":
                        discBuilder = new Discriminator.Builder();
                        classBuilder.setDiscriminator(discBuilder);
                        break;
                    case "when":
                        whenBuilder = new When.Builder(
                                attrs.getValue("condition"),
                                attrs.getValue("column"),
                                attrs.getValue("value"));
                        discBuilder.addWhen(whenBuilder);
                        buf.setLength(0);
                        break;
                    case "otherwise":
                        buf.setLength(0);
                        break;
                    case "id":
                        inId = true;
                        break;
                    case "field": {
                            String name = attrs.getValue("name");
                            String column = attrs.getValue("column");
                            String type = attrs.getValue("type");
                            boolean nullable = "true".equals(
                                    attrs.getValue("nullable"));
                            if (inId) {
                                classBuilder.addIdField(name, column);
                            } else if (compBuilder != null) {
                                compBuilder.addProp(name, column, nullable);
                            } else {
                                classBuilder.addField(name, column);
                            }
                        }
                        break;
                    case "reference": {
                            String name = attrs.getValue("name");
                            String column = attrs.getValue("column");
                            String fetchMode = attrs.getValue("fetch-mode");
                            boolean nullable = "true".equals(
                                    attrs.getValue("nullable"));
                            classBuilder.addReference(name, column.split("[,]"),
                                    fetchMode, nullable);
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
                    case "map":
                        mapBuilder = classBuilder.newMap(
                                attrs.getValue("name"),
                                attrs.getValue("fetch-mode"));
                        break;
                    case "key": {
                        String type = attrs.getValue("type");
                        Class<?> clazz = type == null
                                ? null : builder.findClass(type);
                        String column = attrs.getValue("column");
                        mapBuilder.setKeys(clazz, column);
                        break;
                    }
                    case "element": {
                        String type = attrs.getValue("type");
                        Class<?> clazz = type == null
                                ? null : builder.findClass(type);
                        String column = attrs.getValue("column");
                        if (collectionBuilder != null) {
                            collectionBuilder.setElements(clazz, column);
                        } else {
                            mapBuilder.setElements(clazz, column);
                        }
                        break;
                    }
                    case "component": {
                        boolean nullable = "true".equals(
                                attrs.getValue("nullable"));
                        compBuilder = classBuilder.newComponent(
                                attrs.getValue("name"), nullable);
                        break;
                    }
                    case "query":
                    case "statement": {
                        String name = attrs.getValue("name");
                        String s = attrs.getValue("parameters");
                        String[] paramNames = s == null
                                ? EMPTY_STRING_ARRAY : s.split("[ ,]+");
                        boolean keys = "true".equals(attrs.getValue(
                                        "get-generated-keys"));
                        stmtBuilder = classBuilder.newStatement(paramNames, keys);
                        buf.setLength(0);
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
                        buf.setLength(0);
                        break;
                    case "insert":
                        stmtBuilder = classBuilder.newInsertStatement(
                                "true".equals(attrs.getValue("get-generated-keys")));
                        buf.setLength(0);
                        break;
                    case "update":
                        stmtBuilder = classBuilder.newUpdateStatement();
                        buf.setLength(0);
                        break;
                    case "delete":
                        stmtBuilder = classBuilder.newDeleteStatement();
                        buf.setLength(0);
                        break;
                    case "fetch":
                        if (collectionBuilder != null) {
                            stmtBuilder = collectionBuilder.newFetchStatement(
                                    attrs.getValue("parent"));
                        } else {
                            stmtBuilder = mapBuilder.newFetchStatement(
                                    attrs.getValue("parent"));
                        }
                        buf.setLength(0);
                        break;
                    case "clear":
                        if (collectionBuilder != null) {
                            stmtBuilder = collectionBuilder.newClearStatement(
                                    attrs.getValue("parent"));
                        } else {
                            stmtBuilder = mapBuilder.newClearStatement(
                                    attrs.getValue("parent"));
                        }
                        buf.setLength(0);
                        break;
                    case "add":
                        stmtBuilder = collectionBuilder.newAddStatement(
                                attrs.getValue("parent"), attrs.getValue("element"));
                        buf.setLength(0);
                        break;
                    case "remove":
                        stmtBuilder = collectionBuilder.newRemove(
                                attrs.getValue("parent"), attrs.getValue("element"));
                        buf.setLength(0);
                        break;
                    case "set-at":
                        stmtBuilder = listBuilder.newSetAt(attrs.getValue("parent"),
                                attrs.getValue("element"), attrs.getValue("index"));
                        buf.setLength(0);
                        break;
                    case "add-at":
                        stmtBuilder = listBuilder.newAddAtStatement(
                                attrs.getValue("parent"), attrs.getValue("element"),
                                attrs.getValue("index"));
                        buf.setLength(0);
                        break;
                    case "remove-at":
                        stmtBuilder = listBuilder.newRemoveAt(
                                attrs.getValue("parent"),
                                attrs.getValue("index"));
                        buf.setLength(0);
                        break;
                    case "put":
                        stmtBuilder = mapBuilder.newPutStatement(
                                attrs.getValue("parent"), attrs.getValue("key"),
                                attrs.getValue("element"));
                        buf.setLength(0);
                        break;
                    case "remove-key":
                        stmtBuilder = mapBuilder.newRemoveKeyStatement(
                                attrs.getValue("parent"), attrs.getValue("key"));
                        buf.setLength(0);
                        break;
                }
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, locationString() + e.getMessage(), e);
                throw new SAXException(locationString() + e.getMessage());
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            switch (qName) {
                case "mapping":
                    break;
                case "class":
                    classBuilder = null;
                    break;
                case "discriminator":
                    discBuilder = null;
                    break;
                case "when":
                    whenBuilder.setClass(
                            builder.findClass(buf.toString().trim()));
                    break;
                case "otherwise":
                    discBuilder.setOtherwise(
                            builder.findClass(buf.toString().trim()));
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
                case "map":
                    mapBuilder = null;
                    break;
                case "query":
                case "statement":
                case "load":
                case "insert":
                case "update":
                case "delete":
                case "fetch":
                case "clear":
                case "add":
                case "remove":
                case "set-at":
                case "add-at":
                case "remove-at":
                case "put":
                case "remove-key":
                    stmtBuilder.setSql(buf.toString());
                    stmtBuilder = null;
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            buf.append(ch, start, length);
        }
    }
}
