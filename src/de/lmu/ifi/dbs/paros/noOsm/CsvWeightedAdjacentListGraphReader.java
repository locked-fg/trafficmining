package de.lmu.ifi.dbs.paros.noOsm;

import de.lmu.ifi.dbs.paros.noOsm.WeightedAdjacentListGraph;
import de.lmu.ifi.dbs.paros.noOsm.WeightedLink;
import de.lmu.ifi.dbs.paros.noOsm.WeightedNode2D;
import de.lmu.ifi.dbs.paros.graph.OSMGraph;
import de.lmu.ifi.dbs.paros.graph.OSMLink;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import de.lmu.ifi.dbs.paros.utils.GraphFactory;
import de.lmu.ifi.dbs.utilities.Collections2;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Class for reading WeightedAdjacentListGraphs from CSV Files
 * @author graf
 * @Deprecated
 */
public class CsvWeightedAdjacentListGraphReader {

    public CsvWeightedAdjacentListGraphReader() {
    }

    private WeightedAdjacentListGraph loadGraph(File nodeFileName, File edgeFileName, int numWeights)
            throws IOException {
        WeightedAdjacentListGraph<WeightedNode2D<WeightedLink>, WeightedLink> g = new WeightedAdjacentListGraph();
        {
            BufferedReader in = new BufferedReader(new FileReader(nodeFileName));
            String inputline;
            while ((inputline = in.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(inputline, " ");
                int name = Integer.parseInt(tokenizer.nextToken());
                float x = Float.parseFloat(tokenizer.nextToken());
                float y = Float.parseFloat(tokenizer.nextToken());
                g.addNode(name, x, y);
            }
            in.close();
        }

        {
            BufferedReader in = new BufferedReader(new FileReader(edgeFileName));
            String inputline;
            while ((inputline = in.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(inputline, " ");

                if (numWeights == 0) {
                    numWeights = tokenizer.countTokens() - 3;
                }
                float[] weights = new float[numWeights];

                int node1 = Integer.parseInt(tokenizer.nextToken());
                int node2 = Integer.parseInt(tokenizer.nextToken());
                String isOneWay = tokenizer.nextToken();


                for (int i = 0; i < numWeights; i++) {
                    try {
                        weights[i] = Float.parseFloat(tokenizer.nextToken());
                    } catch (NoSuchElementException e) {
                        throw new IllegalArgumentException("Geringere Anzahl an Kantenkosten als erwartet in Zeile \"" + inputline + "\"", e);
                    }
                }
                new WeightedLink(g.getNode(node1), g.getNode(node2), weights, isOneWay.equals("1") ? true : false);
            }
            in.close();
        }
        return g;
    }

    public WeightedAdjacentListGraph getGraph(File nodes, File links) throws IOException {
        return loadGraph(nodes, links, 1);
    }

    public WeightedAdjacentListGraph getGraph(File nodes, File links, int numberOfWeights) throws IOException {
        return loadGraph(nodes, links, numberOfWeights);
    }

    /**
     * Creates a weighted link graph from OSM Data where the weights are determined by the inverse speed.
     * max speed = 0 weight<br>
     * 
     * weight = maxSpeed - speedOfLink()
     * @param nodes
     * @param links
     * @return
     */
    public WeightedAdjacentListGraph getGraphFromOsmData(File nodes, File links) throws IOException {
        HashMap<String, Integer> gainMap = new OSMGraph().getSpeedMap();
        int maxSpeed = Collections2.maxValue(gainMap.values());

        OSMGraph<OSMNode, OSMLink> osmGraph = GraphFactory.readOsmGraphCsv(nodes, links);
        HashMap<OSMNode, WeightedNode2D> map = new HashMap<OSMNode, WeightedNode2D>(2048);

        WeightedAdjacentListGraph<WeightedNode2D<WeightedLink>, WeightedLink> g = new WeightedAdjacentListGraph<WeightedNode2D<WeightedLink>, WeightedLink>();

        // import nodes
        for (OSMNode node : osmGraph.getNodes()) {
            WeightedNode2D<WeightedLink> w2Node = g.addNode(node.getName(), (float) node.getLat(), (float) node.getLon());
            map.put(node, w2Node);
        }
        final float minValue = 0.00001f;
        final int kmToM = 1000;
        // import gain
        for (OSMLink<OSMNode> link : osmGraph.getLinkList()) {
            WeightedNode2D from2 = map.get(link.getSource());
            WeightedNode2D to2 = map.get(link.getTarget());

            float cost = Math.max((float) link.getDistance() * kmToM, minValue);
            int speed = Math.max(link.getSpeed(), 1);
            float gain = Math.max(maxSpeed - speed, minValue);
            new WeightedLink(from2, to2, new float[]{cost, gain}, link.isOneway());
        }

        return g;
    }
}
