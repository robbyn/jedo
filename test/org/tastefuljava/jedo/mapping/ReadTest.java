/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tastefuljava.jedo.mapping;

import org.tastefuljava.jedo.mapping.Mapper;
import org.tastefuljava.jedo.mapping.MappingFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import org.h2.engine.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.tastefuljava.jedo.util.Files;

/**
 *
 * @author maurice
 */
public class ReadTest {
    private static final File TESTDB_DIR
            = new File(System.getProperty("user.home"), "jedo-testdb");

    private Mapper mapper;
    private Connection cnt;
    private Session sessio;

    public ReadTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException, ClassNotFoundException {
        MappingFileReader reader = new MappingFileReader();
        URL url = getClass().getResource("mapping.xml");
        reader.load(url);
        mapper = reader.getMapper();
        Files.deleteIfExists(TESTDB_DIR);
        Class.forName("org.h2.Driver");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRead() {
        Assert.assertTrue("Just a prototype", false);
    }
}
