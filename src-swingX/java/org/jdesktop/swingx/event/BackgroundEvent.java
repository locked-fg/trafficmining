/*
 * $Id: BackgroundEvent.java 5 2006-06-30 19:39:54Z rbair $
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

package org.jdesktop.swingx.event;

import java.util.EventObject;
import org.jdesktop.swingx.BackgroundWorker;

/**
 * The Event object for events fired to BackgroundListeners. If data has been
 * published, it can be retrieved via the getData() method.
 *
 * @author rbair
 */
public class BackgroundEvent extends EventObject {
    private Object[] data;
    
    /** Creates a new instance of BackgroundEvent */
    public BackgroundEvent(BackgroundWorker source) {
        super(source);
    }
    
    /** Creates a new instance of BackgroundEvent */
    public BackgroundEvent(BackgroundWorker source, Object[] data) {
        super(source);
        this.data = data;
    }
    
    /**
     * @return the BackgroundWorker that fired the event
     */
    public BackgroundWorker getWorker() {
        return (BackgroundWorker)super.getSource();
    }
    
    /**
     * @return data associated with the event. In particular, this is used in the
     * <code>process</code> method of the BackgroundListener to retrieve data that
     * is to be displayed in the GUI.
     */
    public Object[] getData() {
        return data;
    }
}
