package de.lmu.ifi.dbs.trafficmining.utils;

import crosby.binary.osmosis.OsmosisReader;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class PbfBoundsLoader {

    public final static String EVT_BOUNDS = "BOUNDS_LOADED";
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final File pbf;
    private MapBounds mapBounds;

    public PbfBoundsLoader(File pbf) {
        this.pbf = pbf;
    }

    public void loadAsync() throws FileNotFoundException {
        OsmosisReader pbfReader = new OsmosisReader(new BufferedFileInputStream(pbf));
        final Thread thread = new Thread(pbfReader);

        pbfReader.setSink(new EntitiyProcessorAdapter() {

            @Override
            public void process(BoundContainer boundContainer) {
                mapBounds = new MapBounds(boundContainer.getEntity());
                thread.interrupt();
                pcs.firePropertyChange(EVT_BOUNDS, null, mapBounds);
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    public MapBounds getMapBounds() {
        return mapBounds;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
}

/**
 * Adapter class to provide default implementations for sink and entity
 * processor
 *
 * @author Franz
 */
class EntitiyProcessorAdapter implements SinkSource, EntityProcessor {

    @Override
    public void process(EntityContainer ec) {
        ec.process(this);
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

    @Override
    public void process(BoundContainer bc) {
    }

    @Override
    public void process(NodeContainer nc) {
    }

    @Override
    public void process(WayContainer wc) {
    }

    @Override
    public void process(RelationContainer rc) {
    }
}