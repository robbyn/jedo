package org.tastefuljava.jedo.testdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.h2.tools.RunScript;
import org.tastefuljava.jedo.Session;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.mapping.MappingFileReader;
import org.tastefuljava.jedo.util.Files;

public abstract class JedoTestBase {
    protected static final File TESTDB_DIR
            = new File(System.getProperty("user.home"), "jedo-testdb");
    protected static final File TESTDB = new File(TESTDB_DIR, "test");

    protected Mapper mapper;
    protected Connection cnt;
    protected Session session;

    protected JedoTestBase() {
    }

    protected void initialize()
            throws IOException, ClassNotFoundException, SQLException {
        MappingFileReader reader = new MappingFileReader();
        URL url = getClass().getResource("mapping.xml");
        reader.load(url);
        mapper = reader.getMapper();
        Files.deleteIfExists(TESTDB_DIR);
        open();
        runScript("initdb.sql");
    }

    protected void open()
            throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.h2.Driver");
        cnt = DriverManager.getConnection("jdbc:h2:" + TESTDB, "sa", "");
        cnt.setAutoCommit(false);
        session = new Session(cnt, mapper);
    }

    protected void terminate() throws SQLException, IOException {
        if (session != null) {
            session.setCloseConnection(false);
            session.close();
        }
        if (cnt != null && !cnt.isClosed()) {
            cnt.createStatement().execute("SHUTDOWN");
            cnt.close();
        }
    }

    protected void runScript(String name) throws IOException, SQLException {
        try (InputStream stream = getClass().getResourceAsStream(name);
                Reader in = new InputStreamReader(stream, "UTF-8")) {
            RunScript.execute(cnt, in);
        }
    }

    protected Folder getFolder(String path) {
        String[] names = path.split("/");
        if (names.length == 0) {
            return null;
        } else {
            Folder folder = session.queryOne(
                    Folder.class, "rootFolder", names[0]);
            for (int i = 1; i < names.length; ++i) {
                folder = session.queryOne(
                        Folder.class, "subfolder", folder, names[i]);
            }
            return folder;
        }
    }
}
