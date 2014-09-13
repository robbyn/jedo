package org.tastfuljava.jedo.mapping;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class MappingFileReaderTest {
    
    public MappingFileReaderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getMapper method, of class MappingFileReader.
     */
    @Test
    public void testGetMapper() {
        System.out.println("getMapper");
        MappingFileReader instance = new MappingFileReader();
        Mapper expResult = null;
        Mapper result = instance.getMapper();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of load method, of class MappingFileReader.
     */
    @Test
    public void testLoad_File() throws Exception {
        System.out.println("load");
        File file = null;
        MappingFileReader instance = new MappingFileReader();
        instance.load(file);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of load method, of class MappingFileReader.
     */
    @Test
    public void testLoad_URL() throws Exception {
        System.out.println("load");
        URL url = null;
        MappingFileReader instance = new MappingFileReader();
        instance.load(url);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of load method, of class MappingFileReader.
     */
    @Test
    public void testLoad_InputStream() throws Exception {
        System.out.println("load");
        InputStream in = null;
        MappingFileReader instance = new MappingFileReader();
        instance.load(in);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
