package de.lmu.ifi.dbs.beansUI;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import org.jdesktop.swingx.editors.EnumPropertyEditor;
import de.lmu.ifi.dbs.beansUI.test.MyTestBean;

/**
 * @author graf
 */
public class EnumPropertyDescriptor extends PropertyDescriptor {

    private final Class<? extends Enum> theEnum;

    public EnumPropertyDescriptor(String attributeName, Class beanClass, Class<? extends Enum> theEnum) throws IntrospectionException {
        super(attributeName, beanClass);
        this.theEnum = theEnum;
    }

    @Override
    public PropertyEditor createPropertyEditor(Object bean) {
        return new EnumPropertyEditor(theEnum);
    }
}
