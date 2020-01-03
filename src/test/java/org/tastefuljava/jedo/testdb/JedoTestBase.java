package org.tastefuljava.jedo.testdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.h2.tools.RunScript;
import org.tastefuljava.jedo.Session;
import org.tastefuljava.jedo.SessionFactory;
import org.tastefuljava.jedo.conf.SessionFactoryBuilder;
import org.tastefuljava.jedo.conf.SessionImpl;
import org.tastefuljava.jedo.util.Files;

public abstract class JedoTestBase {
    protected static final File TESTDB_DIR
            = new File(System.getProperty("user.home"), "jedo-testdb");
    protected static final File TESTDB = new File(TESTDB_DIR, "test");
    protected static final SessionFactory factory
            = SessionFactoryBuilder.loadFrom(
                "org/tastefuljava/jedo/jedo-conf.xml").build();

    protected Session session;

    protected JedoTestBase() {
    }

    protected void initialize()
            throws IOException, SQLException, ClassNotFoundException {
        try {
            Files.deleteIfExists(TESTDB_DIR);
            open();
            runScript("initdb.sql");
        } catch (IOException | SQLException
                | ClassNotFoundException | RuntimeException ex) {
            Logger.getLogger(JedoTestBase.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    protected void open()
            throws ClassNotFoundException, SQLException, IOException {
        session = factory.openSession();
    }

    protected void terminate() throws SQLException, IOException {
        if (session != null) {
            session.close();
        }
    }

    protected void runScript(String name) throws IOException, SQLException {
        try (InputStream stream = getClass().getResourceAsStream(name);
                Reader in = new InputStreamReader(stream, "UTF-8")) {
            Connection cnt = ((SessionImpl)session).getConnection();
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
