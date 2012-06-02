/*
 * JXMapKitBeanInfo.java
 *
 * Created on May 1, 2007, 11:06:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx;

import org.jdesktop.swingx.editors.EnumPropertyEditor;

/**
 *
 * @author joshy
 */
public class JXMapKitBeanInfo extends BeanInfoSupport {
    
    public JXMapKitBeanInfo() {
        super(JXMapKit.class);
    }
    protected void initialize() {
        setPropertyEditor(DefaultProvidersPropertyEditor.class, "defaultProvider");        
        setPreferred(true, "defaultProvider");
        setPreferred(true, "miniMapVisible");
        setPreferred(true, "defaultProvider");
        setPreferred(true, "zoomButtonsVisible");
        setPreferred(true, "zoomSliderVisible");
    }
    public static final class DefaultProvidersPropertyEditor extends EnumPropertyEditor {
        public DefaultProvidersPropertyEditor() {
            super(JXMapKit.DefaultProviders.class);
        }
    }
}
