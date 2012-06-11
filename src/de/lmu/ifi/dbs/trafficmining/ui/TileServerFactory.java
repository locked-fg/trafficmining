package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.utilities.PropertyContainer;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The TileServerFactory manages the configures tile servers and reads the
 * configuration from the properties file "/tileserver.properties".
 *
 * @author Franz
 */
public class TileServerFactory {

    /**
     * singleton instance
     */
    private static TileServerFactory instance;
    /**
     * Placeholder character used in the properties file. The character is being
     * replace with the ids of the mirror servers (if configured).
     */
    private static final String LB_PLACEHOLDER = "@";
    /**
     * property key that identifies the server property
     */
    private static final String SERVERS_KEY = "servers";
    /**
     * path to the properties file
     */
    private static final String PROPERTIES_FILE = "/tileserver.properties";
    /**
     * Map of tile servers including an alias name.
     */
    private final HashMap<String, TileServer> tileServers = new HashMap<>();
    /**
     * Defines the key of the server that was set in one of the components
     */
    private String defaultServer;

    /**
     * private constructor as the factory should be used as a singleton
     */
    private TileServerFactory() {
    }

    /**
     * Returns (and if needed, creates) a Factory.
     *
     * @return Factory instance
     * @throws IOException if the properties file could not be loaded
     */
    public static TileServerFactory get() throws IOException {
        if (instance == null) {
            instance = new TileServerFactory();
            instance.load();
        }
        return instance;
    }

    /**
     * load the configuration from the properties file
     *
     * @throws IOException
     */
    private void load() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream(PROPERTIES_FILE));
        String[] servers = properties.getProperty(SERVERS_KEY).split(",");
        String defaultKey = properties.getProperty("default");

        PropertyContainer pc = new PropertyContainer(properties);
        for (String aServer : servers) {
            String name = pc.getString(aServer + ".name");
            if (aServer.equals(defaultKey)) {
                defaultServer = name;
            }

            boolean osmUrlFormat = pc.getBoolean(aServer + ".osmUrlFormat");
            int minZoom = pc.getInteger(aServer + ".minZoomLevel");
            int maxZoom = pc.getInteger(aServer + ".maxZoomLevel");
            int totalZoom = pc.getInteger(aServer + ".totalMapZoom");
            int tilesize = pc.getInteger(aServer + ".tilesize");
            boolean xr21 = pc.getBoolean(aServer + ".xr21");
            boolean yt2b = pc.getBoolean(aServer + ".yt2b");
            String url = pc.getString(aServer + ".baseURL");
            String x = pc.getString(aServer + ".param.x");
            String y = pc.getString(aServer + ".param.y");
            String z = pc.getString(aServer + ".param.z");
            boolean caching = pc.getBoolean(aServer + ".caching");
            boolean enableLB = pc.getBoolean(aServer + ".loadbalancing.enable");
            String lburl = pc.getString(aServer + ".loadbalancing.url");
            String[] values = pc.getString(aServer + ".loadbalancing.values").split(";");

            TileServer ts = new TileServer(name, osmUrlFormat, minZoom, maxZoom, totalZoom, tilesize, xr21, yt2b, url, x, y, z);
            ts.setCaching(caching);
            ts.setLoadBalancing(enableLB, lburl, LB_PLACEHOLDER, values);
            ts.setUpTileFactory();

            if (ts.isValid()) {
                tileServers.put(name, ts);
            }
        }
    }

    /**
     * Obtains an unmodifiable map of tileservers.
     *
     * The map's keys are the aliases of the map servers.
     *
     * @return unmodifiable map of tileservers and their aliases
     */
    public Map<String, TileServer> getTileServers() {
        return Collections.unmodifiableMap(tileServers);
    }

    /**
     * Returns the tileserver that was configured as default.
     *
     * The default server can be changed at runtime by using the alias keys of
     * other tile servers.
     *
     * @return the currently active tile server
     */
    public TileServer getDefaultServer() {
        return tileServers.get(defaultServer);
    }

    /**
     * Changes the default server to the server identified by the alias key.
     *
     * @param key alias of the tile server that should be used as new default
     * server
     * @throws IllegalArgumentException if the key is not one of the server map.
     */
    public void setDefaultServer(String key) {
        if (!tileServers.containsKey(key)) {
            throw new IllegalArgumentException("tileserver not vaild: " + key);
        }
        defaultServer = key;
    }
}
