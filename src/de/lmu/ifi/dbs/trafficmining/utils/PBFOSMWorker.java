package de.lmu.ifi.dbs.trafficmining.utils;

import crosby.binary.osmosis.OsmosisReader;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.openstreetmap.osmosis.areafilter.v0_6.BoundingBoxFilter;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;
import org.srtmplugin.osm.osmosis.SrtmPlugin_task;

/**
 *
 * @author greil
 */
public class PBFOSMWorker extends SwingWorker {

    private static final Logger log = Logger.getLogger(PBFOSMWorker.class.getName());
    private File in_pbf, out_osm, dir_srtm;
    private BufferedInputStream buff_inpStream;
    private BufferedWriter buff_outWriter;
    private OsmosisReader pbf_reader;
    private XmlWriter xml_writer;
    private SrtmPlugin_task srtm;
    private BoundingBoxFilter bounding_box;
    private OSMEntityWorker oec;
    public final static String EVT_STAT = "STATUS";
    // FIXME try to get rid of the static
    public static PBFOSMWorker active = null;

    public PBFOSMWorker(File in_pbf, File out_osm, File dir_srtm) {
        this.in_pbf = in_pbf;
        this.out_osm = out_osm;
        this.dir_srtm = dir_srtm;
        PBFOSMWorker.active = this;
    }

    public void init() {
        if (!in_pbf.isFile() || !in_pbf.canRead()) {
            throw new IllegalArgumentException(in_pbf + " does not exist or is not readable!");
        }
        if (out_osm.exists()) {
            if (!out_osm.isFile() || !out_osm.canWrite()) {
                throw new IllegalArgumentException(out_osm + " is not a file or not writeable!");
            }
        } else {
            try {
                if (!out_osm.createNewFile() || !out_osm.canWrite()) {
                    throw new IllegalArgumentException(out_osm + " can not be created or is not writeable!");
                }
            } catch (IOException ex) {
                Logger.getLogger(PBFOSMWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (dir_srtm != null) {
            if (!dir_srtm.isDirectory()) {
                throw new IllegalArgumentException(dir_srtm + " is not a directory!");
            }
        }
    }

    /**
     * Sets up the osmosis pipeline with all needed parameters
     *
     * @param box bounds of the selection
     * @param srtm_enabled use of srtm annotation
     */
    public void config(double[] box, boolean srtm_enabled) {
        try {
            buff_inpStream = new BufferedInputStream(new FileInputStream(in_pbf));
            buff_outWriter = new BufferedWriter(new FileWriter(out_osm), 15 * 1024 * 1024);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PBFOSMWorker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PBFOSMWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
        pbf_reader = new OsmosisReader(buff_inpStream);
        xml_writer = new XmlWriter(buff_outWriter);

        if (srtm_enabled) {
            srtm = new SrtmPlugin_task(
                    dir_srtm,
                    false,
                    false,
                    true);
        }
        oec = new OSMEntityWorker();
        if (box == null) {
            bounding_box = new BoundingBoxFilter(IdTrackerType.BitSet, 0, 0, 0, 0, true, true, true, true);
        } else {
            bounding_box = new BoundingBoxFilter(IdTrackerType.Dynamic, box[0], box[1], box[2], box[3], false, true, false, false);
        }
        pbf_reader.setSink(oec);
        oec.setSink(bounding_box);
        if (srtm_enabled) {
            oec.setEndString("processing SRTM: ...");
            bounding_box.setSink(srtm);
            srtm.setSink(xml_writer);
        } else {
            oec.setEndString("writing osm");
            bounding_box.setSink(xml_writer);
        }
    }

    /**
     * Returns the bounds of the selection at the JXMapKit
     *
     * @return bounds of selection
     */
    public double[] getBounds() {
        Bound b = oec.getBounds();
        if (b == null) {
            return null;
        }
        return new double[]{
                    b.getLeft(),
                    b.getRight(),
                    b.getTop(),
                    b.getBottom(),};
    }

    // FIXME try to get rid of the static
    public static void setStatus(String s) {
        active.firePropertyChange(EVT_STAT, "x", s);
    }

    @Override
    protected Object doInBackground() throws Exception {
        setStatus("processing nodes: ...");
        pbf_reader.run();
        return true;
    }

    @Override
    public void done() {
        try {
            buff_outWriter.flush();
            buff_outWriter.close();
            buff_inpStream.close();
            if (out_osm.length() == 0) {
                out_osm.delete();
            }
            setStatus("");
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * kills the SwingWorker due shutting down the complete osmosis pipeline
     */
    public void kill() {
        pbf_reader.setSink(null);
        oec.setSink(null);
        bounding_box.setSink(null);
        if (srtm != null) {
            srtm.setSink(null);
        }
        cancel(true);
    }
}
