package de.lmu.ifi.dbs.paros.clustering;

import de.lmu.ifi.dbs.paros.graph.Path;
import de.lmu.ifi.dbs.paros.result.Result;
import de.lmu.ifi.dbs.utilities.PriorityQueue;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author skurtz
 */
public class SingleLinkClusteringWithPreprocessing implements
        ClusteringAlgorithm {

    private ArrayList<ExtendedClusterObject> seedlist = null;
    private Cluster result = null;
    private List<RouteDistance> distlist = new ArrayList<RouteDistance>();
    private RouteDistance activeDist = null;
    private PriorityQueue priorityqueue = null;
    private static final Logger log = Logger.getLogger(SingleLinkClusteringWithPreprocessing.class.getName());

    public SingleLinkClusteringWithPreprocessing() {
        seedlist = new ArrayList<ExtendedClusterObject>();
        result = null;
        distlist.add(new IterativeDistWithHashing());
        activeDist = distlist.get(0);
    }

    @Override
    public void setInput(Result input) {
        int counter = 0;
        Entry<Path, double[]> entry = null;
        double costs[];
        List<Double> costlist = null;

        if (!(input == null)) {
            Set<Entry<Path, double[]>> entries = input.getResults().entrySet();

            for (Iterator iterator1 = entries.iterator(); iterator1.hasNext();) {
                counter++;
                entry = (Entry<Path, double[]>) iterator1.next();
                costs = entry.getValue();
                costlist = new ArrayList<Double>();
                for (double d : costs) {
                    costlist.add(d);
                }
                // FIXME raises a class cast exception. Check if Path is REALLY needed here
                seedlist.add(new Route(counter, (Path) entry.getKey(), input.getUnits(), costlist));
            }
        } else {
            seedlist = null;
        }
    }

    @Override
    public void start() {
        ExtendedClusterObject cand1 = null;
        ExtendedClusterObject cand2 = null;
        int counter = 0;
        Point pair = new Point();

        if (seedlist.size() < 1) {
            result = null;
        } else {
            createPriorityQueue();
            System.out.print(priorityqueue.size() + ",");

            log.fine("Start Clustering...");
            while (seedlist.size() > 1) {
                cand1 = null;
                cand2 = null;
                while ((cand1 == cand2)) {
                    cand1 = null;
                    cand2 = null;
                    pair = (Point) priorityqueue.removeFirst();

                    for (int k = 0; k < seedlist.size(); k++) {
                        if (seedlist.get(k) instanceof Route) {
                            if (((Route) seedlist.get(k)).getId() == pair.getX() + 1 || ((Route) seedlist.get(k)).getId() == pair.getY() + 1) {
                                if (cand1 == null) {
                                    cand1 = seedlist.get(k);
                                } else {
                                    cand2 = seedlist.get(k);
                                }
                            }
                        } else {
                            if (((Cluster) seedlist.get(k)).contains(((int) pair.getX()) + 1)) {
                                if (cand1 == null) {
                                    cand1 = seedlist.get(k);
                                } else {
                                    cand2 = seedlist.get(k);
                                }
                            }
                            if (((Cluster) seedlist.get(k)).contains(((int) pair.getY()) + 1)) {
                                if (cand1 == null) {
                                    cand1 = seedlist.get(k);
                                } else {
                                    cand2 = seedlist.get(k);
                                }
                            }
                        }
                        if (cand2 != null) {
                            k = seedlist.size();
                        }

                    }
                }
                counter++;
                Cluster newCluster = new Cluster(counter);
                newCluster.add(cand1);
                newCluster.add(cand2);

                seedlist.remove(cand1);
                seedlist.remove(cand2);

                seedlist.add(newCluster);
            }
            if (seedlist.get(0) instanceof Cluster) {
                result = (Cluster) seedlist.get(0);
            } else {
                Cluster newCluster = new Cluster(1);
                newCluster.add(seedlist.get(0));
                result = newCluster;
            }
        }
        log.fine("Finished Clustering...");
        System.out.println(priorityqueue.size());
    }

    @Override
    public Cluster getResult() {
        return result;
    }

    private void createPriorityQueue() {
        log.fine("Creating PriorityQueue...");
        if (!(seedlist.isEmpty())) {
            int limit = seedlist.size() * (seedlist.size()) / 2;
            priorityqueue = new PriorityQueue<Point>(true, limit);
            for (int i = 0; i < seedlist.size(); i++) {
                for (int j = i + 1; j < seedlist.size(); j++) {
                    priorityqueue.addIfBetter(activeDist.getDist(((Route) seedlist.get(i)).getComplexPath(), ((Route) seedlist.get(j)).getComplexPath()), new Point(i, j), limit);
                }
            }
        }
    }

    @Override
    public void setDistance(RouteDistance rd) {
        activeDist = rd;
    }
}
