package de.lmu.ifi.dbs.paros.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class PathTest {

    @Test
    public void getPathSegmentsTest() {
        Node a = new Node(1);
        Node b = new Node(2);
        Node c = new Node(3);
        Path p1 = new Path(a, b);
        Path p2 = new Path(p1, c);

        List l = p2.getPathSegments();
    }

    @Test
    public void testGetNodes1() {
        // test regular path
        List<Node> nodeList = new ArrayList<Node>();
        for (int i = 0; i < 5; i++) {
            nodeList.add(new Node(i));
        }
        // build path
        Path<Path, Node, Link> path = new Path<Path, Node, Link>(nodeList.get(0), nodeList.get(1), 1, null);
        // add remaining nodes
        for (int i = 2; i < nodeList.size(); i++) {
            path = new Path(path, nodeList.get(i));
        }
        assertEquals(nodeList, path.getNodes());
    }

    @Test
    public void testGetNodes2() {
        // test path with length 1
        Node start = new Node(0);
        Node end = new Node(1);
        Path<Path, Node, Link> path = new Path<Path, Node, Link>(start, end, 1, null);
        assertEquals(1, path.getLength());
        assertNull(path.getParent());
    }

//    @Test
//    public void testReverse() {
//        // test regular path
//        List<Node> nodeList = new ArrayList<Node>();
//        for (int i = 0; i < 5; i++) {
//            nodeList.add(new Node(i));
//        }
//        // build path
//        Path<Path, Node, Link> path1 = new Path<Path, Node, Link>(nodeList.get(0), nodeList.get(1), 1, null);
//        Path<Path, Node, Link> path2 = new Path<Path, Node, Link>(nodeList.get(4), nodeList.get(3), 1, null);
//        // add remaining nodes
//        for (int i = 2; i < nodeList.size(); i++) {
//            path1 = new Path(path1, nodeList.get(i));
//            path2 = new Path(path2, nodeList.get(4 - i));
//        }
//        path1 = path1.reverse();
//        assertEquals(path1.getNodes(), path2.getNodes());
//    }
    @Test
    public void testContains() {
        // 2 elements in path
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Path p = new Path(n1, n2);
        assertTrue("path doesn't contain start node", p.contains(n1));
        assertTrue("path doesn't contain end node", p.contains(n2));

        // 3 elements in path
        Node n3 = new Node(3);
        p = new Path(p, n3);
        assertTrue("path doesn't contain start node", p.contains(n1));
        assertTrue("path doesn't contain mid node", p.contains(n2));
        assertTrue("path doesn't contain end node", p.contains(n3));

        Node n4 = new Node(4);
        p = new Path(p, n4);
        assertTrue("path doesn't contain start node", p.contains(n1));
        assertTrue("path doesn't contain mid node", p.contains(n2));
        assertTrue("path doesn't contain end node", p.contains(n3));
        assertTrue("path doesn't contain end node", p.contains(n4));

        assertFalse("path doesn't contain end node", p.contains(new Node(-1)));
    }
}
