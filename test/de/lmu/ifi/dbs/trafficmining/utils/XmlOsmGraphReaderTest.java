package de.lmu.ifi.dbs.trafficmining.utils;

import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Link;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Franz
 */
public class XmlOsmGraphReaderTest {

    public XmlOsmGraphReaderTest() {
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
     * Test of process method, of class XmlOsmGraphReader.
     */
    @Test
    public void testProcess() throws Exception {
        XmlOsmGraphReader reader = new XmlOsmGraphReader(new File("../toelz.osm"), null);
        reader.process();
        Graph g = reader.getGraph();
        assertNotNull(g);
        
        Node a = g.getNode(313118433);
        Node b = g.getNode(313118453);
        
        List links = a.getLinksTo(b);
        assertFalse(links.isEmpty());
        
        Link link = (Link) links.get(0);
        assertEquals(37843966, link.getId());
        assertEquals(12, link.getNodes().size());
    }
}
