package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;

public class InsertTest extends JedoTestBase {
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
        Folder folder = new Folder();
        folder.setName("newFolder");
        Folder sub1 = new Folder();
        sub1.setName("newSub1");
        session.insert(sub1);
        folder.getSubfolders().add(sub1);
        Folder sub2 = new Folder();
        sub2.setName("newSub2");
        session.insert(sub2);
        folder.getSubfolders().add(sub2);
        session.insert(folder);
        session.commit();
        folder = getFolder("newFolder");
        assertEquals(2, folder.getSubfolders().size());
        assertTrue(folder.getSubfolders().contains(getFolder("newFolder/newSub1")));
        assertTrue(folder.getSubfolders().contains(getFolder("newFolder/newSub2")));
    }
}
