package org.tastefuljava.jedo;

import org.tastefuljava.jedo.testdb.JedoTestBase;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
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
}
