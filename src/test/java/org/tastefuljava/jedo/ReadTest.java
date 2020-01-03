package org.tastefuljava.jedo;

import org.tastefuljava.jedo.testdb.JedoTestBase;
import java.io.IOException;
import java.sql.SQLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.tastefuljava.jedo.testdb.Folder;

public class ReadTest extends JedoTestBase {

    public ReadTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp()
            throws IOException, ClassNotFoundException, SQLException {
        super.initialize();
    }

    @After
    public void tearDown() throws SQLException, IOException {
        super.terminate();
    }

    @Test
    public void testInit() {
        assertNotNull("Session is null", session);
    }

    @Test
    public void testRoot() {
        Folder root = session.queryOne(Folder.class, "rootFolder", "root");
        assertNotNull("Root is null", root);
        assertEquals("Wrong folder name", "root", root.getName());
    }
}
