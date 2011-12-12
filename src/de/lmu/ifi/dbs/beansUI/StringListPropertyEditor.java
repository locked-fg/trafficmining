package de.lmu.ifi.dbs.beansUI;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author graf
 */
public class StringListPropertyEditor extends PropertyEditorSupport {

    private final List options;

    public StringListPropertyEditor(List options) {
        this.options = options;
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof List) {
            value = new NiceToStringList((List) value);
            super.setValue(value);
        } else {
            super.setValue(value);
        }
    }

    @Override
    public Object getValue() {
        return super.getValue();
    }

    @Override
    public String[] getTags() {
        String[] out = new String[options.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = options.get(i).toString();
        }
        return out;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        List l = new ArrayList(1);
        l.add(text);
        setValue(l);
    }

    @Override
    public String getJavaInitializationString() {
        return "java.lang.String"; // ?
    }
}
