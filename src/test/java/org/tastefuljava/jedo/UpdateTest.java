package org.tastefuljava.jedo;

import org.tastefuljava.jedo.testdb.JedoTestBase;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.tastefuljava.jedo.conversion.Conversion;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.GpsData;
import org.tastefuljava.jedo.testdb.Picture;

public class UpdateTest extends JedoTestBase {

    public UpdateTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp()
            throws IOException, ClassNotFoundException, SQLException {
        super.initialize();
    }

    @AfterEach
    public void tearDown() throws SQLException, IOException {
        super.terminate();
    }

    @Test
    public void testRoot() {
        Folder root = session.queryOne(Folder.class, "rootFolder", "root");
        assertNotNull(root);
        assertNull(root.getParent());
        assertTrue(root.isRoot());
        assertEquals("root", root.getName());
        Folder sub1 = getFolder("root/sub1");
        assertNotNull(sub1);
        assertSame(root, sub1.getParent());
        assertEquals("sub1", sub1.getName());
        Folder sub2 = getFolder("root/sub2");
        assertNotNull(sub2);
        assertSame(root, sub2.getParent());
        assertEquals("sub2", sub2.getName());
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
        assertTrue(pic.getId() != 0);
        int picId = pic.getId();
        assertSame(pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        Folder folder2 = getFolder("root/sub1");
        assertNotSame(pic, pic2);
        assertEquals("mybeautifulpic.jpg", pic2.getName());
        assertSame(folder2, pic2.getFolder());
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
        assertTrue(pic.getId() != 0);
        int picId = pic.getId();
        assertSame(pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        assertNotSame(pic, pic2);
        pic2.setName("anothername.jpg");
        LocalDateTime now = Conversion.convert(new Date(), LocalDateTime.class);
        pic2.setTimestamp(now);
        session.update(pic2);
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic3 = session.load(Picture.class, picId);
        assertNotSame(pic3, pic2);
        assertNotSame(pic3, pic);
        assertEquals("anothername.jpg", pic3.getName());
        assertEquals(now, pic3.getTimestamp());
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
        assertTrue(pic.getId() != 0);
        int picId = pic.getId();
        assertSame(pic, session.load(Picture.class, pic.getId()));
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        session.delete(pic2);
        session.commit(); // should clear the cache
        terminate();
        open();
        Picture pic3 = session.load(Picture.class, picId);
        assertNull(pic3);
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
        assertTrue(pic.getId() != 0);
        int picId = pic.getId();
        assertSame(pic, session.load(Picture.class, pic.getId()));
        // no commit
        terminate();
        open();
        Picture pic2 = session.load(Picture.class, picId);
        assertNull(pic2);
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
            assertTrue(pic.getId() != 0);
            int picId = pic.getId();
            assertSame(pic, session.load(Picture.class, pic.getId()));
            session.commit(); // should clear the cache
            terminate();
            open();
            Picture pic2 = session.load(Picture.class, picId);
            GpsData gps2 = pic2.getGpsData();
            assertNotNull(gps2);
            assertNull(gps2.getAltitude());
            assertEquals(gps.getLatitude(), gps2.getLatitude(), 0.0000001);
            assertEquals(gps.getLongitude(), gps2.getLongitude(), 0.0000001);
        } catch (Throwable ex) {
            Logger.getLogger(UpdateTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
