/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package de.lmu.ifi.dbs.trafficmining;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

/**
 *
 * @author wombat
 */
public class TileServer {

    private static final Logger log = Logger.getLogger(TileServer.class.getName());
    private String name, baseURL, baseUrlWithSplitter, splitString, xparam, yparam, zparam;
    private File tmpDir;
    private String[] alternativeServers;
    private int minimumZoomLevel, maximumZoomLevel, totalMapZoom, tileSize;
    private boolean xr2l, yt2b;
    private boolean caching = false;
    private boolean loadBalancing = false;
    private boolean osmUrlFormat = false;
    private TileFactory tf = null;
    private int CACHESIZE = 256;
    private Random rnd;
    private Map<String, String> hm = new HashMap<String, String>(CACHESIZE);
    private boolean VERBOSE = false;
    private boolean broken = false;

    public TileServer(String name, boolean osmUrlFormat, int minimumZoomLevel, int maximumZoomLevel, int totalMapZoom, int tileSize, boolean xr2l, boolean yt2b, String baseURL, String xparam, String yparam, String zparam) {
        this.name = name;
        this.minimumZoomLevel = minimumZoomLevel;
        this.maximumZoomLevel = maximumZoomLevel;
        this.totalMapZoom = totalMapZoom;
        this.tileSize = tileSize;
        this.xr2l = xr2l;
        this.yt2b = yt2b;
        this.baseURL = baseURL;
        this.xparam = xparam;
        this.yparam = yparam;
        this.zparam = zparam;

        this.osmUrlFormat = osmUrlFormat;
    }

