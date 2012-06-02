/*
 * $Id: BackgroundWorker.java 314 2011-12-09 15:50:26Z kschaefe $
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.jdesktop.beans.AbstractBean;
import org.jdesktop.swingx.event.BackgroundEvent;
import org.jdesktop.swingx.event.BackgroundListener;

/**
 * <p>A JavaBean to perform lengthy GUI-interacting tasks in a dedicated thread.</p>
 *
 * <p>When writing a multi-threaded application using Swing, there are
 * two constraints to keep in mind:
 * (refer to 
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html">
 *   How to Use Threads
 * </a> for more details):
 * <ul>
 *   <li> Time-consuming tasks should not be run on the <i>Event
 *        Dispatch Thread</i>. Otherwise the application becomes unresponsive.
 *   </li>
 *   <li> Swing components should be accessed on the <i>Event
 *        Dispatch Thread</i> only.
 *   </li>
 * </ul></p>
 *
 * <p>These constraints mean that a GUI application with time intensive
 * computing needs at least two threads:  1) a thread to perform the lengthy
 * task and 2) the <i>Event Dispatch Thread</i> (EDT) for all GUI-related
 * activities.  This involves inter-thread communication which can be
 * tricky to implement.</p>
 *
 * <p>BackgroundWorker is a non visual JavaBean that simplifies writing these
 * multithreaded tasks from within a GUI Builder environment. It is built upon 
 * SwingWorker, and exhibits most of the same API features as SwingWorker.</p>
 *
 * <h2>Workflow</h2>
 * <p>There are <b>two</b> threads involved in the life cycle of a 
 * {@code BackgroundWorker} (unlike SwingWorker which can operate with three
 * threads):
 * <ul>
 *  <li>
 *      <p><i>Event Dispatch Thread</i>:  All Swing related activities occur
 *      on this thread. {@code BackgroundWorker} invokes the
 *      {@link #process process} and {@link #done} events and notifies
 *      any {@code PropertyChangeListeners} on this thread. In addition, the
 *      {@link #execute} method must be called on this thread</p>
 *  </li>
 *  <li>
 *      <p><i>Worker</i> thread: The {@link #doInBackground} 
 *      event is called on this thread.
 *      This is where all background activities should happen..
 *  </li>
 * </ul>
 * 
 * <p>A single BackgroundWorker instance may be used more than once. You cannot
 * however call <code>execute</code> while the BackgroundWorker is in progress.
 * </p>
 *
 * <p>You can retrieve and set progress information on the BackgroundWorker at
 * any time. There are two complementary methods for doing so,
 * <code>setProgress(float)</code> and <code>setProgressPercent(int)</code>.
 * The first method accepts a float between the range of 0 to 1 while the
 * second method accepts an int between the values of 0 and 100.</p>
 *
 * <p>You may also specify the exact ExecutorService to use for the background
 * task. This allows you to specify a thread pool, default thread priority,
 * and other such attributes of the background task. See the ExecutorService
 * API for more information.</p>
 *
 * <p>BackgroundWorker operates on the basis of callbacks. You first register a
 * BackgroundListener with the BackgroundWorker, and implement the relevant methods.
 * For example, you implement the <code>doInBackground</code> method if you have 
 * tasks that must be performed on the background thread. </p>
 *
 * <p>From within the <code>doInBackground</code> method you may call the
 * <code>publish</code> method of your BackgroundWorker to place some data on
 * a queue to be processed on the Event Dispatch Thread. At some point the
 * <code>process</code> event will be fired with this data available within
 * the <code>BackgroundEvent</code> object. In this event handler you may freely
 * modify the GUI.</p>
 *
 * <h2>Sample Usage</h2>
 * <p>The following example illustrates the simplest use case.  Some 
 * processing is done in the background and when done you update a Swing 
 * component.</p>
 *
 * <p>Say we want to find the "Meaning of Life" and display the result in
 * a {@code JLabel}.
 * 
 * <pre><code>
 *   final JLabel label;
 *   class MeaningOfLifeFinder implements BackgroundListener {
 *       public void doInBackground(BackgroundEvent evt) {
 *           String meaningOfLife = findTheMeaningOfLife();
 *           evt.getWorker().publish(meaningOfLife);
 *       }
 *
 *       public void process(BackgroundEvent evt) {
 *           label.setText("" + evt.getData());
 *       }
 *
 *       public void done(BackgroundEvent evt) {}
 *       public void started(BackgroundEvent evt) {}
 *   }
 * 
 *   (new MeaningOfLifeFinder()).execute();
 * </pre></code></p>
 * 
 * @author rbair
 */
