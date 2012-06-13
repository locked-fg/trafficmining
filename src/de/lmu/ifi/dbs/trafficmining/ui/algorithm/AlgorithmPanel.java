package de.lmu.ifi.dbs.trafficmining.ui.algorithm;

import de.lmu.ifi.dbs.trafficmining.algorithms.Algorithm;
import de.lmu.ifi.dbs.trafficmining.ui.BeansConfigDialog;
import de.lmu.ifi.dbs.utilities.Arrays2;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Franz
 */
public class AlgorithmPanel extends javax.swing.JPanel {
    
    static final Logger log = Logger.getLogger(AlgorithmPanel.class.getName());
    private final DefaultComboBoxModel<AlgorithmComboBoxElement> model = new DefaultComboBoxModel<>();
    private File pluginDir;
    //
    public static final String EVT_CONFIG = AlgorithmPanel.class.getName() + " CONFIG";
    public static final String EVT_EXECUTE = AlgorithmPanel.class.getName() + " EXECUTE";
    public static final String EVT_CANCEL = AlgorithmPanel.class.getName() + " CANCEL";
    private final MyObserver observerable = new MyObserver();

    /**
     * Creates new form AlgorithmPanel
     */
    public AlgorithmPanel() {
        initComponents();
        
        configureButton.addActionListener(observerable);
        executeButton.addActionListener(observerable);
        cancelButton.addActionListener(observerable);
    }
    
    class MyObserver extends Observable implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setChanged();
            if (e.getSource().equals(configureButton)) {
                notifyObservers(EVT_CONFIG);
            } else if (e.getSource().equals(executeButton)) {
                notifyObservers(EVT_EXECUTE);
            } else if (e.getSource().equals(cancelButton)) {
                notifyObservers(EVT_CANCEL);
            }
        }
    }
    
    public void setPluginDir(File pluginDir) {
        this.pluginDir = pluginDir;
        initAlgorithmComboBox();
    }
    
    private void initAlgorithmComboBox() {
        new AlgorithmComboboxLoader(model, pluginDir).load();
        if (model.getSize() > 0) {
            configureButton.setEnabled(true);
            executeButton.setEnabled(true);
            algorithmComboBox.setSelectedIndex(0);
        }
    }
    
    public Algorithm configureAlgorithm(Algorithm currentAlgorithm, JFrame parent) {
        BeansConfigDialog bcd = null;
        try {
            ensureInitAlgorithm(currentAlgorithm);
            // FIXME
            JFrame f = null;
            if (currentAlgorithm != null) {
                bcd = new BeansConfigDialog(parent, true);
                bcd.setBean(currentAlgorithm);
                bcd.setLocationRelativeTo(null);
                bcd.setVisible(true);
            }
        } catch (Exception ex) {
            if (bcd != null) {
                bcd.dispose();
            }
            // tell the user that s.th went wrong
            log.log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "The selected algorithm could not be instanciated.\n"
                    + "This can be caused by a faulty plugin:\n"
                    + Arrays2.join(ex.getStackTrace(), "\n") + "\n"
                    + "Maybe the log file is more informative about what went wrong.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        return currentAlgorithm;
    }
    
    public void setBusy(boolean flag) {
        progressBar.setIndeterminate(flag);
        if (!flag) {
            progressBar.setValue(100);
            enableExecute(!flag);
        }
    }
    
    public void setState(int i) {
        if (i < 0 || i > 100) {
            throw new IllegalArgumentException("i must be in [0,100], was " + i);
        }
        progressBar.setValue(i);
        if (i == 100) {
            setBusy(false);
        }
    }
    
    private void enableExecute(boolean flag) {
        executeButton.setEnabled(flag);
        cancelButton.setEnabled(!flag);
    }

    /**
     * Ensures that currentAlgorithm is set and initialized. The method also
     * sets the current graph and the node list.
     *
     * For any configuration beyond this basic setting, call
     * #configureAlgorithm()
     *
     * @throws InstantiationException if the algorithm could not be instanciated
     * @throws IllegalAccessException if the algorithm could not be instanciated
     */
    public Algorithm ensureInitAlgorithm(Algorithm currentAlgorithm) throws InstantiationException,
            IllegalAccessException {
        final Object selectedItem = algorithmComboBox.getSelectedItem();
        
        if (selectedItem == null || !AlgorithmComboBoxElement.class.isAssignableFrom(selectedItem.getClass())) {
            log.log(Level.WARNING, "item not a combobox element: {0}", selectedItem);
            return null;
        }
        final AlgorithmComboBoxElement boxElement = (AlgorithmComboBoxElement) selectedItem;
        final Class<Algorithm> clazz = boxElement.getAlgorithm();

        // new algorithm or same as previous?
        if (currentAlgorithm == null || !currentAlgorithm.getClass().equals(clazz)) {
            currentAlgorithm = clazz.newInstance();
        }
        return currentAlgorithm;
    }
    
    public void addButtonObserver(Observer o) {
        observerable.addObserver(o);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        algorithmComboBox = new javax.swing.JComboBox();
        configureButton = new javax.swing.JButton();
        executeButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();

        setLayout(new java.awt.GridBagLayout());

        algorithmComboBox.setModel(model);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(algorithmComboBox, gridBagConstraints);

        configureButton.setText("...");
        configureButton.setPreferredSize(new java.awt.Dimension(30, 23));
        add(configureButton, new java.awt.GridBagConstraints());

        executeButton.setText("Execute");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        add(executeButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        add(cancelButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(progressBar, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox algorithmComboBox;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton configureButton;
    private javax.swing.JButton executeButton;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
