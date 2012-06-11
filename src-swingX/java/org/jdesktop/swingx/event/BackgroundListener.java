/*
 * $Id: BackgroundListener.java 5 2006-06-30 19:39:54Z rbair $
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

/**
 * The Event Handler listener for the BackgroundWorker. The various methods in
 * this interface are called at various points in the processing of the background
 * task.
 *
 * @author rbair
 */
public interface BackgroundListener extends java.util.EventListener {
    /**
     * Always called on the EDT right before the background task actually begins.
     */
    public void started(BackgroundEvent evt);
    /**
     * Always called on a background thread. This is the method that all long
     * running tasks are placed in. If you need to update the Swing GUI, you may call
     * the setProgress/setProgressPercent methods on BackgroundWorker, or you may
     * call the publish method on BackgroundWorker to cause some data to be passed
     * to the GUI.
     */
    public void doInBackground(BackgroundEvent evt);
    /**
     * Always called on the EDT when all background work as finished
     */
    public void done(BackgroundEvent evt);
    /**
     * Always called on the EDT. This method may be called many times during the
     * processing of the background task (depending on how many times the doInBackground
     * method calls publish). This method will be called concurrently with work
     * going on in the doInBackground method.
     */
    public void process(BackgroundEvent evt);
}
