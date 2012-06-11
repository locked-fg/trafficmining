package de.lmu.ifi.dbs.trafficmining.algorithms;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author greil
 */
public class AStarBeanInfo extends SimpleBeanInfo {

    private static final Logger log = Logger.getLogger(AStarBeanInfo.class.getName());

    public AStarBeanInfo() {
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
                        new PropertyDescriptor("weight", AStar.class)
                    };

        } catch (IntrospectionException ex) {
            log.log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
