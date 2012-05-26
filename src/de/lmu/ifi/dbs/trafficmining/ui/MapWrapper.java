package de.lmu.ifi.dbs.trafficmining.ui;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JSlider;
import org.jdesktop.swingx.JXMapKit.DefaultProviders;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;

/**
 * Wrapper class around the map component.
 *
 * This class is intended to manage waypoint painting and stuff. The aim is to
 * get somewhat more independent from the singX implementation. Currently all
 * method calls are delegated to the map component. Yet they are all marked as
 * deprecated in orer to provide some aid in getting rif of these methods.
 *
 * @author Franz
 */
public class MapWrapper extends javax.swing.JPanel {

    /**
     * Creates new form MapWrapper
     */
    public MapWrapper() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mapKit = new org.jdesktop.swingx.JXMapKit();

        setLayout(new java.awt.BorderLayout());
        add(mapKit, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jdesktop.swingx.JXMapKit mapKit;
    // End of variables declaration//GEN-END:variables

    //<editor-fold defaultstate="collapsed" desc="delegates to mapKit instance">
    @Deprecated
    public void setZoom(int zoom) {
        mapKit.setZoom(zoom);
    }

    @Deprecated
    public Action getZoomOutAction() {
        return mapKit.getZoomOutAction();
    }

    @Deprecated
    public Action getZoomInAction() {
        return mapKit.getZoomInAction();
    }

    @Deprecated
    public boolean isMiniMapVisible() {
        return mapKit.isMiniMapVisible();
    }

    @Deprecated
    public void setMiniMapVisible(boolean miniMapVisible) {
        mapKit.setMiniMapVisible(miniMapVisible);
    }

    @Deprecated
    public boolean isZoomSliderVisible() {
        return mapKit.isZoomSliderVisible();
    }

    @Deprecated
    public void setZoomSliderVisible(boolean zoomSliderVisible) {
        mapKit.setZoomSliderVisible(zoomSliderVisible);
    }

    @Deprecated
    public boolean isZoomButtonsVisible() {
        return mapKit.isZoomButtonsVisible();
    }

    @Deprecated
    public void setZoomButtonsVisible(boolean zoomButtonsVisible) {
        mapKit.setZoomButtonsVisible(zoomButtonsVisible);
    }

    @Deprecated
    public void setTileFactory(TileFactory fact) {
        mapKit.setTileFactory(fact);
    }

    @Deprecated
    public void setCenterPosition(GeoPosition pos) {
        mapKit.setCenterPosition(pos);
    }

    @Deprecated
    public GeoPosition getCenterPosition() {
        return mapKit.getCenterPosition();
    }

    @Deprecated
    public GeoPosition getAddressLocation() {
        return mapKit.getAddressLocation();
    }

    @Deprecated
    public void setAddressLocation(GeoPosition pos) {
        mapKit.setAddressLocation(pos);
    }

    @Deprecated
    public JXMapViewer getMainMap() {
        return mapKit.getMainMap();
    }

    @Deprecated
    public JXMapViewer getMiniMap() {
        return mapKit.getMiniMap();
    }

    @Deprecated
    public JButton getZoomInButton() {
        return mapKit.getZoomInButton();
    }

    @Deprecated
    public JButton getZoomOutButton() {
        return mapKit.getZoomOutButton();
    }

    @Deprecated
    public JSlider getZoomSlider() {
        return mapKit.getZoomSlider();
    }

    @Deprecated
    public void setAddressLocationShown(boolean b) {
        mapKit.setAddressLocationShown(b);
    }

    @Deprecated
    public boolean isAddressLocationShown() {
        return mapKit.isAddressLocationShown();
    }

    @Deprecated
    public void setDataProviderCreditShown(boolean b) {
        mapKit.setDataProviderCreditShown(b);
    }

    @Deprecated
    public boolean isDataProviderCreditShown() {
        return mapKit.isDataProviderCreditShown();
    }

    @Deprecated
    public void setDefaultProvider(DefaultProviders prov) {
        mapKit.setDefaultProvider(prov);
    }

    @Deprecated
    public DefaultProviders getDefaultProvider() {
        return mapKit.getDefaultProvider();
    }
    //</editor-fold>
}
