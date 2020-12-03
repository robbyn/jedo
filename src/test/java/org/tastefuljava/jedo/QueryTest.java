package org.tastefuljava.jedo;

import java.io.IOException;
import java.net.URL;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.jedo.mapping.ClassMapper;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.mapping.MappingFileReader;
import org.tastefuljava.jedo.testdb.Folder;
import org.tastefuljava.jedo.testdb.Picture;

public class QueryTest {
    private Mapper mapper;
    
    public QueryTest() {
    }
    
    @Before
    public void setUp() throws IOException {
        MappingFileReader reader = new MappingFileReader();
        URL url = getClass().getResource("mapping.xml");
        reader.load(url);
        mapper = reader.getMapper();
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testFolderQuery() {
        testClassQuery(Folder.class,
                "SELECT R1.ID,R1.NAME,R0.PARENT_ID\n" +
                "FROM folders AS R0\n" +
                "JOIN named AS R1 ON R0.FID=R1.ID");
    }

    @Test
    public void testPictureQuery() {
        testClassQuery(Picture.class,
                "SELECT R1.ID,R1.NAME,R0.TIMESTAMP,R0.WIDTH,R0.HEIGHT,R3.ID,R3.NAME,R2.PARENT_ID\n" +
                "FROM pictures AS R0\n" +
                "JOIN named AS R1 ON R0.PID=R1.ID\n" +
                "LEFT OUTER JOIN folders AS R2 ON R0.FOLDER_ID=R2.FID\n" +
                "JOIN named AS R3 ON R2.FID=R3.ID");
    }

    private void testClassQuery(Class<?> clazz, String expected) {
        ClassMapper cm = mapper.getClassMapper(clazz);
        String qry = cm.newQuery().toString();
        System.out.println(qry);
        assertEquals(expected, qry);
    }
}
