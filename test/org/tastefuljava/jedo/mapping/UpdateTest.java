package org.tastefuljava.jedo.mapping;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.tastefuljava.jedo.Session;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.Picture;
import org.tastefuljava.jedo.util.Files;

public class UpdateTest {
    private static final File TESTDB_DIR
            = new File(System.getProperty("user.home"), "jedo-testdb");
    private static final File TESTDB = new File(TESTDB_DIR, "test");

    private Mapper mapper;
    private Connection cnt;
    private Session session;

    public UpdateTest() {
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
        MappingFileReader reader = new MappingFileReader();
        URL url = getClass().getResource("mapping.xml");
        reader.load(url);
        mapper = reader.getMapper();
        Files.deleteIfExists(TESTDB_DIR);
        Class.forName("org.h2.Driver");
        cnt = DriverManager.getConnection("jdbc:h2:" + TESTDB, "sa", "");
        runScript("initdb.sql");
        session = new Session(cnt, mapper);
    }

    @After
    public void tearDown() throws SQLException, IOException {
        if (session != null) {
            session.close();
        }
        if (cnt != null) {
            cnt.close();
        }
    }

    @Test
    public void testInit() {
        Assert.assertNotNull("Mapper is null", mapper);
        Assert.assertNotNull("JDBC connection is null", cnt);
        Assert.assertNotNull("Session is null", session);
    }

    @Test
    public void testRoot() {
        Folder root = session.queryOne(Folder.class, "rootFolder", "root");
        Assert.assertNotNull("Root is null", root);
        Assert.assertNull("Root is not a root", root.getParentId());
        Assert.assertTrue("Root is not a root", root.isRoot());
        Assert.assertEquals("Wrong folder name", "root", root.getName());
        Folder sub1 = getFolder("root/sub1");
        Assert.assertNotNull("Folder is null", sub1);
        Assert.assertEquals("Wrong parent id",
                (Integer)root.getId(), sub1.getParentId());
        Assert.assertEquals("Wrong name",
                "sub1", sub1.getName());
        Folder sub2 = getFolder("root/sub2");
        Assert.assertNotNull("Folder is null", sub2);
        Assert.assertEquals("Wrong parent id",
                (Integer)root.getId(), sub2.getParentId());
        Assert.assertEquals("Wrong name",
                "sub2", sub2.getName());
    }

    @Test
    public void testInsert() {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        
    }

    private Folder getFolder(String path) {
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

    private void runScript(String name) throws IOException, SQLException {
        try (InputStream stream = getClass().getResourceAsStream(name);
                Reader in = new InputStreamReader(stream, "UTF-8")) {
            RunScript.execute(cnt, in);
        }
    }
}
