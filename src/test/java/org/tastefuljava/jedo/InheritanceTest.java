package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;
import org.tastefuljava.jedo.testdb.Named;
import org.tastefuljava.jedo.testdb.Picture;

public class InheritanceTest extends JedoTestBase {    
    @Before
    public void setUp() throws IOException, SQLException, ClassNotFoundException {
        super.initialize();
    }
    
    @After
    public void tearDown() throws SQLException, IOException {
        super.terminate();
    }

    @Test
    public void testInheritance() {
        Folder folder =  getFolder("root");
        Picture pic = new Picture();
        pic.setName("thePic");
        pic.setFolder(folder);
        pic.setDescription("fr", "Ma magnifique image");
        pic.setDescription("en", "My beautiful picture");
        session.insert(pic);
        session.commit();
        Map<String,Named> table = new HashMap<>();
        for (Named obj: session.query(Named.class, "all")) {
            table.put(obj.getName(), obj);
        }
        assertTrue(table.get("root") instanceof Folder);
        assertTrue(table.get("sub1") instanceof Folder);
        assertTrue(table.get("sub2") instanceof Folder);
        assertTrue(table.get("thePic") instanceof Picture);
    }
}
