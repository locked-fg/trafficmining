/*
 * GeoPosition.java
 *
 * Created on March 31, 2006, 9:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

/**
 * An immutable coordinate in the real (geographic) world, 
 * composed of a latitude and a longitude.
 * @author rbair
 */
public class GeoPosition {
    private double latitude;
    private double longitude;
    
    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude. These are double values in decimal degrees, not
     * degrees, minutes, and seconds.  Use the other constructor for those.
     * @param latitude a latitude value in decmial degrees
     * @param longitude a longitude value in decimal degrees
     */
    public GeoPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    // must be an array of length two containing lat then long in that order.
    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude as an array of two doubles, with the
     * latitude first. These are double values in decimal degrees, not
     * degrees, minutes, and seconds.  Use the other constructor for those.
     * @param coords latitude and longitude as a double array of length two
     */
    public GeoPosition(double [] coords) {
        this.latitude = coords[0];
        this.longitude = coords[1];
    }
    
    /**
     * Creates a new instance of GeoPosition from the specified
     * latitude and longitude. 
     * Each are specified as degrees, minutes, and seconds; not
     * as decimal degrees. Use the other constructor for those.
     * @param latDegrees the degrees part of the current latitude
     * @param latMinutes the minutes part of the current latitude
     * @param latSeconds the seconds part of the current latitude
     * @param lonDegrees the degrees part of the current longitude
     * @param lonMinutes the minutes part of the current longitude
     * @param lonSeconds the seconds part of the current longitude
     */
    public GeoPosition(double latDegrees, double latMinutes, double latSeconds,
            double lonDegrees, double lonMinutes, double lonSeconds) {
        this(latDegrees + (latMinutes + latSeconds/60.0)/60.0,
             lonDegrees + (lonMinutes + lonSeconds/60.0)/60.0);
    }
    
    /**
     * Get the latitude as decimal degrees
     * @return the latitude as decimal degrees
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * Get the longitude as decimal degrees
     * @return the longitude as decimal degrees
     */
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * Returns true the specified GeoPosition and this GeoPosition represent
     * the exact same latitude and longitude coordinates.
     * @param obj a GeoPosition to compare this GeoPosition to
     * @return returns true if the specified GeoPosition is equal to this one
     */
    public boolean equals(Object obj) {
        if (obj instanceof GeoPosition) {
            GeoPosition coord = (GeoPosition)obj;
            return coord.latitude == latitude && coord.longitude == longitude;
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     * @return 
     */
    public String toString() {
        return "[" + latitude + ", " + longitude + "]";
    }
}