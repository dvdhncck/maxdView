/*
 * TTestType.java
 *
 * Created on 31 July 2003, 13:03
 */

/**
 *
 * @author  hhulme
 */
public class TTestType {
    private int m_identifier;
    private String m_name;
    private String m_shortDesc;
    private String m_longDesc;
    
    /** Creates a new instance of TTestType */
    public TTestType(int identifier, String name, String shortDesc, String longDesc) {
        m_identifier = identifier;
        m_name = name;
        m_shortDesc = shortDesc;
        m_longDesc = longDesc;
    }
    
    public int getIdentifier() {
        return m_identifier;
    }
    
    public String getName() {
        return m_name;
    }
    
    public String getShortDescription() {
        return m_shortDesc;
    }
    
    public String getLongDescription() {
        return m_longDesc;
    }
    
    public String toString() {
        return m_name;
    }
        
    
}
