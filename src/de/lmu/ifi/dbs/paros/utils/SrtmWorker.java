package de.lmu.ifi.dbs.paros.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 *
 * @author Dominik Paluch
 * @modified Robert Greil
 */
public class SrtmWorker implements SinkSource, EntityProcessor {

    private static final Logger log = Logger.getLogger(SrtmWorker.class.getName());
    private Sink sink;
    private File SRTMbase = new File("./");
    private long start;
    private Map<File, SoftReference<BufferedInputStream>> srtmMap = new HashMap<File, SoftReference<BufferedInputStream>>();
    private Map<String, Integer> map_failed_srtm = new HashMap<String, Integer>();
    private List<String> srtm_server;
    private String latlon = "";
    private boolean doNot = false;
    private String status = "";

    public SrtmWorker(final File aDir, List<String> urls) {
        start = System.currentTimeMillis();
        if (!aDir.exists()) {
            if (!aDir.mkdirs()) {
                throw new IllegalArgumentException("Can not create directory " + aDir.getAbsolutePath());
            }
        }
        if (!aDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory " + aDir.getAbsolutePath());
        }
        SRTMbase = aDir;
        srtm_server = (ArrayList<String>) urls;
    }

    public SrtmWorker() {
    }

    @Override
    public void process(EntityContainer entityContainer) {
        entityContainer.process(this);
    }

    @Override
    public void process(BoundContainer boundContainer) {
        sink.process(boundContainer);
    }

    @Override
    public void process(NodeContainer container) {
        Node node = container.getEntity();
        double lat = node.getLatitude();
        double lon = node.getLongitude();
        Double srtmHeight = new Double(srtmHeight(lat, lon));

        Collection<Tag> tags = node.getTags();
        Tag pbf_tag = null;
        for (Tag tag : tags) {
            if (tag.getKey().equalsIgnoreCase("height")) {
                pbf_tag = tag;
                break;
            }
        }
        boolean addHeight = true;
        if (pbf_tag != null) {
            try {
                if (srtmHeight > 0) {
                    tags.remove(pbf_tag);
                } else {
                    Double h = Double.parseDouble(pbf_tag.getValue());
                    addHeight = false;
                }
            } catch (NumberFormatException nfe) {
                tags.remove(pbf_tag);
            }
        }
        if (addHeight) {
            tags.add(new Tag("height", srtmHeight.toString()));
            if (srtmHeight > 0) {
                if (!status.equals(latlon.toString())) {
                    status = latlon.toString();
                    PBFOSMWorker.setStatus("processing SRTM: " + status);
                    doNot = false;
                }
            } else if (srtmHeight <= 0 && !doNot) {
                if (!status.equals("...")) {
                    status = "...";
                    PBFOSMWorker.setStatus("processing SRTM: " + status);
                    doNot = true;
                }
            }
        }

        CommonEntityData ced = new CommonEntityData(
                node.getId(),
                node.getVersion(),
                node.getTimestamp(),
                node.getUser(),
                node.getChangesetId(),
                tags);

        sink.process(new NodeContainer(new Node(ced, lat, lon)));
    }

    @Override
    public void process(WayContainer container) {
        sink.process(container);
    }

    @Override
    public void process(RelationContainer container) {
        sink.process(container);
    }

    @Override
    public void complete() {
        log.log(Level.INFO, "{0}:complete() [Duration: {1} secs]", new Object[]{this.getClass().toString(), (System.currentTimeMillis() - start) / 1000});
        PBFOSMWorker.setStatus("writing osm");

        sink.complete();
    }

