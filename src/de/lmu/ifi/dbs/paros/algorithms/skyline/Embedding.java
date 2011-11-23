package de.lmu.ifi.dbs.paros.algorithms.skyline;

import de.lmu.ifi.dbs.utilities.Arrays2;
import de.lmu.ifi.dbs.paros.graph.OSMGraph;
import de.lmu.ifi.dbs.paros.graph.OSMNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Embedding<N extends OSMNode> {

    private final Logger log = Logger.getLogger(Embedding.class.getName());
    private Map<N, NodeWrapper<N>> embedding = new HashMap<N, NodeWrapper<N>>();

    public Embedding() {
    }

    public Embedding(Map<N, NodeWrapper<N>> embedding) {
        this.embedding = embedding;
    }

    public Map<N, NodeWrapper<N>> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Map<N, NodeWrapper<N>> embedding) {
        if (embedding == null) {
            throw new NullPointerException("embedding must not be null");
        }
        this.embedding = embedding;
    }

    public synchronized void serializeTo(File file) throws IOException {
        if (embedding == null || embedding.size() == 0) {
            log.info("embedding null or empty");
            return;
        }
        long before = System.currentTimeMillis();
        log.fine("writing embedding to " + file);

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF8"), 1024 * 1024);
            bw.append("# nodeID,RefNodeId,weight1,weight2,weight3,weightX");
            bw.newLine();
            for (NodeWrapper<N> wrapper : embedding.values()) {
                int srcId = wrapper.getSource().getName();
                for (Map.Entry<N, float[]> entry : wrapper.getDistances().entrySet()) {
                    bw.append(srcId + "," + entry.getKey().getName() + "," + Arrays2.join(entry.getValue(), ","));
                    bw.newLine();
                }
            }
        } catch (IOException io) {
            throw io;
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException io) {
                throw io;
            }
        }
        long after = System.currentTimeMillis();
        log.info("serialized embedding (" + embedding.size() + " entries) in " + (after - before) + "ms");
    }

    public void deserializeFrom(OSMGraph<N, ?> g, File file) throws IOException {
        if (file == null || g == null) {
            throw new NullPointerException("file and graph must not be null.");
        }
        if (!file.exists() || !file.canRead()) {
            throw new IOException("file not there or not readable: " + file.getAbsolutePath());
        }

        log.info("reading embedding from file");
        long before = System.currentTimeMillis();
        embedding.clear();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"), 1 * 1024 * 1024);
            String line = null;
            int linecounter = 1;
            while ((line = br.readLine()) != null && !Thread.interrupted()) {
                linecounter++;
                if (line.startsWith("#") || line.length() <= 0) {
                    continue;
                }
                String[] parts = line.split(",");
                N src = g.getNode(Integer.parseInt(parts[0]));
                N ref = g.getNode(Integer.parseInt(parts[1]));
                assert src != null : "src must be != null in line " + linecounter + ": " + line;
                assert ref != null : "ref must be != null in line " + linecounter + ": " + line;
                assert parts.length >= 3 : "no weights in line " + linecounter + ": " + line;
                float[] weights = Arrays2.convertToFloat(Arrays.copyOfRange(parts, 2, parts.length));

                NodeWrapper wrapper = embedding.get(src);
                if (wrapper == null) {
                    wrapper = new NodeWrapper(src, weights.length);
                    embedding.put(src, wrapper);
                }
                wrapper.insert(ref, weights);
            }
        } catch (IOException io) {
            throw io;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException io) {
                throw io;
            }
        }
        long after = System.currentTimeMillis();
        log.info("deserialized embedding in " + (after - before) + "ms");
    }
}
