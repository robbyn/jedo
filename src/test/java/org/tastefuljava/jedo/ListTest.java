package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;
import org.tastefuljava.jedo.testdb.Picture;

public class ListTest extends JedoTestBase {
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
    public void testLists() {
        String[] tagNames = {"tag0","tag1","tag2","tag3","tag4","tag5"};
        List<String> tagNameList = new ArrayList<>(Arrays.asList(tagNames));
        Folder folder =  getFolder("root");
        Picture pic = new Picture();
        pic.setName("thePic");
        pic.setFolder(folder);
        for (String name: tagNames) {
            pic.addTag(name);
        }
        session.insert(pic);
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
    }
}
