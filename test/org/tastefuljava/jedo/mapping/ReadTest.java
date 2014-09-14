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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.h2.tools.RunScript;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.tastefuljava.jedo.Session;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.util.Files;

public class ReadTest {
    private static final File TESTDB_DIR
            = new File(System.getProperty("user.home"), "jedo-testdb");
    private static final File TESTDB = new File(TESTDB_DIR, "test");

    private Mapper mapper;
    private Connection cnt;
    private Session session;

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
        Assert.assertEquals("Wrong folder name", "root", root.getName());
    }

    @Test
    public void testSubs() {
        Folder root = session.queryOne(Folder.class, "rootFolder", "root");
        List<Folder> subs = session.query(Folder.class, "subfolders", root);
        Assert.assertNotNull("Null list returned", subs);
        Assert.assertEquals("Wrong number of subfoders returned",
                2, subs.size());
        Map<Integer,Folder> idMap = new HashMap<>();
        idMap.put(root.getId(), root);
        Map<String,Folder> map = new HashMap<>();
        map.put(root.getName(), root);
        for (Folder sub: subs) {
            idMap.put(sub.getId(), sub);
            map.put(sub.getName(), sub);
        }
        Assert.assertEquals("IDs not unique", 3, idMap.size());
        Assert.assertEquals("Wrong number of names", 3, map.size());
        Assert.assertTrue("Bad names", map.keySet().containsAll(
                Arrays.asList("root", "sub1", "sub2")));
        for (Folder f: idMap.values()) {
            Assert.assertSame("Cache failure", f,
                    session.load(Folder.class, f.getId()));
        }
    }

    private void runScript(String name) throws IOException, SQLException {
        try (InputStream stream = getClass().getResourceAsStream(name);
                Reader in = new InputStreamReader(stream, "UTF-8")) {
            RunScript.execute(cnt, in);
        }
    }
}
