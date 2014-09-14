package org.tastefuljava.jedo;

import org.tastefuljava.jedo.testdb.JedoTestBase;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.GpsData;
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

    @Test
    public void testUpdate()
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
        pic2.setName("anothername.jpg");
        Date now = new Date();
        pic2.setTimestamp(now);
        session.update(pic2);
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic3 = session.load(Picture.class, picId);
        Assert.assertNotSame("Cache not cleared", pic3, pic2);
        Assert.assertNotSame("Cache not cleared", pic3, pic);
        Assert.assertEquals("Wrong name", "anothername.jpg", pic3.getName());
        Assert.assertEquals("Wrong date", now, pic3.getTimestamp());
    }

    @Test
    public void testDelete()
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
        session.delete(pic2);
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic3 = session.load(Picture.class, picId);
        Assert.assertNull("Cache not deleted", pic3);
    }

    @Test
    public void testRollback()
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
        // no commit
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        Assert.assertNull("Not rollbacked", pic2);
    }

    @Test
    public void testComponent()
            throws SQLException, ClassNotFoundException, IOException {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setFolderId(folder.getId());
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        GpsData gps = new GpsData();
        gps.setLatitude(46);
        gps.setLongitude(7);
        pic.setGpsData(gps);
        session.insert(pic);
        Assert.assertTrue("Picture ID is zero", pic.getId() != 0);
        int picId = pic.getId();
        Assert.assertSame("Reread failed",
                pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        GpsData gps2 = pic2.getGpsData();
        Assert.assertNotNull("Component not present", gps2);
        Assert.assertNull("Unexpected value here", gps2.getAltitude());
        Assert.assertEquals("Different values",
                gps.getLatitude(), gps2.getLatitude(), 0.0000001);
        Assert.assertEquals("Different values",
                gps.getLongitude(), gps2.getLongitude(), 0.0000001);
    }
}
