package de.lmu.ifi.dbs.trafficmining.algorithms.demo;

import de.lmu.ifi.dbs.trafficmining.Statistics;
import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.graph.Graph;
import de.lmu.ifi.dbs.trafficmining.graph.Node;
import de.lmu.ifi.dbs.trafficmining.graph.Path;
import de.lmu.ifi.dbs.trafficmining.result.Simplex1Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DemoAlgorithm1D extends Algorithm<Node, Graph, Path> {

    private List<String> attribs = new ArrayList();
    final static List<String> attribsOptions = Collections.unmodifiableList(
            new ArrayList() {

                {
                    add("straight");
                    add("detour");
                }
            });

    public List<String> getAttribs() {
        return attribs;
    }

    public void setAttribs(List<String> attribs) {
        this.attribs = attribs;
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
    public Simplex1Result getResult() {
        Simplex1Result s1result = new Simplex1Result();
        List<Node> nodes = getNodes();
        Path p1 = new Path(nodes.get(0), nodes.get(nodes.size() - 1));
        s1result.addResult(p1, 150d);

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
        s1result.addResult(p2, 170d);


        s1result.setUnits("test");
        s1result.setAttributes("test");
        return s1result;
    }

    @Override
    public void run() {
        System.out.println("I'm just a demo");
    }

    @Override
    public String getName() {
        return "Demo algorithm 1D";
    }
}
