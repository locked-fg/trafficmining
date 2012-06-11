package de.lmu.ifi.dbs.beansUI;

import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;

public class BeanModel extends DefaultTableModel {

    static final Logger logger = Logger.getLogger(BeanModel.class.getName());
    private Object bean;
    private PropertyDescriptor[] descriptors;
    private HashMap<Class, Class> typeMap = new HashMap<>();

    public BeanModel() {
        super();
        addColumn("Attribute");
        addColumn("Value");

        typeMap.put(byte.class, Byte.class);
        typeMap.put(short.class, Short.class);
        typeMap.put(int.class, Integer.class);
        typeMap.put(long.class, Long.class);
        typeMap.put(float.class, Float.class);
        typeMap.put(double.class, Double.class);
        typeMap.put(boolean.class, Boolean.class);
        typeMap.put(char.class, Character.class);
    }

    public void setBean(Object beanObject) throws IntrospectionException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        descriptors = null;
        setRowCount(0);

        this.bean = beanObject;
        if (bean != null) {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            descriptors = beanInfo.getPropertyDescriptors();

            for (PropertyDescriptor descriptor : descriptors) {
                String name = descriptor.getDisplayName();
                Object value = descriptor.getReadMethod().invoke(bean);
                addRow(new Object[]{name, value});
            }
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (bean == null) {
            return null;
        }
        if (column == 0) { // name of the attribute
            return descriptors[row].getDisplayName();
        } else { // value of the attribute
            try { // get value from bean
                return descriptors[row].getReadMethod().invoke(bean);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(BeanModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (bean == null) {
            throw new NullPointerException("no bean set");
        }
        if (column != 1) {
            throw new IndexOutOfBoundsException("column must be 1 but was " + column);
        }

        try { // set via PropertyEditor for correct conversions
            PropertyEditor editor = getPropertyEditor(row, -1);
            try {
                editor.setAsText(aValue.toString());
            } catch (Exception e) { // some editors like coloreditor somehow
                logger.log(Level.FINE, null, e);
                editor.setValue(aValue);
            }
            Method writeMethod = descriptors[row].getWriteMethod();
            writeMethod.invoke(bean, editor.getValue());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(BeanModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    PropertyEditor getPropertyEditor(int row, int column) {
        PropertyEditor editor = descriptors[row].createPropertyEditor(bean);
        if (editor == null) {
            editor = PropertyEditorManager.findEditor(descriptors[row].getPropertyType());
        }
        return editor;
    }

    Class getPropertyType(int row, int column) {
        if (column == 0) {
            return String.class;
        } else {
            Class clazz = descriptors[row].getPropertyType();
            if (clazz.isPrimitive()) {
                return typeMap.get(clazz);
            }
            return clazz;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 1;
    }
}
