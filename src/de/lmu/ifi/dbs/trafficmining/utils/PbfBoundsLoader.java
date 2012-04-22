package de.lmu.ifi.dbs.trafficmining.utils;

import crosby.binary.osmosis.OsmosisReader;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 *
 * @author Franz
 */
public class PbfBoundsLoader {

    public final static String EVT_BOUNDS = "BOUNDS";
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final FileInputStream inputStream;
    private final OsmosisReader pbfReader;
    private final Thread thread;
    private MapBounds mapBounds;

    public PbfBoundsLoader(File pbf) throws FileNotFoundException {
        inputStream = new FileInputStream(pbf);
        pbfReader = new OsmosisReader(new BufferedInputStream(inputStream));
        pbfReader.setSink(new OSMEntityWorker());
        thread = new Thread(pbfReader);
        thread.setDaemon(true);
    }

    public void loadAsync() {
        thread.start();
    }

    private void setBound(MapBounds bounds) {
        mapBounds = bounds;
//        try {
            thread.interrupt();
//            inputStream.close();
//        } catch (IOException | NullPointerException i) {
////             ignore this exception
//        }
        pcs.firePropertyChange(EVT_BOUNDS, null, mapBounds);
    }

    public MapBounds getMapBounds() {
        return mapBounds;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    class OSMEntityWorker implements SinkSource, EntityProcessor {

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            pcs.addPropertyChangeListener(listener);
        }

        @Override
        public void process(EntityContainer entityContainer) {
            entityContainer.process(this);
        }

        @Override
        public void process(BoundContainer boundContainer) {
            setBound(new MapBounds(boundContainer.getEntity()));
        }

        @Override
        public void process(NodeContainer container) {
        }

        @Override
        public void process(WayContainer container) {
        }

        @Override
        public void process(RelationContainer container) {
        }

        @Override
        public void complete() {
        }

        @Override
        public void release() {
        }

        @Override
        public void setSink(Sink sink) {
        }
    }
}