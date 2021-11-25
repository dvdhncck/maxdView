/*
 * QCResults.java
 *
 * Created on 14 June 2004, 15:21
 */
import java.util.*;
/**
 *Results object.
 *Created and manipulated by the QCCalculator.
 *This object can then be queried by e.g. a QCDisplayPanel in 
 *order to display the results contained
 * @author  hhulme
 */
public class QCResults {
    
    public static int ZONE_UNDEFINED = 0;
    public static int ZONE_EXCEPTIONALLY_GOOD = 1;
    public static int ZONE_BORDERLINE_EXCELLENT = 2;
    public static int ZONE_NORMAL = 3;
    public static int ZONE_BORDERLINE_BAD = 4;
    public static int ZONE_BAD = 5;
    
    private QCControlLineSet m_primaryControlLineSet;
    
    private QCControlLineSet[] m_otherControlLineSets;
    
    /**List of groups - in the order specified by the user*/
    private Vector m_groupCols;
    
    /**Key is group, value is vector containing col names*/
    private HashMap m_groupsColsHash;
     
    /**Key is Col, value is value of variance of diff from group mean, once calculated*/
    private HashMap m_colsValsHash;
    
    /**Key is col, value is log of var of diff from group mean, once calculated*/
    private HashMap m_colLogValsHash;
    
    /**Key is col, value is int recording value according to whether the col is V Good, Good, Iffy, Bad
     *based on the primary sig value
     */
    private HashMap m_colGoodBadHash;
    
    /**Key is col, val is QCInput, which in turn contains data*/
    private HashMap m_colsInputsHash;
    
    /**Mean of all logged variances once calculated*/
    private double m_meanOfLoggedVariances;
    
    /**Var of all logged variances once calculated*/
    private double m_varOfLoggedVariances;
    
    /**Stdev of all logged variances once calculated*/
    private double m_stdevOfLoggedVariances;
    
    /**Number of rows which were bad and not included in the calculation*/
    private int m_numBadRows;
    
    /**Main Sig level on basis of which slides will be classified*/
    private double m_primary_sig_level;
    
    /** Creates a new instance of QCResults */
    public QCResults() {
     
        //create the hashmaps for storage
        m_groupCols = new Vector();
        m_groupsColsHash = new HashMap();
        m_colsValsHash = new HashMap();
        m_colsInputsHash = new HashMap();
        m_colLogValsHash = new HashMap();
        m_colGoodBadHash = new HashMap();
    }
    
    /**Retures String[] containing a list of group names with measurements*/
    public String[] getGroupNames() {
        int len = m_groupCols.size();
        String[] answer = new String[len];
        for (int i = 0 ; i < len ; i++) {
            answer[i] = (String) m_groupCols.get(i);
        }
        
        /*
        Set set = m_groupsColsHash.keySet();
        int len = set.size();
        String[] answer = new String[len];
        Iterator it = set.iterator();
        int i = 0;
        while (it.hasNext()) {
            
            answer[i] = (String) it.next();
            i++;
        }
         */
        return answer;
    }
    
    /**Given a group name, returns a list of measurement names within that replicate group*/
    public String[] getCols(String group) {
        Vector vec = (Vector) m_groupsColsHash.get(group);
        int len = vec.size();
        String[] answer = new String[len];
        for (int i = 0 ; i < len ; i++) {
          answer[i] = (String) vec.elementAt(i);   
        }
        return answer;
    }
    
    /**Add a QCInput ready for the calculation*/
    protected void addInput(QCInput input) {
        String group = input.getGroup();
        String column = input.getColName();
   
        if (!m_groupsColsHash.containsKey(group)) {
            m_groupsColsHash.put(group, new Vector());
            m_groupCols.add(group);
        }
        Vector vec = (Vector) m_groupsColsHash.get(group);
        if (!vec.contains(column)) {
            vec.add(column);
        }
        m_colsInputsHash.put(column, input);
    }
    
    /**Get the variance of the diff from the mean (once calculated*/
    public double getValue(String colName) {
        return ((Double) m_colsValsHash.get(colName)).doubleValue();
    }
    
     /**Get the log (2) of the variance of the diff from the mean (once calculated*/
    public double getLoggedValue(String colName) {
        return ((Double) m_colLogValsHash.get(colName)).doubleValue();
    }
    
    public double[] getAllLoggedValues() {
        int len = m_colLogValsHash.size();
        Collection vals = m_colLogValsHash.values();
        double[] allLoggedVals = new double[len];
        int counter = 0;
        Iterator it = vals.iterator();
        while (it.hasNext()) {
            allLoggedVals[counter] = ((Double) it.next()).doubleValue();
            counter++;
        }
        return allLoggedVals;
    }
    
