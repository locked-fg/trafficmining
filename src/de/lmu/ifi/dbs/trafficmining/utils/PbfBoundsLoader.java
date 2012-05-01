package de.lmu.ifi.dbs.trafficmining.utils;

import crosby.binary.osmosis.OsmosisReader;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private final Thread thread;
    private MapBounds mapBounds;

    public PbfBoundsLoader(File pbf) throws FileNotFoundException {
        
        OsmosisReader pbfReader = new OsmosisReader(new BufferedInputStream(new FileInputStream(pbf)));
        pbfReader.setSink(new OSMEntityWorker());

        thread = new Thread(pbfReader);
        thread.setDaemon(true);
    }

    public void loadAsync() {
        thread.start();
    }

    private void setBound(MapBounds bounds) {
        mapBounds = bounds;
        thread.interrupt();
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