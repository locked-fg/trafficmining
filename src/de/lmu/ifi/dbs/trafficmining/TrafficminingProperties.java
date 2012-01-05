package de.lmu.ifi.dbs.trafficmining;

import de.lmu.ifi.dbs.utilities.PropertyContainer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author graf
 */
public class TrafficminingProperties {

    public static final String SETTINGS_FILE = "config.properties";
    public static final String SETTINGS_DIR = "PAROS";
    public static final String TAG_WHITELIST_FILE = "whitelist-tags.properties";
    private final PropertyContainer pc;
    // settings
    public static final String autoload_graph = "autoload.graph";
    public static final String plugin_dir = "plugin.dir";
    public static final String lru_graph_dir = "lru.graph.dir";
    public static final String lru_graph_file = "lru.graph.file";
    public static final String map_last_zoom = "map.last.zoom";
    public static final String map_last_center_latitude = "map.last.center.latitude";
    public static final String map_last_center_longitude = "map.last.center.longitude";

    public TrafficminingProperties() throws IOException {
        File propFile = new File(SETTINGS_FILE);
        if (!propFile.exists()) {

            String home = System.getProperty("user.home");


            if (home != null) {
                File base = new File(home);
                String osName = System.getProperty("os.name").toLowerCase();
                // Win7
                if (osName.startsWith("win")) {
                    if (osName.contains("7")) {
                        base = new File(base, "AppData");
                    }
                    base = new File(base, SETTINGS_DIR);
                } else {
                    // Linux?
                    base = new File(base, "." + SETTINGS_DIR);
                }
                propFile = new File(base, SETTINGS_FILE);
            } else {
                propFile = new File(SETTINGS_FILE);
            }
        }
        Logger.getLogger(TrafficminingProperties.class.getName()).log(Level.FINE, "using config file: " + propFile.getAbsolutePath());
        this.pc = new PropertyContainer(propFile);
    }

// <editor-fold defaultstate="collapsed" desc="Delegate to PropertyContainer">
    public String toString() {
        return pc.toString();
    }

    public String toSplitString(String splitSeparator) {
        return pc.toSplitString(splitSeparator);
    }

    public String setString(String key, String value) {
        return pc.setString(key, value);
    }

    public Point setProperty(String key, Point p) {
        return pc.setProperty(key, p);
    }

    public Color setProperty(String key, Color c) {
        return pc.setProperty(key, c);
    }

    public Dimension setProperty(String key, Dimension d) {
        return pc.setProperty(key, d);
    }

    public List<String> setProperty(String key, List<String> tags) {
        return pc.setProperty(key, tags);
    }

    public List<String> setProperty(String key, String[] tags) {
        return pc.setProperty(key, tags);
    }

    public File setProperty(String key, File f) {
        return pc.setProperty(key, f);
    }

    public Boolean setProperty(String key, boolean b) {
        return pc.setProperty(key, b);
    }

    public Double setProperty(String key, double i) {
        return pc.setProperty(key, i);
    }

    public Integer setProperty(String key, int i) {
        return pc.setProperty(key, i);
    }

    public String setProperty(String key, String value) {
        return pc.setProperty(key, value);
    }

    public synchronized void save() {
        try {
            pc.save();
        } catch (IOException ex) {
            Logger.getLogger(TrafficminingProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removeProperty(String key) {
        pc.removeProperty(key);
    }

    public List<String> getStringArray(String key) {
        return pc.getStringArray(key);
    }

    public String getString(String key, String def) {
        return pc.getString(key, def);
    }

    public String getString(String key) {
        return pc.getString(key);
    }

    public String getProperty(String string, String string0) {
        return pc.getProperty(string, string0);
    }

    public String getProperty(String key) {
        return pc.getProperty(key);
    }

    public Point getPoint(String key) {
        return pc.getPoint(key);
    }

    public List<String> getKeys() {
        return pc.getKeys();
    }

    public Integer getInteger(String key, int def) {
        return pc.getInteger(key, def);
    }

    public Integer getInteger(String key) {
        return pc.getInteger(key);
    }

    public File getFile(File parent, String key) {
        return pc.getFile(parent, key);
    }

    public File getFile(String key, File def) {
        return pc.getFile(key, def);
    }

    public File getFile(String key) {
        return pc.getFile(key);
    }

    public Double getDouble(String key) {
        return pc.getDouble(key);
    }

    public Dimension getDimension(String key) {
        return pc.getDimension(key);
    }

    public Color getColor(String key) {
        return pc.getColor(key);
    }

    public Boolean getBoolean(String key, Boolean default_value) {
        return pc.getBoolean(key, default_value);
    }

    public Boolean getBoolean(String key) {
        return pc.getBoolean(key);
    }

    public void configureObject(Object myObject) throws IllegalArgumentException,
            IllegalAccessException {
        pc.configureObject(myObject);
    }// </editor-fold>
}