    protected void updateGoodBad() {
        Iterator it = m_colLogValsHash.keySet().iterator();
        double lcl_bottom = m_primaryControlLineSet.confidenceBarLCL[0];
        double lcl = m_primaryControlLineSet.lowerControlLine;
        double lcl_top = m_primaryControlLineSet.confidenceBarLCL[1];
        
        double ucl_bottom = m_primaryControlLineSet.confidenceBarUCL[0];
        double ucl = m_primaryControlLineSet.upperControlLine;
        double ucl_top = m_primaryControlLineSet.confidenceBarUCL[1];
        
        while (it.hasNext()) {
            String col = (String) it.next();
            double val = getLoggedValue(col);
            if (val <= lcl_bottom) {
                m_colGoodBadHash.put(col, new Integer(this.ZONE_EXCEPTIONALLY_GOOD));
            } else if (val <= lcl_top) {
                m_colGoodBadHash.put(col, new Integer(this.ZONE_BORDERLINE_EXCELLENT));
            } else if (val <=ucl_bottom) {
                m_colGoodBadHash.put(col, new Integer(this.ZONE_NORMAL));
            } else if (val <=ucl_top) {
                m_colGoodBadHash.put(col, new Integer(this.ZONE_BORDERLINE_BAD));
            } else {
                m_colGoodBadHash.put(col, new Integer(this.ZONE_BAD));
            }
            
        }
    }
    
    public int getGoodBadValue(String colName) {
        return ((Integer) m_colGoodBadHash.get(colName)).intValue();
    }
    
    
    /**Set the var of the diff from the mean - 
     *should only really be done by the QCCalculator*/
    protected void setValue(String colName, double db) {
        m_colsValsHash.put(colName, new Double(db));
        m_colLogValsHash.put(colName, new Double(Math.log(db)/ Math.log(2.0)));
    }
    
    /**Get the data for the measurement*/
    public double[] getData(String colName) {
        return ((QCInput) m_colsInputsHash.get(colName)).getData();
    }
    
    public double[][] getData(String[] colNames) {
        double[][] data = new double[colNames.length][];
        for (int i = 0 ; i < colNames.length ; i ++) {
            data[i] = getData(colNames[i]);
        }
        return data;
    }
    
    public double[][] getAllData() {
        int size = m_colsInputsHash.size();
        double[][] data = new double[size][];
        String[] groups = getGroupNames();
        int counter = 0;
        for (int i = 0 ; i < groups.length; i++) {
            String[] cols = getCols(groups[i]);
            for (int j = 0 ; j < cols.length; j++) {
                data[counter] = getData(cols[j]);
                counter++;
            }
        }
        return data;
    }
    
    /**Get the original QC Input for that measuremtne*/
    public QCInput getInput(String colName) {
        return (QCInput) m_colsInputsHash.get(colName);
    }
    
    /**Set the mean of logged vars - this is done by QCCalculator*/
    protected void setMeanOfLoggedVars(double db) {    
        m_meanOfLoggedVariances = db;
    }
    
    /**Get mean of logged vars if calculated*/
    public double getMeanOfLoggedVars() {
        return m_meanOfLoggedVariances;
    }
    
    /**Set var of logged vars - done by QCCalculator*/
    protected void setVarOfLoggedVars(double db) {
        m_varOfLoggedVariances = db;
        m_stdevOfLoggedVariances = Math.sqrt(db);
    }
    
    /**Get the var of logged vars if calculated*/
    public double getVarOfLoggedVars() {
        return m_varOfLoggedVariances;
    }
    
    /**Get the stdev of the vars if calculated*/
    public double getStdevOfLoggedVars() {
        return m_stdevOfLoggedVariances;
    }
    
    /**Set the number of bad rows*/
    protected void setNumBadRows(int in) {
        m_numBadRows=in;
    }
    
    /**get the number of bad rows*/
    public int getNumbadRows() {
        return m_numBadRows;
    }
    
    protected void setPrimaryControlLineSet(QCControlLineSet set) {
        m_primaryControlLineSet = set;
    }
    
    protected void setOtherControlLineSets(QCControlLineSet[] sets) {
        m_otherControlLineSets = sets;
    }
    
    public QCControlLineSet getPrimaryControlLineSet() {
        return m_primaryControlLineSet;
    }
    
    public QCControlLineSet[] getOtherControlLineSets() {
        return m_otherControlLineSets;
    }
    
    
}
