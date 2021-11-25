/*
 * QCQuickSetWindow.java
 *
 * Created on 01 February 2005, 21:11
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
/**
 *
 * @author  hhulme
 */
/**
     *This Quickset window listens to the names.
     *And changes the settings on the QCMeasurementStructs.
     *Its a bit clumsy.
     * @author  hhulme
     */
 public class QCQuicksetWindow extends JFrame {
        
     
        private QCChart m_qcchart;
        private DragAndDropList m_quickSetMeasList;
        
        private JComboBox m_quicksetCombo;
        
        
        
        private JRadioButton m_forwardButton;
        private JRadioButton m_reverseButton;
        
        private ButtonGroup m_buttonGroup;
        /** Creates a new instance of QCQuicksetWindow.
         */
public QCQuicksetWindow(QCChart qcchart) {
    m_qcchart = qcchart;
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            setupComponents();
            pack();
        }
        
        /**
         *set up the GUI
         */
        private void setupComponents() {
            DragAndDropPanel panel = new DragAndDropPanel();
            panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            
            panel.setLayout(new GridBagLayout());
            
            m_quickSetMeasList = new DragAndDropList();
            JScrollPane scrollList = new JScrollPane(m_quickSetMeasList);
            
            m_quickSetMeasList.setModel(m_qcchart.new SelectedMeasModel());
            m_quickSetMeasList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            
            
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 2;
            constraints.weightx = 8.0;
            constraints.weighty = 8.0;
            constraints.fill = GridBagConstraints.BOTH;
 //           gridbag.setConstraints(scrollList, constraints);
            panel.add(scrollList, constraints);
            
            m_quicksetCombo = new JComboBox(m_qcchart.getCLMOfRepSets());
            m_quicksetCombo.setSelectedItem(null);
            
            constraints.gridx = 3;
            constraints.gridwidth = 1;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            panel.add(m_quicksetCombo, constraints);
            
            m_forwardButton = new JRadioButton("forward");
            m_reverseButton = new JRadioButton("reverse");
            JPanel buttonPanel = new JPanel();
            
            m_buttonGroup = new ButtonGroup();
            m_buttonGroup.add(m_forwardButton);
            m_buttonGroup.add(m_reverseButton);
            buttonPanel.add(m_forwardButton);
            buttonPanel.add(m_reverseButton);
            constraints.gridx = 4;
            panel.add(buttonPanel, constraints);
            
            constraints.gridy=1;
            constraints.gridx = 0;
            JButton quicksetButton = new JButton("Set");
            quicksetButton.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    actionWhenSetPressed();
                }
            });
            panel.add(quicksetButton, constraints);
            
            constraints.gridy = 1;
            constraints.gridx=1;
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    m_forwardButton.setSelected(false); // doesn't seem to be working
                    m_reverseButton.setSelected(false); //doesn't seem to be working
                   
                    m_quicksetCombo.setSelectedItem(null);
                   // m_quickSetMeasList.clearSelection();
                }
            });
            panel.add(clearButton, constraints);
            
            getContentPane().add(panel);
            
        }
        
        /**When set is pressed, set the options in tab 3 of the
         *qcchart gui accordingly
         */
        public void actionWhenSetPressed() {
            /*work out which rep set group the user chose, if they did pick one*/
            String group = (String) m_quicksetCombo.getSelectedItem();
            /*work out if the user chose forward or reverse (or neither)*/
            boolean isForward = m_forwardButton.isSelected();
            boolean isReverse = m_reverseButton.isSelected();
            
            /*work out which measurement names the user wants to apply those settings to*/
            Object[] selMeas = m_quickSetMeasList.getSelectedValues();
            
        /*If the user didn't select any measurements,
         *then there's nothing to do,
         *so just return
         */
            if (selMeas == null || selMeas.length == 0) {
                return;
            }
            //loop through the selected measurements
            for (int i = 0 ; i < selMeas.length ; i++) {
                
                QCMeasurementStruct[] structs = m_qcchart.getMeasurementStructs();
                
                //loop through all the structs
                for (int j = 0 ; j < structs.length ; j++) {
                    QCMeasurementStruct struc = structs[j];
                    
                    //test if they match
                    if (struc.getName().equals((String) selMeas[i])) {
                        //if they do match, set that measurements to have the settings selected (if any)
                        if (isForward) {
                            struc.selectForward();
                        }
                        if (isReverse) {
                            struc.selectReverse();
                        }
                        if (group!=null) {
                            struc.getComboBox().setSelectedItem(group);
                        }
                    }
                }
            }
        }
        
        /**Reset the model of the quickset window*/
        public void resetModel(ListModel model) {
            m_quickSetMeasList.setModel(model);
        }
        
        public void updateGroupNames() {
            m_quicksetCombo.setModel(m_qcchart.getCLMOfRepSets());
            
        }
    }