public class BackgroundWorker extends AbstractBean {
    /**
     * List of Background listeners
     */
    private List<BackgroundListener> listeners = new ArrayList<BackgroundListener>();
    /**
     * The SwingWorker delegate. The delegate is used for almost everything:
     * maintaining state information, progress information, and so forth.
     */
    private DelegateWorker delegate;
    /**
     * If specified, this ExecutorService will be used to run the SwingWorker
     */
    private ExecutorService executor;
    /**
     * True if execute() has been called, and the background task has not yet finished
     */
    private boolean running = false;
    
    /** Creates a new instance of BackgroundWorker */
    public BackgroundWorker() {
        delegate = new DelegateWorker();
    }

    /**
     * @returns true if the BackgroundWorker has finished an execution. If
     * the BackgroundWorker has not yet been executed, this will return false.
     * If the BackgroundWorker has been executed, finishes, and is re-executed
     * then this will be false until the new execution finishes.
     *
     * This is a bound property.
     */
    public boolean isDone() {
        return !running && delegate.isDone();
    }
    
    /**
     * @return true if the BackgroundWorker is currently running, false otherwise
     */
    public final boolean isRunning() {
        return running;
    }
    
    //------------------------------------------------ Bean Property Methods
    
    /**
     * Set the progress of this worker based on a value between 0 and 1 where 0
     * represents 0% and 1 represents 100%. If the progress has changed a percentage
     * point, then a property change event will be fired. Not that changes from 0.1
     * to 0.11 will not be registered.
     *
     * This is a convenience method, and is analogous to the setProgressPercent method.
     *
     * This method may be called from any thread.
     */
    public final void setProgress(float progress) {
        if (progress < 0 || progress > 1) {
            throw new IllegalArgumentException("the value should be from 0 to 1");
        }
        int percent = (int)(progress * 100);
        delegate.setProgressPercent(percent);
    }

    /**
     * Returns the {@code progress} bound property.
     * This method may be called from any thread.
     * 
     * @return the progress bound property.
     */
    public final float getProgress() {
        return delegate.getProgress() / 100f;
    }
    
    /**
     * Set the progress of this worker based on a value between 0 and 100 where 0
     * represents 0% and 100 represents 100%. If the progress has changed a percentage
     * point, then a property change event will be fired.
     *
     * This method may be called from any thread.
     */
    public final void setProgressPercent(int percent) {
        delegate.setProgressPercent(percent);
    }
    
    /**
     * Returns the {@code progressPercent} bound property.
     * This method may be called from any thread.
     * 
     * @return the progressPercent bound property.
     */
    public final int getProgressPercent() {
        return delegate.getProgress();
    }
    
