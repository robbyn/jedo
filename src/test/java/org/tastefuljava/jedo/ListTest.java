package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;
import org.tastefuljava.jedo.testdb.Picture;

public class ListTest extends JedoTestBase {
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
    public void testLists() {
        String[] tagNames = {"tag0","tag1","tag2","tag3","tag4","tag5"};
        List<String> tagNameList = new ArrayList<>(Arrays.asList(tagNames));
        Folder folder =  getFolder("root");
        Picture pic = new Picture();
        pic.setName("thePic");
        pic.setFolder(folder);
        session.insert(pic);
        for (String name: tagNames) {
            pic.addTag(name);
        }
        session.commit();
        folder = getFolder("root");
        pic = session.queryOne(Picture.class, "byName", folder, "thePic");
        assertNotNull(pic);
        assertEquals(tagNameList.size(), pic.tagCount());
        for (int i = 0; i < tagNameList.size(); ++i) {
            assertEquals(tagNameList.get(i), pic.getTag(i));
        }
        tagNameList.remove(1);
        tagNameList.remove(1);
        pic.removeTag(1);
        pic.removeTag(1);
        assertEquals(tagNameList.size(), pic.tagCount());
        for (int i = 0; i < tagNameList.size(); ++i) {
            assertEquals(tagNameList.get(i), pic.getTag(i));
        }
        session.commit();
        folder = getFolder("root");
        pic = session.queryOne(Picture.class, "byName", folder, "thePic");
        assertNotNull(pic);
        assertEquals(tagNameList.size(), pic.tagCount());
    }
}
