/*
 * StudentTTestCalculator.java
 *
 * Created on 19 July 2003, 14:06
 */

/**
 *
 * @author  helen
 */
public class StudentTTestCalculator {
    private double[] m_tValue;
    private double[] m_degFreedom;
    private double[] m_meanA;
    private double[] m_meanB;
    private double[] m_varA;
    private double[] m_varB;
    private double[] m_meanDiff;
    
    public static int TTEST_TYPE_STANDARD_VARSAME = 1;
    public static int TTEST_TYPE_VARSDIFFER = 2;
    
    public static int ACTION_ON_NAN_NAN = 100;
    public static int ACTION_ON_NAN_USE_OTHERS = 200;
//    public static int TTEST_TYPE_MANN_WHITNEY = 3;
    
    /** Creates a new instance of StudentTTestCalculator */
    public StudentTTestCalculator() {
    }
    
    public double[] getTValues() {
        return m_tValue;
    }
    
    public double[] getDegsFreedom() {
        return m_degFreedom;
    }
    
    public double[] getMeansA() {
        return m_meanA;
    }
    
    public double[] getMeansB() {
        return m_meanB;
    }
    
    public double[] getMeanDiffs() {
        return m_meanDiff;
    }
    
    public double[] getVarA() {
        return m_varA;
    }
    
    public double[] getVarB() {
        return m_varB;
    }
    
    private void resetValuesToNull() {
        m_degFreedom = null;
        m_meanDiff = null;
        m_meanA = null;
        m_meanB = null;
        m_varA = null;
        m_degFreedom = null;
        m_tValue = null;
    }
    
    private void setNaN(int i) {
        m_degFreedom[i] = Double.NaN;
        m_tValue[i] = Double.NaN;
        m_meanB[i] = Double.NaN;
        m_meanA[i] = Double.NaN;
        m_meanDiff[i] = Double.NaN;
        m_varA[i] = Double.NaN;
        m_varB[i] = Double.NaN;
    }
    
    private void resetValues(int len) {
        m_degFreedom = new double[len];
        m_meanDiff = new double[len];
        m_meanA = new double[len];
        m_meanB = new double[len];
        m_varA = new double[len];
        m_varB = new double[len];
        m_tValue = new double[len];
    }
    
   
    
 
     
     
    
    public boolean doTTest(double[][] dataSetA, double[][] dataSetB, boolean[] filter, int type, int actionOnNaN) {
        boolean success = false;
        
        if (dataSetA == null || dataSetB == null ) {
            resetValuesToNull();
            return false;
        }
        
        
        double[] ttestResultSet;
        int numSpots = dataSetA[0].length;       
        int lenA = dataSetA.length;
        int lenB = dataSetB.length;
        double[] setA = new double[lenA];
        double[] setB = new double[lenB];
        
       resetValues(numSpots);
        
//        double[] answer = new double[numSpots];
        boolean doFiltering = true;
        if (filter == null) {
            doFiltering = false;
        }
        
        
        for (int i = 0 ; i < numSpots ; i++) {
            if ((!doFiltering) || !filter[i]) {
            //fill setA, setB
            for (int j = 0 ; j < lenA; j++) {
                setA[j]=dataSetA[j][i];
            }
            for (int j = 0 ; j < lenB; j++) {
                setB[j]=dataSetB[j][i];
            }
            ttestResultSet = ttest(setA, setB, type, actionOnNaN);
            m_tValue[i] = ttestResultSet[0];
            m_degFreedom[i] = ttestResultSet[1];
            m_meanA[i] = ttestResultSet[2];
            m_meanB[i] = ttestResultSet[3];
            m_varA[i] = ttestResultSet[4];
          
            m_varB[i] = ttestResultSet[5];
         
            m_meanDiff[i] = ttestResultSet[6]; 
            
            } else {
                setNaN(i);
            }
        }
        return true;
    }
    
  //  public boolean doTTest(double[][] dataSetA, double[][] dataSetB) {
    //    return doTTest(dataSetA, dataSetB, null);
    //}
    
    private double[] NaNs(int i) {
        double[] answer = new double[i];
        for (int j =0; j < i; j++) {
            answer[j] = Double.NaN;
        }
        return answer;
    }
    
    private static double[] ttest(double[] setA, double[] setB) {
        return ttest(setA, setB, TTEST_TYPE_STANDARD_VARSAME);
    }
    
    private static double[] ttest(double[] setA, double[] setB, int type, int actionOnNaN) {
        
        if (actionOnNaN == ACTION_ON_NAN_NAN) {
            return ttest(setA, setB, type) ;
        } else if (actionOnNaN == ACTION_ON_NAN_USE_OTHERS) {
            return ttest(stripNaNsAndInfty(setA), stripNaNsAndInfty(setB), type);
        } else { //unknown type
        return null;
        }
    }
    
