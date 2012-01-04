package de.lmu.ifi.dbs.beansUI;

import de.lmu.ifi.dbs.beansUI.test.MyTestBean;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;

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
