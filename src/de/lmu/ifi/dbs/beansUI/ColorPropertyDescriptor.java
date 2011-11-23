package de.lmu.ifi.dbs.beansUI;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import de.lmu.ifi.dbs.beansUI.test.MyTestBean;

/**
 * @author graf
 */
public class ColorPropertyDescriptor extends PropertyDescriptor {

    public ColorPropertyDescriptor(String attributeName, Class<MyTestBean> beanClass) throws IntrospectionException {
        super(attributeName, beanClass);
    }

    @Override
    public PropertyEditor createPropertyEditor(Object bean) {
        return new ColorPropertyEditor();
    }
}
