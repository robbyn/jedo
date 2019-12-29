package org.tastefuljava.jedo;

import java.io.File;
import java.net.URL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.mapping.MappingFileReader;
import org.tastefuljava.jedo.util.XMLWriter;

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
        Assert.assertNotNull(mapper);
    }
}
