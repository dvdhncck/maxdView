/*
 * StatCalculator.java
 *
 * Created on 21 July 2003, 17:12
 */

/**
 *This calculator is used to convert t values from the students t distribution 
 * to P values.
 *It relies on classes developed at CERN to implement the various stats distribution functions.
 * @author  hhulme
 */
public class PvalueCalculator {
    
    /**if the number of degrees of freedom is high, then the t distribution is well
     *approximated by the normal distribution. For values above this CUTOFF
     *the normal dist will be used
     */
    public static double CUTOFF= 10000000.0;
    //just for debugging checks:
    //public static double CUTOFF=30;
    
    /** Creates a new instance of PvalueCalculator */
    public PvalueCalculator() {
    }
    
    public static double[] pvalueOneTail(double[] ts, double df) {
        if (ts == null) {
            return null;
        }
        int len = ts.length;
        double[] answer = new double[len];
        for (int i = 0 ; i < len ; i++) {
            answer[i] = pvalueOneTail(ts[i], df);
        }
        return answer;
    }
    
    public static double[] pvalueOneTail(double[] ts, double[] dfs) {
        if (ts == null) {
            return null;
        }
        int len = ts.length;
        if (dfs== null || dfs.length!= len) {
            System.out.println("[PvalueCalculator] warning, degrees of freedon not specified or wrong size");
            return null;
        }
        double[] answer = new double[len];
        for (int i = 0 ; i < len ; i++) {
            answer[i] = pvalueOneTail(ts[i], dfs[i]);
        }
        return answer;
    }
    
    public static double[] pvalueTwoTail(double[] ts, double df) {
        if (ts == null) {
            return null;
        }
        int len = ts.length;
        double[] answer = new double[len];
        for (int i = 0 ; i < len ; i++) {
            answer[i] = pvalueTwoTail(ts[i], df);
        }
        return answer;
    }
    
    public static double[] pvalueTwoTail(double[] ts, double[] dfs) {
        if (ts == null) {
            return null;
        }
        int len = ts.length;
        if (dfs== null || dfs.length!= len) {
            System.out.println("[PvalueCalculator] warning, degrees of freedon not specified or wrong size");
            return null;
        }
        double[] answer = new double[len];
        for (int i = 0 ; i < len ; i++) {
            answer[i] = pvalueTwoTail(ts[i], dfs[i]);
        }
        return answer;
    }
    
    public static double pvalueOneTail(double t, double df) {
        return 1.0-cumulativeTdist(t, df);
    }
    
    public static double pvalueTwoTail(double t, double df) {
        if (t<0) {
            return 2.0*cumulativeTdist(t,df);
        } else {
            return (2.0*(1.0-cumulativeTdist(t,df)));
        }
    }
    
    /**
     *This converts t statistic  to a P-value
     *@arg default degrees of freedom of the t distribution
     *This is P(x<t | x from t dist with n df)
     */
    public static double cumulativeTdist(double t, double df) {
        //some cases are simpler than having to do the actual formula:
        //do them first:
        if (Double.isNaN(t) || Double.isNaN(df) || df<=0.0) {
            return Double.NaN;
        }
        
        if (Double.isInfinite(t)) {
            return 0.0;
        }
        
        if (t==0.0) {
            return 0.5;
        }
        
        
        
        if (df==1.0) {
            return (0.5 + (Math.atan(t)/Math.PI));
        }
        
    
        if (df>=CUTOFF) {
            return cern.jet.stat.Probability.normal(t);
        }
     
        
        //return (1.0-0.5*(regBeta(df/(df+(t*t)), df*0.5, 0.5)));
        double a=df*0.5;
        double b= 0.5;
        double z = df/(df + (t*t));
        
        //this is the actual formula:
        double symmetricValue = 0.5*cern.jet.stat.Gamma.incompleteBeta(a,b,z);
        if (t<0) {
            return symmetricValue;
        } else {
            return 1.0-symmetricValue;
        }
    }
    
    
    /*regularized beta function
     */
    /*
    public static double regBeta(double z, double a, double b) {
        //return (incompleteBeta(z,a,b)/beta(a,b));
        double numerator = cern.jet.stat.Gamma.incompleteBeta(a,b,z);
        double denominator = cern.jet.stat.Gamma.beta(a,b);
        return numerator/denominator;
    }
     */
    
    public static void main(String[] args) {
        /*OK, lets "test" this class.
        *going to check for various values if our cumulative ttest agrees with
         *matlab and excel for a variety of values.
         *For matlab, we use formulae for 1 tailed t value:
         *1-tcdf if t positive, tcdf if t is neg
         *t dv  matlabsays excelsays
         *0.4 1 0.3789 0.378881058

         *65 1 0.0049 0.004896689

         *6.4 1 0.0493 0.049336995

         *0 2 0.5000 0.5

         *5 3 0.0077 0.007696219

         *54 4 3.5201e-007 3.52009E-07

         *1.2 10 0.1289 0.12889815

         *-7 3 0.0030 #NUM!

         *3 38.5 0.0024 0.002373166

         *
         */

        
        double[] tvalues = {0.4, 65, 6.4, 0.0, 5.0, 54.0, 1.2, -7.0, 3.0, -4, -4, 4, 1.2, -1.2, 3, -3};
        double[] degsfre = {1,1,1,2,3,4,10,3,38.5, 6, 21, 21,1,1, 40, 40};
        for (int i = 0 ; i < 16 ; i++) {
            System.out.println("tvalue "+ tvalues[i] + " degfree " + degsfre[i] + " one tailed p " + PvalueCalculator.pvalueOneTail(tvalues[i], degsfre[i]));
            System.out.println("tvalue "+ tvalues[i] + " degfree " + degsfre[i] + " two tailed p " + PvalueCalculator.pvalueTwoTail(tvalues[i], degsfre[i]) + "\n");
        }
         
        System.out.println("Testing the incomplete beta function");
        
        System.out.println("0.5 0.6 0.7 "+ cern.jet.stat.Gamma.incompleteBeta(0.5,0.6,0.7)); 
        
        System.out.println("3 2 0.5 "+ cern.jet.stat.Gamma.incompleteBeta(3,2,0.5));
       
    }
}