    /**
     * Sets the {@code executorService} bound property. This property, if not
     * null, will specify the ExecutorService to use for running the background
     * task. If null, then the default ExecutorService of SwingWorker will be
     * used instead.
     *
     * This method must only be called on the EDT, and may not be called while
     * the BackgroundWorker is running.
     */
    protected final void setExecutorService(ExecutorService svc) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method can only be called on the EDT");
        }
        
        if (running) {
            throw new IllegalStateException("Cannot set the executor service if the worker is running");
        }
        
        Object old = this.executor;
        this.executor = svc;
        firePropertyChange("executorService", old, this.executor);
    }
    
    /**
     * Returns the {@code executorService} bound property.
     * This method may be called from any thread.
     * 
     * @return the executorService bound property.
     */
    protected final ExecutorService getExecutorService() {
        return executor;
    }
    
    //----------------------------------------------------- Business Methods
    
    /**
     * <p>Executes the BackgroundWorker, causing it to begin processing.
     * This method <b>MUST</b> be called on the EDT, and <b>MUST NOT</b> be
     * called if the BackgroundWorker is already running.</p>
     *
     * <p>Execution follows these steps:
     *  <ol>
     *      <li>The {@code running} and {@code done} states are updated and
     *          PropertyChangeEvents are fired for them if necessary</li>
     *      <li>The {@code started} event of the BackroundListener is called</li>
     *      <li>The {@code doInBackground} event of the BackgroundListener is called.
     *          This is where background tasks must be performed. If you want to publish
     *          some data to the Event Dispatch Thread (EDT), then you must call the
     *          <code>publish</code> method of the BackgroundWorker. Calling the
     *          <code>publish</code> method will in turn cause the
     *          <code>process</code> event handler to be called on the EDT. From
     *          this event handler you are free to update the Swing GUI in any way.
     *          If you need to update the <code>progress</code> state, you may do
     *          so directly from the <code>doInBackground</code> event handler. Updates
     *          to the progress will be set via PropertyChangeEvents on the EDT.
     *      </li>
     *      <li>Once concluded, the {@code done} event will be called. Subsequent
     *          to handling the event, the {@code running} and {@code done} states are updated and
     *          PropertyChangeEvents are fired for them if necessary</li>
     *  </ol>
     * </p>
     */
    public void execute() {
        //if another task is already executing, fire an IllegalStateException
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method can only be called on the EDT");
        }

        if (running) {
            throw new IllegalStateException("A background task is already in progress");
        }
        
        delegate = new DelegateWorker();
        boolean oldDone = isDone();
        running = true;
        //fire property changes for running and done
        firePropertyChange("running", false, true);
        firePropertyChange("done", oldDone, false);
        //fire the event indicating that execution is about to begin
        fireStartedEvent();
        if (executor == null) {
            delegate.execute();
        } else {
            executor.submit(delegate);
        }
    }
    
    /**
     * Causes the given "chunks" of data to be published on the Event Dispatch Thread.
     * This method causes the <code>process</code> event handler to be called. That
     * event handler is guaranteed to be called on the EDT.
     *
     * This method <b>MUST</b> be called on the background thread.
     */
    public void publish(Object... chunks) {
        //Just forward this off to the SwingWorker.
        delegate.doPublish(chunks);
    }

    //-------------------------------------------------------- Event Methods
    
    /**
     * Adds a BackgroundListener
     */
    public void addBackgroundListener(BackgroundListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes the given BackgroundListener such that it will no longer be notified
     * of background events.
     */
    public void removeBackgroundListener(BackgroundListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Returns an array of all BackgroundListeners registered with this object
     */
    public BackgroundListener[] getBackgroundListeners() {
        return listeners.toArray(new BackgroundListener[0]);
    }

    ///must be called on the EDT
    private void fireStartedEvent() {
        BackgroundEvent evt = new BackgroundEvent(this);
        for (BackgroundListener listener : listeners) {
            listener.started(evt);
        }
    }
    
    ///must be called on the EDT
    private void fireDoneEvent() {
        BackgroundEvent evt = new BackgroundEvent(this);
        for (BackgroundListener listener : listeners) {
            listener.done(evt);
        }
    }
    
    ///must be called on the background thread
    private void fireDoInBackgroundEvent() {
        BackgroundEvent evt = new BackgroundEvent(this);
        for (BackgroundListener listener : listeners) {
            listener.doInBackground(evt);
        }
    }
    
    ///must be called on the EDT
    private void fireProcessEvent(Object[] data) {
        BackgroundEvent evt = new BackgroundEvent(BackgroundWorker.this, data);
        for (BackgroundListener listener : listeners) {
            listener.process(evt);
        }
    }

    //------------------------------------------------------------- Delegate
    
    /**
     * Delegate SwingWorker implementation. BackgroundWorker delegates all of its
     * core tasks to this class. DelegateWorker registers as a PropertyChangeListener
     * on itself so that it can forward any relevant property change events (such as
     * progress changes) on to listeners of BackgroundWorker.
     */
    private final class DelegateWorker extends SwingWorker implements PropertyChangeListener {
        /**
         * Create a new DelegateWorker. This is not intended to be called outside
         * of BackgroundWorker
         */
        private DelegateWorker() {
            //register as a listener of myself to catch SwingWorker property change
            //notifications and propogate them out to listeners of BackgroundWorker
            addPropertyChangeListener(this);
        }
        
        /**
         * Fires the doInBackgroundEvent. This method must be called on a
         * background thread.
         */
        protected Object doInBackground() throws Exception {
            fireDoInBackgroundEvent();
            return null;
        }

        /**
         * Fires the done event. This must be called on the EDT.
         */
        protected void done() {
            fireDoneEvent();
            running = false;
            firePropertyChange("running", true, false);
            firePropertyChange("done", false, true);
        }

        /**
         * Fires the process event. This must be called on the EDT.
         */
        protected void process(Object... chunks) {
            fireProcessEvent(chunks);
        }

        /**
         * Propogates (and if necessary mutates) property change events from
         * SwingWorker to BackgroundWorker. This must be called on the EDT.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            //all property change events are on the EDT
            if ("progress".equals(evt.getPropertyName())) {
                float oldProgress = ((Integer)evt.getOldValue()) / 100f;
                float newProgress = ((Integer)evt.getNewValue()) / 100f;
                BackgroundWorker.this.firePropertyChange("progress", oldProgress, newProgress);
                BackgroundWorker.this.firePropertyChange("progressPercent", evt.getOldValue(), evt.getNewValue());
            }
        }
        
        /**
         * Necessary because setProgress cannot be overridden or exposed directly
         */
        private void setProgressPercent(int percent) {
            super.setProgress(percent);
        }
        
        /**
         * Necessary because publish cannot be overridden or exposed directly
         */
        private void doPublish(Object... chunks) {
            super.publish(chunks);
        }
    }
}