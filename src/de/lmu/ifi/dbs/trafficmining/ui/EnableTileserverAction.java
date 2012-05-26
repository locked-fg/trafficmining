/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.ui;

import de.lmu.ifi.dbs.trafficmining.TileServerFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action that updates the map Wrapper and informs the TileserverFactoy about a
 * request to change the tile server.
 *
 * @author Franz
 */
public class EnableTileserverAction implements ActionListener {

    /**
     * alias of the tile server
     */
    private final String key;
    /**
     * map wrapper that should be configured to use the new tile server
     */
    private final MapWrapper mapWrapper;

    /**
     * Constructs the action with the given tile server key and the mapWrapper
     * that should be reconfigured
     *
     * @param key tile server alias
     * @param mapWrapper
     * @see TileServerFactory
     */
    public EnableTileserverAction(String key, MapWrapper mapWrapper) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        if (mapWrapper == null) {
            throw new NullPointerException("mapWrapper must not be nill");
        }

        this.key = key;
        this.mapWrapper = mapWrapper;
    }

    /**
     * The event that causes the update of the map wrapper and tileserver
     * factory.
     *
     * @param e ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        mapWrapper.setTileServer(key);
        mapWrapper.repaint();
        try {
            TileServerFactory.get().setDefaultServer(key);
        } catch (IOException ex) {
            Logger.getLogger(EnableTileserverAction.class.getName()).log(Level.INFO, "could not load tileserver factory", ex);
        }

    }
}