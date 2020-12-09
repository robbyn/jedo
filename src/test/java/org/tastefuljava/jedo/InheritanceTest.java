package org.tastefuljava.jedo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.JedoTestBase;
import org.tastefuljava.jedo.testdb.Named;
import org.tastefuljava.jedo.testdb.Picture;

public class InheritanceTest extends JedoTestBase {    
    @BeforeEach
    public void setUp() throws IOException, SQLException, ClassNotFoundException {
        super.initialize();
    }
    
    @AfterEach
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
        session.commit();
        assertTrue(session.load(Named.class, table.get("root").getId()) instanceof Folder);
        assertTrue(session.load(Named.class, table.get("sub1").getId()) instanceof Folder);
        assertTrue(session.load(Named.class, table.get("sub2").getId()) instanceof Folder);
        assertTrue(session.load(Named.class, table.get("thePic").getId()) instanceof Picture);
        pic = session.load(Picture.class, table.get("thePic").getId());
        assertNotNull(pic);
        assertEquals("Ma magnifique image", pic.getDescription("fr"));
        assertEquals("My beautiful picture", pic.getDescription("en"));
    }
}
