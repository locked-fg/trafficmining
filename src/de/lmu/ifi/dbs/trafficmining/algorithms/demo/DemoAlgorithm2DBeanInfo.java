package de.lmu.ifi.dbs.trafficmining.algorithms.demo;

import de.lmu.ifi.dbs.beansUI.EnumPropertyDescriptor;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author graf
 */
public class DemoAlgorithm2DBeanInfo extends SimpleBeanInfo {

    private static final Logger log = Logger.getLogger(DemoAlgorithm2DBeanInfo.class.getName());

    public DemoAlgorithm2DBeanInfo() {
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
                        new EnumPropertyDescriptor("myAttribs", DemoAlgorithm2D.class, DemoAlgorithm2D.ATTRIBS.class),};
        } catch (IntrospectionException ex) {
            log.log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
