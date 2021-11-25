/*
 * QCControlLineSet.java
 *This is a struct for keeping info about a set of control lines.
 *Its just a convenience class.
 * Created on 26 January 2005, 14:44
 */



/**
 *
 * @author  hhulme
 */
public class QCControlLineSet {

    /**This is the sig level*/
    public double sigLevel;
    
    public double sigLevel_mu;
    
    public double sigLevel_sigma;
    
    /**This is Phi^(-1) of the sig level, 
     *where Phi is the c.d.f (Cumulative Density Function) of the standard normal dist.
     */
    public double width;
    
    public double centerLine;
    
    public double upperControlLine;
    
    public double lowerControlLine;
    
    public double[] confidenceBarUCL;
    
    public double[] confidenceBarCentreLine;
    
    public double[] confidenceBarLCL;
    
    
    
    
    /** Creates a new instance of QCControlLineSet */
    public QCControlLineSet() {
    }
    
}
