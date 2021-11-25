/**
 * Class.java
 *This is a utility class for creating a DefaultComboBoxModel
 *when you already have a Default list model, and you want the 
 *elements in it to be the same.
 *Once created, 
 *any elements added to or removed from the original ListModel
 *will then be automatically adde to or removed from this ComboBoxModel,
 *so that you don't have to worry about it.
 
 * Created on 21 January 2005, 12:42
 *@author  hhulme
 */
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class ComboListModel extends DefaultComboBoxModel {
    
    DefaultListModel m_mod;
    
    /** Creates a new instance of Class
     *@param mod a default list model, which may already contain some elements
     */
    public ComboListModel(DefaultListModel mod) {
        super();
        //remember the list model
        m_mod = mod;
        //create a new listener to listen what happens in the list model
        ListDataListener mylistener = new listener();
        mod.addListDataListener(mylistener);
        
        //add all elements of the list model to this combo list model
        Enumeration enumeration = mod.elements();
        while (enumeration.hasMoreElements()) {
            addElement(enumeration.nextElement());
        }
    }
    
    /**A class for listening to additions / deletions / changes to a list model
     *and updating the combo list model to reflect the changes
     */
    private class listener implements ListDataListener {
        /**
         *creates a new listener
         */
        public listener() {
        }
        
        /**
         *action on contents changed: 
         *change all corresponding elements of 
         *the combolist model accordingly
         */
        public void contentsChanged(ListDataEvent e) {
            int index0 = e.getIndex0();
            int index1 = e.getIndex1();
            for (int i = index0 ; i <=index1 ; i++) {
                removeElementAt(i);
                insertElementAt(m_mod.getElementAt(i),i);
            }
        }
        
        /**
         *action on interval added:
         *add an identical interval to the combo box model
         */
        public void intervalAdded(ListDataEvent e) {
            int index0 = e.getIndex0();
            int index1 = e.getIndex1();
            for (int i = index0 ; i <=index1 ; i++) {
                insertElementAt(m_mod.getElementAt(i),i);
            }
        }
        
        /**action on interval removed:
         *remove the corresponding interval from the combo box model
         */
        public void intervalRemoved(ListDataEvent e) {
            int index0 = e.getIndex0();
            int index1 = e.getIndex1();
            for (int i = index1 ; i >=index0 ; i--) {
                removeElementAt(i);
            }
        }
        
    }
    
}
