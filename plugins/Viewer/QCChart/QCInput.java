/*
 * QCInput.java
 *
 * Created on 14 June 2004, 15:21
 */

/**
 * Struct like object for storing data needed to calculate QC Results
 * @author  hhulme
 */
public class QCInput {
    
    //data to carry out calc on
    private double[] m_data;
    
    //indicates whether the data is ordinary or refers to a dye flip
    //If dye flip, the negative of the data is taken before the calc is carried out
    private boolean m_isForward;
    
    //different groups for different conditions
    private String m_group;
    
    //Name of the measurement
    private String m_colName;
    
    /** Creates a new instance of QCInput */
    public QCInput(String colName, double[] data, boolean isForward, String group) {
        setData(data);
        setIsForward(isForward);
        setGroup(group);
        setColName(colName);
    }
    
    /**Set col name*/
    public void setColName(String colName) {
        m_colName = colName;
    }
    
    /**Get Col Name*/
    public String getColName() {
        return m_colName;
    }
    
    /**Set data*/
    public void setData(double[] data) {
        m_data = data;
    }
    
    /**Get Data*/
    public double[] getData() {
        return m_data;
    }
    
    /**Set IsForward*/
    public void setIsForward(boolean isForward) {
        m_isForward = isForward;
    }
    
    /**Get isForward*/
    public boolean isForward() {
        return m_isForward;
    }
    
    /**Set Group*/
    public void setGroup(String group) {
        m_group = group;
    }
    
    /**Get Group*/
    public String getGroup() {
        return m_group;
    }
    
}
