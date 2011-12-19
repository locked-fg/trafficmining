/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.algorithms;

/**
 *
 * @author wombat
 */
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OSMSkylineBeanInfo extends SimpleBeanInfo {

    public OSMSkylineBeanInfo() {
        super();
    }

    @Override
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(getClass());
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[]{
                        new PropertyDescriptor("DISTANCE", OSMSkyline.class),
                        new PropertyDescriptor("TIME", OSMSkyline.class),
                        new PropertyDescriptor("HEIGHT", OSMSkyline.class),
                        new PropertyDescriptor("TRAFFIC_LIGHTS", OSMSkyline.class),
                        new PropertyDescriptor("_EmbeddingFilePath", OSMSkyline.class),
                        };
        } catch (IntrospectionException ex) {
            Logger.getLogger(OSMSkylineBeanInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
