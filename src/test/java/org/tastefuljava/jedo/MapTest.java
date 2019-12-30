package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;
import org.tastefuljava.jedo.testdb.Picture;

public class MapTest extends JedoTestBase {
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
    public void testMaps() {
        Folder folder =  getFolder("root");
        Picture pic = new Picture();
        pic.setName("thePic");
        pic.setFolder(folder);
        pic.setDescription("fr", "Ma magnifique image");
        pic.setDescription("en", "My beautiful picture");
        session.insert(pic);
        session.commit();
        pic = session.queryOne(Picture.class, "byName", folder, "thePic");
        assertNotNull(pic);
        Set<String> languages = pic.getDescriptionLanguages();
        assertTrue(languages.contains("fr"));
        assertTrue(languages.contains("en"));
        assertEquals("Ma magnifique image", pic.getDescription("fr"));
        assertEquals("My beautiful picture", pic.getDescription("en"));
        assertEquals(2, languages.size());
    }
}