    public void setUpTileFactory() {
        tf = new DefaultTileFactory(new TileFactoryInfo(
                name,
                minimumZoomLevel,
                maximumZoomLevel,
                totalMapZoom,
                tileSize,
                xr2l,
                yt2b,
                baseURL,
                xparam,
                yparam,
                zparam) {

            @Override
            public String getTileUrl(int xtile, int ytile, int zoom) {
                String uri = "";
                if (osmUrlFormat) {
                    zoom = totalMapZoom - zoom;
                    String number = (zoom + "/" + xtile + "/" + ytile);

                    if (loadBalancing) {
                        String[] split = baseUrlWithSplitter.split(splitString);
                        uri = split[0] + alternativeServers[rnd.nextInt(alternativeServers.length)] + split[1];
                        uri = uri + number;
                        uri = uri + ".png";
                    } else {
                        uri = this.baseURL + number + ".png";
                    }


                    String v = hm.get(number);
                    if (VERBOSE) {
                        System.out.println("REQ uri: " + uri);
                    }
                    if (v != null) {
                        uri = v;
                        if (VERBOSE) {
                            System.out.println("RET uri (hm): " + uri);
                        }
                    } else if (v == null) {
                        hm.put(number, uri);
                        if (VERBOSE) {
                            System.out.println("RET uri (preload): " + uri);
                        }
                        if (caching) {
                            String number_format = number.replaceAll("/", "_") + ".png";
                            File f = new File(tmpDir, number_format);
                            if (f.exists() && f.canRead()) {
                                if (checkImage(f)) {
                                    uri = "file:" + f.getAbsolutePath().replaceAll("\\\\", "/");
                                    hm.put(number, uri);
                                    if (VERBOSE) {
                                        System.out.println("RET uri (exists): " + uri);
                                    }
                                    return uri;
                                } else {
                                    f.delete();
                                }
                            }

//                            if (hm.size() < CACHESIZE) {
                            final File sw_f = f;
                            final String sw_uri = uri;
                            final String sw_number = number;
                            SwingWorker sw = new SwingWorker() {

                                @Override
                                protected Object doInBackground() throws Exception {
                                    if (downloadRemoteToLocalTempfile(sw_uri, sw_f)) {
                                        String uri = "file:" + sw_f.getAbsolutePath().replaceAll("\\\\", "/");
                                        hm.put(sw_number, uri);
                                        if (VERBOSE) {
                                            System.out.println("RET uri (loaded): " + uri);
                                        }
                                        return true;
                                    } else {
                                        sw_f.delete();
                                        return false;
                                    }
                                }
                            };
                            sw.execute();
//                            }
                        }
                    }
                } else {
                    try {
                        throw new NoSuchMethodException("osmUrlFormat: " + osmUrlFormat + "\nnot yet implemented!");
                    } catch (NoSuchMethodException ex) {
                        Logger.getLogger(TileServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return uri;
            }
        });
        checkTileServer();
    }

    private boolean checkImage(File f) {
        try {
            Image i = ImageIO.read(f);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void checkTileServer() {
        String urlstring = tf.getTile(1, 1, 1).getURL();
        try {
            URL url = new URL(urlstring);
            Object content = url.getContent();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Tileserver \"{0}\" can not retrieve testuri: " + urlstring + "!"
                    + "\nSeems to be broken, therefore broken set to TRUE.", name);
            broken = true;
        }
    }

    public boolean downloadRemoteToLocalTempfile(String s, File f) {
        try {
            URL url = new URI(s).toURL();
            URLConnection urlConnection = url.openConnection();

            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

            byte[] buffer = new byte[1024 * 1024];
            int len = in.read(buffer);
            while (len >= 0) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
            out.flush();
            out.close();
            in.close();
            if (urlConnection.getContentLength() == f.length()) {
                return true;
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(TileServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(TileServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //Logger.getLogger(TileServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void setVERBOSE(boolean b) {
        VERBOSE = b;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * @return the xparam
     */
    public String getXparam() {
        return xparam;
    }

    /**
     * @return the yparam
     */
    public String getYparam() {
        return yparam;
    }

    /**
     * @return the zparam
     */
    public String getZparam() {
        return zparam;
    }

    /**
     * @return the minimumZoomLevel
     */
    public int getMinimumZoomLevel() {
        return minimumZoomLevel;
    }

    /**
     * @return the maximumZoomLevel
     */
    public int getMaximumZoomLevel() {
        return maximumZoomLevel;
    }

    /**
     * @return the totalMapZoom
     */
    public int getTotalMapZoom() {
        return totalMapZoom;
    }

    /**
     * @return the tileSize
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * @return the xr2l
     */
    public boolean isXr2l() {
        return xr2l;
    }

    /**
     * @return the yt2b
     */
    public boolean isYt2b() {
        return yt2b;
    }

    /**
     * @return the caching
     */
    public boolean isCaching() {
        return caching;
    }

    /**
     * @param caching the caching to set
     */
    public void setCaching(boolean caching) {
        this.caching = caching;
        if (caching) {
            String tempDir = System.getProperty("java.io.tmpdir");

            if (!(tempDir.endsWith("/") || tempDir.endsWith("\\"))) {
                tempDir += System.getProperty("file.separator") + name + System.getProperty("file.separator");
            } else {
                tempDir += name + System.getProperty("file.separator");
            }
            tmpDir = new File(tempDir);
            tmpDir.mkdirs();
        }
    }

    /**
     * @return the loadBalancing
     */
    public boolean isLoadBalancing() {
        return loadBalancing;
    }

    /**
     * @param loadBalancing the loadBalancing to set
     */
    public void setLoadBalancing(boolean loadBalancing, String baseUrlWithSplitter, String splitString, String[] alternativeServers) {
        this.loadBalancing = loadBalancing;
        this.baseUrlWithSplitter = baseUrlWithSplitter;
        this.splitString = splitString;
        this.alternativeServers = alternativeServers;
        rnd = new Random();
    }

    /**
     * @return the tfi
     */
    public TileFactory getTileFactory() {
        return tf;
    }

    /**
     * @return the CACHESIZE
     */
    public int getCACHESIZE() {
        return CACHESIZE;
    }

    /**
     * @param CACHESIZE the CACHESIZE to set
     */
    public void setCACHESIZE(int CACHESIZE) {
        this.CACHESIZE = CACHESIZE;
    }

    /**
     * @return the broken
     */
    public boolean isBroken() {
        return broken;
    }
}
