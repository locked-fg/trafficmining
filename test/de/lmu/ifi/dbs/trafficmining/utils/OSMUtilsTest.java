package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Franz
 */
public class OSMUtilsTest {

    public OSMUtilsTest() {
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
     * Test of getNearestNode method, of class OSMUtils.
     */
    @Test
    public void testGetNearestNode() {
        Node a = new Node(1);
        a.setLat(47.0);
        a.setLon(11.0);
        Node b = new Node(2);
        b.setLat(48.0);
        b.setLon(12.0);

        Graph<Node, Link> g = new Graph<>();
        g.addNode(a);
        g.addNode(b);

        GeoPosition pos = new GeoPosition(47, 11);
        Node expResult = a;
        Node result = OSMUtils.getNearestNode(pos, g);
        assertEquals(expResult, result);

        pos = new GeoPosition(47, 11-180);
        expResult = a;
        result = OSMUtils.getNearestNode(pos, g);
        assertEquals(expResult, result);
    }
//    /**
//     * Test of split method, of class OSMUtils.
//     */
//    @Test
//    public void testSplit() {
//        System.out.println("split");
//        Link<Node> splitLink = null;
//        Node splitNode = null;
//        GeoDistance distance = null;
//        List expResult = null;
//        List result = OSMUtils.split(splitLink, splitNode, distance);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getViewport method, of class OSMUtils.
//     */
//    @Test
//    public void testGetViewport() {
//        System.out.println("getViewport");
//        JXMapViewer map = null;
//        Rectangle2D expResult = null;
//        Rectangle2D result = OSMUtils.getViewport(map);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPathInfos method, of class OSMUtils.
//     */
//    @Test
//    public void testGetPathInfos() {
//        System.out.println("getPathInfos");
//        List<Node<Link>> nodes = null;
//        Map expResult = null;
//        Map result = OSMUtils.getPathInfos(nodes);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isConnectable method, of class OSMUtils.
//     */
//    @Test
//    public void testIsConnectable() {
//        System.out.println("isConnectable");
//        Path a = null;
//        Path b = null;
//        boolean expResult = false;
//        boolean result = OSMUtils.isConnectable(a, b);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setSpeed method, of class OSMUtils.
//     */
//    @Test
//    public void testSetSpeed() {
//        System.out.println("setSpeed");
//        Graph g = null;
//        Link l = null;
//        OSMUtils.setSpeed(g, l);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