    @Override
    public void release() {
        log.log(Level.INFO, "{0}:release()", this.getClass().toString());
        sink.release();
    }

    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    private double srtmHeight(double lat, double lon) {
        //FIXME: cleanup!
        int nlat = Math.abs((int) Math.floor(lat));
        int nlon = Math.abs((int) Math.floor(lon));
        double val = 0;
        try {
            NumberFormat nf = NumberFormat.getInstance();
            String NS, WE;
            String f_nlat, f_nlon;

            if (lat > 0) {
                NS = "N";
            } else {
                NS = "S";
            }
            if (lon > 0) {
                WE = "E";
            } else {
                WE = "W";
            }

            nf.setMinimumIntegerDigits(2);
            f_nlat = nf.format(nlat);
            nf.setMinimumIntegerDigits(3);
            f_nlon = nf.format(nlon);

            File file = new File(NS + f_nlat + WE + f_nlon + ".hgt");
            latlon = NS + f_nlat + WE + f_nlon;

            String ID_file = file.getName();
            if (map_failed_srtm.containsKey(ID_file)) {
//                log.fine("STRM file " + ID_file + " already blacklisted, Returning height: 0.0");
                return 0;
            }
            double ilat = getILat(lat);
            double ilon = getILon(lon);
            int rowmin = (int) Math.floor(ilon);
            int colmin = (int) Math.floor(ilat);
            short[] values = new short[4];
            values[0] = getValues(file, rowmin, colmin);
            values[1] = getValues(file, rowmin + 1, colmin);
            values[2] = getValues(file, rowmin, colmin + 1);
            values[3] = getValues(file, rowmin + 1, colmin + 1);
            double coefrowmin = rowmin + 1 - ilon;
            double coefcolmin = colmin + 1 - ilat;
            double val1 = values[0] * coefrowmin + values[1] * (1 - coefrowmin);
            double val2 = values[2] * coefrowmin + values[3] * (1 - coefrowmin);
            val = val1 * coefcolmin + val2 * (1 - coefcolmin);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return val;
    }

    private static double getILat(double lat) {
        double dlat = lat - Math.floor(lat);
        double ilat = dlat * 1200;
        return ilat;
    }

    private static double getILon(double lon) {
        double dlon = lon - Math.floor(lon);
        double ilon = dlon * 1200;
        return ilon;
    }

    private short readShort(BufferedInputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    private short getValues(File file, int rowmin, int colmin) throws Exception {
        //FIXME: cleanup!
        
        file = new File(SRTMbase, file.getName());
        boolean ex1 = false;
        URL exUrl = new URL("http://127.0.0.1/");
        if (!file.exists()) {
            String ID_file = file.getName();
            if (map_failed_srtm.containsKey(ID_file)) {
//                log.fine("SRTM file " + ID_file + " already blacklisted, Returning height: 0.0");
                return 0;
            }
            log.log(Level.FINE, "Local SRTM file {0} not found. Trying to uncompress.", file.getName());
            File zipped = new File(SRTMbase, file.getName() + ".zip");
            if (!zipped.exists()) {
                log.log(Level.FINE, "Local zipped SRTM file {0}" + ".zip" + " not found. Trying to download from server.", file.getName());
                for (String url_srv : srtm_server) {
                    String url_rm_file = url_srv + file.getName() + ".zip";
                    exUrl = new URL(url_rm_file);
                    if (urlExist(exUrl)) {
                        ex1 = true;
                        break;
                    }
                }
                if (!ex1) {
                    log.log(Level.FINE, "Remote zipped SRTM file {0}.zip not found. Returning height 0.0", file.getName());
                    map_failed_srtm.put(file.getName(), 1);
                    return 0;
                }
            }

            ZipFile zipfile;
            File srtmzip = null;
            if (ex1) {
                InputStream inp = new BufferedInputStream(exUrl.openStream());
                srtmzip = File.createTempFile(file.getName(), ".zip", SRTMbase);
//                srtmzip = new File(SRTMbase, file.getName() + ".zip");
                BufferedOutputStream outp = new BufferedOutputStream(new FileOutputStream(srtmzip), 1024);
                copyInputStream(inp, outp);
                inp.close();
                outp.close();
                zipfile = new ZipFile(srtmzip, ZipFile.OPEN_READ);
            } else {
                zipfile = new ZipFile(zipped, ZipFile.OPEN_READ);
            }

            InputStream inp = zipfile.getInputStream(zipfile.getEntry(file.getName()));
            BufferedOutputStream outp = new BufferedOutputStream(new FileOutputStream(file), 1024);

            copyInputStream(inp, outp);
            outp.flush();
            outp.close();
            inp.close();

            if (srtmzip != null) {
                    srtmzip.deleteOnExit();
//                file.deleteOnExit();
            }
            log.log(Level.FINE, "Uncompressed zipped SRTM file {0} to {1}", new Object[]{zipped.getName(), file.getName()});

        }

        if (!file.exists()) {
            return 0;
        }

        SoftReference<BufferedInputStream> inRef = srtmMap.get(file);
        BufferedInputStream in = (inRef != null) ? inRef.get() : null;
        if (in == null) {
            int srtmbuffer = 4 * 1024 * 1024;
            in = new BufferedInputStream(new FileInputStream(file), srtmbuffer);
            srtmMap.put(file, new SoftReference<BufferedInputStream>(in));
            in.mark(srtmbuffer);
        }
        in.reset();

        long starti = ((1200 - colmin) * 2402) + rowmin * 2;
        in.skip(starti);
        short readShort = readShort(in);
        return readShort;
    }

    private static void copyInputStream(InputStream in, BufferedOutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        in.close();
        out.close();
    }

    private static boolean urlExist(URL urlN) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) urlN.openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
