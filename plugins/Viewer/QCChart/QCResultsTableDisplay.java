/*
 * QCResultsTable.java
 *
 * Created on 26 January 2005, 13:05
 */
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
/**
 *
 * @author  hhulme
 */
public class QCResultsTableDisplay extends JFrame {
    
    
    
           //reference to the parent maxd view
    private maxdView mview;
    /** Creates a new instance of QCResultsTable */
    public QCResultsTableDisplay(QCResults results, maxdView _mview) {
        super("QC Results Table");
        
        mview = _mview;
        
        
        mview.decorateFrame(this);
        
        //create a table model of the results
        TableModel tableModel = new ResultsTableModel(results);
        
        //create a table which views the table model
        JTable table = new JTable(tableModel);
        
        //setting to enabled means the user can
        //copy and paste from the table, which is useful
        //Unfortualy it also makes the cells editable :-)
        //oh well
        
        //table.setEnabled(false);
        
        //scroll pane for displaying the table
        JScrollPane scrollPane = new JScrollPane(table);
        
        String message = getSummaryMessage(results);
        
       
        
        JTextArea explanationTextArea = new JTextArea(message);
        explanationTextArea.setEditable(false);
        
        
        
        JScrollPane messageScrollPane = new JScrollPane(explanationTextArea);
        
        this.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        cons.fill = GridBagConstraints.BOTH;
        cons.gridx = 0;
        cons.gridy = 0;
        this.getContentPane().add(messageScrollPane, cons);
        cons.gridy++;
        this.getContentPane().add(scrollPane,cons);
        this.pack();
        

    } 
    
    private String getSummaryMessage(QCResults results) {
        StringBuffer message = new StringBuffer();
         message.append("The results of the calculation are:\n");
        message.append("mean of logged vars: (mu)\t\t " + results.getMeanOfLoggedVars() + "\n");
        message.append("stdev of logged vars: (sigma)\\tt " + results.getStdevOfLoggedVars() + "\n");
        message.append("Chosen significance value (alpha)\t " + results.getPrimaryControlLineSet().sigLevel + "\n");
        message.append("Phi^(-1) (1-alpha/2) \t\t " + results.getPrimaryControlLineSet().width +"\n");
        message.append("Centerline \t\t\t " + results.getPrimaryControlLineSet().centerLine + "\n");
        message.append("Upper Control Line \t\t " + results.getPrimaryControlLineSet().upperControlLine + "\n");
        message.append("Lower Control Line \t\t " + results.getPrimaryControlLineSet().lowerControlLine + "\n\n");
        
        message.append("Confidence Bars:\n");
        message.append("Chosen significance value (alpha_mu)\t " + results.getPrimaryControlLineSet().sigLevel_mu + "\n");
        message.append("Chosen significance value (alpha_sigma)\t " + results.getPrimaryControlLineSet().sigLevel_sigma + "\n");
        
        QCControlLineSet controlLineSet = results.getPrimaryControlLineSet();
        
        double ucl = controlLineSet.upperControlLine;
        double lcl = controlLineSet.lowerControlLine;
        
        double cl_bottom = controlLineSet.confidenceBarCentreLine[0];
        double cl_top = controlLineSet.confidenceBarCentreLine[1];
        
        double ucl_bottom = controlLineSet.confidenceBarUCL[0];
        double ucl_top = controlLineSet.confidenceBarUCL[1];
        
        double lcl_bottom = controlLineSet.confidenceBarLCL[0];
        double lcl_top = controlLineSet.confidenceBarLCL[1];
        message.append("Confidence Bar for UCL:\t " + ucl_bottom + "\t to " + ucl_top + "\n");
        message.append("Confidence Bar for CL:\t " + cl_bottom + "\t to " + cl_top + "\n");
        message.append("Confidence Bar for LCL:\t " + lcl_bottom + "\t to " + lcl_top + "\n");
        
        return message.toString();
    }
    
    /**Table model which contains info about each slide:
     *Group, name, variance of difference from mean, and log 2 of this value
     */
    private class ResultsTableModel extends DefaultTableModel {
        
        public ResultsTableModel(QCResults results) {
            super();
            //set up headers of table
            String[] colIDs = new String[]{"measurement", "group", "variance of diff", "log 2 var", "flag"};
            
            Vector diagnoses = new Vector(6);
            diagnoses.add(QCResults.ZONE_UNDEFINED, "");
            diagnoses.add(QCResults.ZONE_EXCEPTIONALLY_GOOD, "Excellent");
            diagnoses.add(QCResults.ZONE_BORDERLINE_EXCELLENT, "Borderline Excellent");
            diagnoses.add(QCResults.ZONE_NORMAL, "Normal");
            diagnoses.add(QCResults.ZONE_BORDERLINE_BAD, "Borderline Bad");
            diagnoses.add(QCResults.ZONE_BAD, "Bad");
            
            this.setColumnIdentifiers(colIDs);
            
            String[] groups = results.getGroupNames();
            
            //fill the table model with data from the QC results
            for (int i = 0 ; i < groups.length ; i++) {
                String[] colNames = results.getCols(groups[i]);
                for (int j = 0 ; j < colNames.length ; j++) {
                    Object[] obs = new Object[5];
                    
                    String colName = colNames[j];
                    double value = results.getValue(colName);
                    int goodbad = results.getGoodBadValue(colName);
                    
                    obs[0] = colName;
                    obs[1] = groups[i];
                    obs[2] = new Double(value);
                    obs[3] = new Double(Math.log(value)/ Math.log(2.0));
                    obs[4] = diagnoses.get(goodbad);
                    this.addRow(obs);
                }
            }
        }
    }
}
