package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.utils.PluginLoader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;

/**
 * The class searches and identifies all plugins.
 *
 * After having found the plugins, the classes and aliases are loaded in a
 * DefaultComboBoxModel that is used in the main frame.
 *
 * @author Franz
 */
public class AlgorithmComboboxLoader {

    private final static Logger logger = Logger.getLogger(AlgorithmComboboxLoader.class.getName());
    private final DefaultComboBoxModel model;
    private final File pluginDir;

    /**
     * Constructs the loader eith the given model and the given pluginDir. The
     * directory is NOT scanned until load() is invoked.
     *
     * @param model
     * @param pluginDir
     */
    public AlgorithmComboboxLoader(DefaultComboBoxModel model, File pluginDir) {
        this.model = model;
        this.pluginDir = pluginDir;
    }

    /**
     * Perform the search for algorithms.
     */
    public void load() {
        try {
            logger.log(Level.FINE, "checking {0} for plugins", pluginDir.getCanonicalPath());
            model.removeAllElements();

            // some sanity checks and logging output.
            // this should help debugging if no algorithms are found
            if (pluginDir == null) {
                logger.log(Level.INFO, "plugin.dir is null, returning from call");
                return;
            }
            if (!pluginDir.exists() || !pluginDir.canRead()) {
                logger.log(Level.INFO, "plugin.dir set but does not exist or is not readable: {0}. Returning from call.",
                        pluginDir);
                return;
            }

            // now try to load the algorithms!
            PluginLoader<Algorithm> pluginLoader = new PluginLoader<>(pluginDir, Algorithm.class);
            List<Map.Entry<Class<Algorithm>, File>> map = pluginLoader.getMap();
            List<AlgorithmComboBoxElement> list = new ArrayList<>();
            for (Map.Entry<Class<Algorithm>, File> entry : map) {
                try {
                    // FIXME: check length of strings and use substrings if too long
                    // reason: too long string will break the GridBagLayout
                    list.add(new AlgorithmComboBoxElement(entry.getValue(), entry.getKey()));
                } catch (InstantiationException ex) {
                    logger.log(Level.SEVERE, "tried to instanciate an uninstanciable class", ex);
                }
            }

            // Possibly found some algorithms, now add them to the model
            Collections.sort(list);
            for (AlgorithmComboBoxElement elem : list) {
                model.addElement(elem);
            }
        } catch (IOException | IllegalAccessException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
