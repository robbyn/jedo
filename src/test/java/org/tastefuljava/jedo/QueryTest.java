package org.tastefuljava.jedo;

import java.io.IOException;
import java.net.URL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
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
    public void testQuery() {
        testClassQuery(Folder.class);
        testClassQuery(Picture.class);
    }

    private void testClassQuery(Class<?> clazz) {
        ClassMapper cm = mapper.getClassMapper(clazz);
        String qry = cm.newQuery().toString();
        System.out.println(qry);
    }
}
