package de.lmu.ifi.dbs.trafficmining.utils;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Franz
 */
public class NNDistanceTest {

    public NNDistanceTest() {
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

//    /**
//     * Test of length method, of class NNDistance.
//     */
//    @Test
//    public void testLength() {
//        System.out.println("length");
//        Link<? extends Node> link = null;
//        NNDistance instance = new NNDistance();
//        double expResult = 0.0;
//        double result = instance.length(link);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of distance method, of class NNDistance.
//     */
//    @Test
//    public void testDistance_Node_Node() {
//        System.out.println("distance");
//        Node a = null;
//        Node b = null;
//        NNDistance instance = new NNDistance();
//        double expResult = 0.0;
//        double result = instance.distance(a, b);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of distance method, of class NNDistance.
     */
    @Test
    public void testDistance_4args() {
        System.out.println("distance");
        NNDistance instance = new NNDistance();
        double latitudeA = 0.0;
        double longitudeA = 0.0;
        double latitudeB = 0.0;
        double longitudeB = 0.0;
        double expResult = 0.0;
        assertEquals(expResult, instance.distance(latitudeA, longitudeA, latitudeB, longitudeB), 0.0);

        latitudeA = -180;
        assertEquals(expResult, instance.distance(latitudeA, longitudeA, latitudeB, longitudeB), 0.0);

        latitudeA = 180;
        assertEquals(expResult, instance.distance(latitudeA, longitudeA, latitudeB, longitudeB), 0.0);

        latitudeA = 45;
        expResult = instance.distance(latitudeA, longitudeA, latitudeB, longitudeB);

        latitudeA -= 180;
        assertEquals(expResult, instance.distance(latitudeA, longitudeA, latitudeB, longitudeB), 0.0);
    }
}
