/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package de.lmu.ifi.dbs.trafficmining.utils;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 *
 * @author wombat
 */
public class OSMEntityWorker implements SinkSource, EntityProcessor {

    private static final Logger log = Logger.getLogger(OSMEntityWorker.class.getName());
    private Bound bounds = null;
    private Sink sink = null;
    private int n = 0;
    private int w = 0;
    private int r = 0;
    private long start;
    private String end = "";

    public OSMEntityWorker() {
        start = System.currentTimeMillis();
    }

    @Override
    public void process(EntityContainer entityContainer) {
        entityContainer.process(this);
    }

    @Override
    public void process(BoundContainer boundContainer) {
        bounds = boundContainer.getEntity();
        sink.process(boundContainer);
    }

    @Override
    public void process(NodeContainer container) {
        if (n>0 && n % 500000 == 0) {
            log.log(Level.FINE, "nodes: {0}", n);
            PBFOSMWorker.setStatus("processing nodes: "+NumberFormat.getInstance().format(n));
        }
        n++;
        sink.process(container);
    }

    @Override
    public void process(WayContainer container) {
        if (w>0 && w % 500000 == 0) {
            log.log(Level.FINE, "ways: {0}", w);
            PBFOSMWorker.setStatus("processing ways: "+NumberFormat.getInstance().format(w));
        }
        w++;
        sink.process(container);
    }

    @Override
    public void process(RelationContainer container) {
        if (r>0 && r % 10000 == 0) {
            log.log(Level.FINE, "relations: {0}", r);
            PBFOSMWorker.setStatus("processing relations: "+NumberFormat.getInstance().format(r));
        }
        r++;
        sink.process(container);
    }

    @Override
    public void complete() {
        log.log(Level.INFO, "{0}:complete() [Duration: {1} secs]", new Object[]{this.getClass().toString(), (System.currentTimeMillis() - start) / 1000});
//        PBFOSMWorker.setStatus(this.getClass().toString() + ":complete() [Duration: " + (System.currentTimeMillis() - start) / 1000 + " secs]");
        PBFOSMWorker.setStatus(end);
        sink.complete();
    }

    @Override
    public void release() {
                log.log(Level.INFO, "{0}:release() [Duration: {1} secs]", new Object[]{this.getClass().toString(), (System.currentTimeMillis() - start) / 1000});
//        PBFOSMWorker.setStatus(this.getClass().toString() + ":release() [Duration: " + (System.currentTimeMillis() - start) / 1000 + " secs]");
        sink.release();
    }

    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    public Bound getBounds() {
        return bounds;
    }
    
    public void setEndString(String s) {
        end = s;
    }
}
