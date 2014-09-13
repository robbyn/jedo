package org.tastfuljava.jedo.mapping;

import java.net.URL;
import org.junit.After;
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
    }
}
