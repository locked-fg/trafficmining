package de.lmu.ifi.dbs.paros.algorithms;

import de.lmu.ifi.dbs.beansUI.StringListPropertyDescriptor;
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
public class DemoAlgorithm1DBeanInfo extends SimpleBeanInfo {

    private static final Logger log = Logger.getLogger(DemoAlgorithm1DBeanInfo.class.getName());

    public DemoAlgorithm1DBeanInfo() {
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
                        new StringListPropertyDescriptor("attribs", DemoAlgorithm1D.class, DemoAlgorithm1D.attribsOptions)};
        } catch (IntrospectionException ex) {
            log.log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