    private static double[] stripNaNsAndInfty(double[] set) {
        double[] newSet;
        double[] answer;
        int lenSet;
        int lenNewSet = 0;
        if (set == null) {
            return null;
        }
        if (set.length == 0) {
            return new double[0];
        }
        lenSet = set.length;
        newSet = new double[lenSet];
        for (int i = 0 ; i < lenSet ; i++) {
            if (Double.isInfinite(set[i]) || Double.isNaN(set[i])) {
                //reject
            } else {
                newSet[lenNewSet] = set[i];
                lenNewSet++;
            }
        }
        answer = new double[lenNewSet];
        System.arraycopy(newSet, 0, answer, 0, lenNewSet);
        return answer;
    }
    
    
    private static double[] ttest(double[] setA, double[] setB, int type) {
        double meanA = mean(setA);
        
        double meanB = mean(setB);

        double varEstA = varEst(setA);
       
        double varEstB = varEst(setB);
        
        double lenA =  (double) setA.length;
 
        double lenB = (double) setB.length;
 
        double meanDiff = meanA - meanB;
        //System.out.println("meanDiff " + meanDiff);
        double sterrOfDiff;
        double ttest_result;
        if (type == TTEST_TYPE_STANDARD_VARSAME) {
        /*
         *OK, this is the formula we are using: its the t test with the assumption that
         *the true variance of each of the the two sets of samples is the same
         */
        sterrOfDiff = Math.sqrt( (lenA+ lenB)/ (lenA*lenB) * (sumOfSqs(setA) + sumOfSqs(setB)) / (lenA+lenB-2));
        ttest_result =  meanDiff/sterrOfDiff;
        //The following line is just an alternative way of writing the same thing
        // double sterrOfDiff = Math.sqrt((lenA+lenB)/(lenA*lenB)*(((lenA-1)*varEstA)+((lenB-1)*varEstB))/(lenB+lenA-2));
        //System.out.println("sterrOfDiff " + sterrOfDiff);
        } else if (type == TTEST_TYPE_VARSDIFFER) {
        /*
         *The following formula, commented out, is the t test if we were not
         *assuming that the two samples have the same variation.
         *We are NOT using that version: an added complication would be that
         *the number of degrees of freedom would also need to be calculated and
         *reported
         */
        sterrOfDiff = Math.sqrt((varEstA/lenA) + (varEstB/lenB));
        ttest_result =  meanDiff/sterrOfDiff;
        } else {//type unknown
            ttest_result =  Double.NaN;
        }
        double degsFre;
        if (type == TTEST_TYPE_STANDARD_VARSAME) {
            degsFre = (double) (lenA + lenB-2);
        } else if (type == TTEST_TYPE_VARSDIFFER) {
            degsFre = degsFree(varEstA, varEstB, lenA, lenB);
        } else {
            degsFre = Double.NaN;
        }
        double[] answer = new double[]{ttest_result, degsFre, meanA, meanB, varEstA, varEstB, meanDiff };
        return answer;
    }
    
    private static double degsFree(double varA, double varB, double lenA, double lenB) {
        double u = (varA/lenA) / (varA/lenA + varB/lenB);
        double df = 1.0/(u*u/(lenA-1.0)+ (1.0-u)*(1.0-u)/(lenB-1.0));
        return df;
    }
    
    private static double sum(double[] set) {
        if (set == null) {
            return Double.NaN;
        }
        int len = set.length;
        double sum = 0;
        for (int i = 0 ; i < len; i++) {
            if (Double.isNaN(set[i])) {
                sum = Double.NaN;
                break;
            }
            sum = sum + set[i];
        }
        return sum;
    }
    

    
    private static double mean(double[] set) {
        if (set == null || set.length == 0) {
            return Double.NaN;
        }
        int len = set.length;
        return sum(set)/len;
    }
    
    private static double varEst(double[] set) {
        double sumOfSquares = sumOfSqs(set);
        if (Double.isNaN(sumOfSquares) || Double.isInfinite(sumOfSquares)) {
            return Double.NaN;
        }
        
        int len = set.length;
        if (len == 0) {
            return Double.NaN;
        }
        if (len ==1) {
            return 0;
        }
       
        return sumOfSquares/(len-1);
    }
    
    private static double sumOfSqs(double[] set) {
        if (set == null) {
            return Double.NaN;
        }
        int len = set.length;
        if (len == 0) {
            return Double.NaN;
        }
        double mean = mean(set);
        if (Double.isInfinite(mean) || Double.isNaN(mean)) {
            return Double.NaN;
        }
        double sumOfSquares = 0;
        for ( int i = 0 ; i < len ; i++) {
            sumOfSquares = sumOfSquares+((set[i]-mean) * (set[i]-mean));
        }
        return sumOfSquares;
    }
    
    public static void main (String[] args) {
        /*lets just test if it works:*/
        StudentTTestCalculator calculator = new StudentTTestCalculator();
        
        double[] setA = new double[]{ 6,2,6,3,4,7,8,9,2,4,3,5,6,7,2,3,4,5,6,3,4,5};
        double[] setB = new double[]{6,4,5,2,6,7,8,3,5,43,32,8,4,6,4,5,23};
        System.out.println("StudentTtest: " + calculator.ttest(setA,setB));
    }
/*
 *checks:
 *meanA = 4.7272727272727275
 *meanB = 10.058823529411764
 *varA = 3.9220779220779214
 *varB = 131.43382352941174
 *lenA = 22.0
 *lenB = 17.0
 *Tscore = -2.1483
 *degFre = 37.0
 *One tailed P = 0.0192
 *Two tailed P = 0.0384
 */
 }