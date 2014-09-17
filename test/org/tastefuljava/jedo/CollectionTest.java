package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;

public class CollectionTest extends JedoTestBase {

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
        Folder folder =  getFolder("root");
        assertNotNull("Subfolders collection is null", folder.getSubfolders());
        assertEquals("Wrong number of subfolders", 2,
                folder.getSubfolders().size());
        for (Folder sub: folder.getSubfolders()) {
            assertSame("Wrong folder instance",
                    sub, getFolder("root/" + sub.getName()));
        }
    }
}
