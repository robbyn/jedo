package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;

public class CollectionTest extends JedoTestBase {
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
    public void testSubfolders() {
        Folder folder =  getFolder("root");
        assertNotNull(folder.getSubfolders(), "Subfolders collection is null");
        assertEquals(2, folder.getSubfolders().size(),
                "Wrong number of subfolders");
        for (Folder sub: folder.getSubfolders()) {
            assertSame(sub, getFolder("root/" + sub.getName()),
                    "Wrong folder instance");
        }
        Folder sub1 = getFolder("root/sub1");
        assertTrue(folder.getSubfolders().remove(sub1));
        assertTrue(folder.getSubfolders().size() == 1);
        Folder sub2 = getFolder("root/sub2");
        assertTrue(folder.getSubfolders().remove(sub2));
        assertTrue(folder.getSubfolders().isEmpty());
        assertSame(sub1, getFolder("sub1"), "Folder not rooted");
        assertSame(sub2, getFolder("sub2"), "Folder not rooted");
        assertTrue(sub1.getSubfolders().add(sub2));
        assertSame(sub2, getFolder("sub1/sub2"), "Folder not rooted");
        session.commit();
    }
}
