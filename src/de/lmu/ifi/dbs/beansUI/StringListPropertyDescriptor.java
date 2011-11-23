package de.lmu.ifi.dbs.beansUI;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author graf
 */
public class StringListPropertyDescriptor extends PropertyDescriptor {

    private final List<String> options;

    public StringListPropertyDescriptor(String propertyName, Class<?> beanClass, List<String> options) throws IntrospectionException {
        super(propertyName, beanClass);
        this.options = Collections.unmodifiableList(new ArrayList<String>(options));
    }

    @Override
    public PropertyEditor createPropertyEditor(Object bean) {
        return new StringListPropertyEditor(options);
    }
}
