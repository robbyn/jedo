package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;

public class InsertTest extends JedoTestBase {
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
    }
}
