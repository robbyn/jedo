package org.tastefuljava.jedo;

import org.tastefuljava.jedo.testdb.JedoTestBase;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
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
        assertNotNull("Mapper is null", mapper);
        assertNotNull("JDBC connection is null", cnt);
        assertNotNull("Session is null", session);
    }

    @Test
    public void testRoot() {
        Folder root = session.queryOne(Folder.class, "rootFolder", "root");
        assertNotNull("Root is null", root);
        assertNull("Root is not a root", root.getParent());
        assertTrue("Root is not a root", root.isRoot());
        assertEquals("Wrong folder name", "root", root.getName());
        Folder sub1 = getFolder("root/sub1");
        assertNotNull("Folder is null", sub1);
        assertSame("Wrong parent",
                root, sub1.getParent());
        assertEquals("Wrong name",
                "sub1", sub1.getName());
        Folder sub2 = getFolder("root/sub2");
        assertNotNull("Folder is null", sub2);
        assertSame("Wrong parent id",
                root, sub2.getParent());
        assertEquals("Wrong name",
                "sub2", sub2.getName());
    }

    @Test
    public void testInsert()
            throws SQLException, ClassNotFoundException, IOException {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setFolder(folder);
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        session.insert(pic);
        assertTrue("Picture ID is zero", pic.getId() != 0);
        int picId = pic.getId();
        assertSame("Reread failed",
                pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        Folder folder2 = getFolder("root/sub1");
        assertNotSame("Cache not cleared", pic, pic2);
        assertEquals("Wrong name", "mybeautifulpic.jpg", pic2.getName());
        assertSame("Different folder loaded", folder2, pic2.getFolder());
    }

    @Test
    public void testUpdate()
            throws SQLException, ClassNotFoundException, IOException {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setFolder(folder);
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        session.insert(pic);
        assertTrue("Picture ID is zero", pic.getId() != 0);
        int picId = pic.getId();
        assertSame("Reread failed",
                pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        assertNotSame("Cache not cleared", pic, pic2);
        pic2.setName("anothername.jpg");
        Date now = new Date();
        pic2.setTimestamp(now);
        session.update(pic2);
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic3 = session.load(Picture.class, picId);
        assertNotSame("Cache not cleared", pic3, pic2);
        assertNotSame("Cache not cleared", pic3, pic);
        assertEquals("Wrong name", "anothername.jpg", pic3.getName());
        assertEquals("Wrong date", now, pic3.getTimestamp());
    }

    @Test
    public void testDelete()
            throws SQLException, ClassNotFoundException, IOException {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setFolder(folder);
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        session.insert(pic);
        assertTrue("Picture ID is zero", pic.getId() != 0);
        int picId = pic.getId();
        assertSame("Reread failed",
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
        assertNull("Cache not deleted", pic3);
    }

    @Test
    public void testRollback()
            throws SQLException, ClassNotFoundException, IOException {
        Folder folder = getFolder("root/sub1");
        Picture pic = new Picture();
        pic.setFolder(folder);
        pic.setName("mybeautifulpic.jpg");
        pic.setSize(1024, 768);
        session.insert(pic);
        assertTrue("Picture ID is zero", pic.getId() != 0);
        int picId = pic.getId();
        assertSame("Reread failed",
                pic, session.load(Picture.class, pic.getId()));
        // no commit
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        assertNull("Not rollbacked", pic2);
    }

    @Test
    public void testComponent()
            throws SQLException, ClassNotFoundException {
        try {
            Folder folder = getFolder("root/sub1");
            Picture pic = new Picture();
            pic.setFolder(folder);
            pic.setName("mybeautifulpic.jpg");
            pic.setSize(1024, 768);
            GpsData gps = new GpsData();
            gps.setLatitude(46);
            gps.setLongitude(7);
            pic.setGpsData(gps);
            session.insert(pic);
            assertTrue("Picture ID is zero", pic.getId() != 0);
            int picId = pic.getId();
            assertSame("Reread failed",
                    pic, session.load(Picture.class, pic.getId()));
            session.commit(); // should clear the cache
            terminate();
            open();
            Picture pic2 = session.load(Picture.class, picId);
            GpsData gps2 = pic2.getGpsData();
            assertNotNull("Component not present", gps2);
            assertNull("Unexpected value here", gps2.getAltitude());
            assertEquals("Different values",
                    gps.getLatitude(), gps2.getLatitude(), 0.0000001);
            assertEquals("Different values",
                    gps.getLongitude(), gps2.getLongitude(), 0.0000001);
        } catch (Throwable ex) {
            Logger.getLogger(UpdateTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
