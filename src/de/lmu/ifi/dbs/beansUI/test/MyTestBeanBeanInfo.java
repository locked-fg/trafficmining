package de.lmu.ifi.dbs.beansUI.test;

import de.lmu.ifi.dbs.beansUI.ColorPropertyDescriptor;
import de.lmu.ifi.dbs.beansUI.EnumPropertyDescriptor;
import de.lmu.ifi.dbs.beansUI.StringListPropertyDescriptor;
import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyTestBeanBeanInfo extends SimpleBeanInfo {

    public MyTestBeanBeanInfo() {
        super();
    }

    @Override
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(MyTestBean.class);
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[]{
                        new PropertyDescriptor("myBool", MyTestBean.class),
                        new PropertyDescriptor("myDouble", MyTestBean.class),
                        new PropertyDescriptor("myInt", MyTestBean.class),
                        new PropertyDescriptor("myString", MyTestBean.class),
                        new EnumPropertyDescriptor("myFoo", MyTestBean.class, MyTestBean.Foo.class),
                        new ColorPropertyDescriptor("color", MyTestBean.class),
                        new StringListPropertyDescriptor("myListOptions", MyTestBean.class, MyTestBean.stringOptions),};
        } catch (IntrospectionException ex) {
            Logger.getLogger(MyTestBeanBeanInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
