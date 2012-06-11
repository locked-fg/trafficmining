package de.lmu.ifi.dbs.trafficmining.utils;

import crosby.binary.osmosis.OsmosisReader;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.openstreetmap.osmosis.areafilter.v0_6.BoundingBoxFilter;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.task.v0_6.Source;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;
import org.srtmplugin.osm.osmosis.SrtmPlugin_task;

/**
 *
 * @author greil
 */
public class PbfOsmLoader extends SwingWorker {

    private static final Logger log = Logger.getLogger(PbfOsmLoader.class.getName());
    private final File pbfFile, osmFile, srtmDir;
    private final MapBounds bounds;
    private final boolean useSrtm;

    public PbfOsmLoader(File in_pbf, File out_osm, File dir_srtm, MapBounds bounds, boolean srtm) throws IOException {
        this.pbfFile = in_pbf;
        this.osmFile = out_osm;
        this.srtmDir = dir_srtm;

        if (!pbfFile.isFile() || !pbfFile.canRead()) {
            throw new IllegalArgumentException(pbfFile + " does not exist or is not readable!");
        }

        if (osmFile.exists()) {
            if (!osmFile.isFile() || !osmFile.canWrite()) {
                throw new IllegalArgumentException(osmFile + " is not a file or not writeable!");
            }
        } else {
            if (!osmFile.createNewFile() || !osmFile.canWrite()) {
                throw new IllegalArgumentException(osmFile + " can not be created or is not writeable!");
            }
        }

        if (srtmDir != null && !srtmDir.isDirectory()) {
            throw new IllegalArgumentException(srtmDir + " is not a directory!");
        }

        this.bounds = bounds;
        this.useSrtm = srtm;
    }

    @Override
    protected Void doInBackground() {
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(pbfFile));
                BufferedWriter output = new BufferedWriter(new FileWriter(osmFile))) {

            BoundingBoxFilter boundingBoxFilter = null;
            SrtmPlugin_task srtmTask = null;

            if (bounds != null) {
                boundingBoxFilter = new BoundingBoxFilter(
                        IdTrackerType.Dynamic,
                        bounds.left, bounds.right, bounds.top, bounds.bottom,
                        false, true, false, false);
            }

            if (useSrtm) {
                srtmTask = new SrtmPlugin_task(srtmDir, false, false, true);
            }

            OsmosisReader pbfReader = new OsmosisReader(input);
            Source source = pbfReader;
            if (boundingBoxFilter != null) {
                source.setSink(boundingBoxFilter);
                source = boundingBoxFilter;
            }
            if (srtmTask != null) {
                source.setSink(srtmTask);
                source = srtmTask;
            }
            source.setSink(new XmlWriter(output));

            pbfReader.run();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
