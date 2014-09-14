package org.tastefuljava.jedo;

import org.tastefuljava.jedo.testdb.JedoTestBase;
import java.io.IOException;
import java.sql.SQLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.Picture;

public class UpdateTest extends JedoTestBase {

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
    public void testInsert()
            throws SQLException, ClassNotFoundException, IOException {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setFolderId(folder.getId());
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        session.insert(pic);
        Assert.assertTrue("Picture ID is zero", pic.getId() != 0);
        int picId = pic.getId();
        Assert.assertSame("Reread failed",
                pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        Assert.assertNotSame("Cache not cleared", pic, pic2);
        Assert.assertEquals("Wrong name", "mybeautifulpic.jpg", pic2.getName());
    }
}
