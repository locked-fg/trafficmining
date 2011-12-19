/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.algorithms;

import de.lmu.ifi.dbs.beansUI.EnumPropertyDescriptor;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wombat
 */
public class OSMDijkstraBeanInfo extends SimpleBeanInfo {

    private static final Logger log = Logger.getLogger(OSMDijkstraBeanInfo.class.getName());

    public OSMDijkstraBeanInfo() {
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
                        new EnumPropertyDescriptor("myAttribs", OSMDijkstra.class, OSMDijkstra.ATTRIBS.class)};

        } catch (IntrospectionException ex) {
            log.log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
