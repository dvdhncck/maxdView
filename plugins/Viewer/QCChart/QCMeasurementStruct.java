/*
 * QCMeasurementStruct.java
 *
 * Created on 24 January 2005, 07:09
 */
import javax.swing.*;

 /**Struct for keeping info pertinent to a picked measurement
     *
     *keeps references to the UI objects which the user uses to pick options about the measurement
  *@author  hhulme
     */
public class QCMeasurementStruct {
        /**Name of the meas, col name*/
        private String measurementName;
        /**Combo box for specifying replicate group of the meas*/
        private  JComboBox repSetComboBox;
        /**Button panel of radio buttons for specifying hyb direction*/
        private  JPanel buttonPanel;
        
        /**Radio button for forward hyb*/
        private JRadioButton forward;
        
        /**Radio button for reverse hyb*/
        private JRadioButton reverse;
        
        private DefaultListModel m_model;
        
        /**Creates a new measurement struct*/
        public QCMeasurementStruct(String name, DefaultListModel model) {
            measurementName = name;
            m_model = model;

            repSetComboBox = new JComboBox(new ComboListModel(m_model));
 
            buttonPanel = new JPanel();
            forward = new JRadioButton("forward",true);
            reverse = new JRadioButton("reverse", false);
            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(forward);
            buttonGroup.add(reverse);
            buttonPanel.add(forward);
            buttonPanel.add(reverse);
        }
        
        /**Gets name of the measurement*/
        public String getName() {
            return measurementName;
        }
        
        /**Gets the name of the replicate group specified for the measurement*/
        public String getRepSet() {
            return (String) repSetComboBox.getSelectedItem();
        }
        
        /**Gets the hyb direction specification*/
        public boolean isForward() {
            return forward.isSelected();
        }
        
        /**Gets the button panel*/
        public JPanel getButtonPanel() {
            return buttonPanel;
        }
        
        /**Gets the combo box*/
        public JComboBox getComboBox() {
            return repSetComboBox;
        }
        
        public void selectForward() {
            forward.setSelected(true);
            reverse.setSelected(false);
        }
        
        public void selectReverse() {
            reverse.setSelected(true);
            forward.setSelected(false);
        }
        
        
    }