package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.utilities.PropertyContainer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author Franz
 */
public class TileServerFactory {

    private static final String LB_PLACEHOLDER = "@";
    private static final String SERVERS_KEY = "servers";
    private static final String PROPERTIES_FILE = "/tileserver.properties";
    private final HashMap<String, TileServer> tileServers = new HashMap<>();
    private String defaultServer;

    public TileServerFactory() {
    }

    public void load() throws IOException {
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

    public HashMap<String, TileServer> getTileServers() {
        return tileServers;
    }

    public TileServer getDefaultServer() {
        return tileServers.get(defaultServer);
    }
}
