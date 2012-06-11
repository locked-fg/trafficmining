/*
 * $Id: BackgroundWorkerBeanInfo.java 6 2006-06-30 19:40:18Z rbair $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx;

import java.beans.BeanDescriptor;

/**
 *
 * @author rbair
 */
public class BackgroundWorkerBeanInfo extends BeanInfoSupport {
    
    /** Creates a new instance of BackgroundWorkerBeanInfo */
    public BackgroundWorkerBeanInfo() {
        super(BackgroundWorker.class);
    }

    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setName("BackgroundWorker");
        bd.setShortDescription("A JavaBean for writing multithreaded Swing GUIs");
        
        setHidden(true, "backgroundListeners", "done", "propertyChangeListeners", "running", "progress");
//        setColorIconName("BackgroundWorker32.png");
//        setSmallColorIconName("BackgroundWorker16.png");
    }
    
}
