package org.tastfuljava.jedo.mapping;

import java.io.File;
import java.net.URL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MappingFileReaderTest {

    public MappingFileReaderTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetMapper() throws Exception {
        MappingFileReader reader = new MappingFileReader();
        URL url = getClass().getResource("mapping.xml");
        reader.load(url);
        Mapper mapper = reader.getMapper();
        try (XMLWriter out = new XMLWriter(new File("mapping-out.xml"))) {
            mapper.writeTo(out);
        }
        Assert.assertNotNull(mapper);
    }
}
