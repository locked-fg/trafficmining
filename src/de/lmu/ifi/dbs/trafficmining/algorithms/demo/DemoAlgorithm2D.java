package de.lmu.ifi.dbs.trafficmining.algorithms.demo;

import de.lmu.ifi.dbs.trafficmining.Statistics;
import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.result.Simplex2Result;
import java.util.List;

public class DemoAlgorithm2D extends Algorithm<Node, Graph, Path> {

    public ATTRIBS myAttribs = ATTRIBS.SPEED;

    public enum ATTRIBS {

        SPEED, HEIGHT
    }

    public ATTRIBS getMyAttribs() {
        return myAttribs;
    }

    public void setMyAttribs(ATTRIBS myAttribs) {
        this.myAttribs = myAttribs;
    }

    @Override
    public Statistics getStatistics() {
        Statistics stats = new Statistics();
        stats.put("Runtime", "60");
        stats.put("Nodes visited", "15000");
        stats.put("Paths in Queue", "100");
        return stats;
    }

    @Override
    public Simplex2Result getResult() {
        Simplex2Result s2result = new Simplex2Result();
        List<Node> nodes = getNodes();
        Path p = new Path(nodes.get(0), nodes.get(nodes.size() - 1));

        s2result.addResult(p, new double[]{150d, 10d});
        Node start = nodes.get(0);
        Node end = nodes.get(nodes.size() - 1);
        Node intermediate = new Node(-1);
        double lat = (start.getLat() + end.getLat()) / 2;
        double lon = (start.getLon() + end.getLon()) / 2;
        lat += (start.getLat() - end.getLat()) * 0.3;
        intermediate.setLat(lat);
        intermediate.setLon(lon);

        Path p2 = new Path(start, intermediate);
        p2 = new Path(p2, end);
        s2result.addResult(p2, new double[]{170d, 5d});

        s2result.setUnits(new String[]{"SPEED", "HEIGHT"});
        s2result.setAttributes(new String[]{"SPEED", "HEIGHT"});
        return s2result;
    }

    @Override
    public void run() {
        System.out.println("doing nothing");
    }

    @Override
    public String getName() {
        return "Demo algorithm 2D";
    }
}
