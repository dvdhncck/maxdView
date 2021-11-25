/**
 * QCChartCalculator.java
 *
 * Created on 14 June 2004, 15:19
 *
 * Calculator which calculates QC control results
 * @author  hhulme
 */

import sun.misc.*;
import java.util.*;

public class QCChartCalculator {
    
    public static int USE_MEDIAN_FOR_CALCULATION=0;
    public static int USE_MEAN_FOR_CALCULATION=1;
    
    /** Creates a new instance of QCChartCalculator */
    public QCChartCalculator() {
    }
    
    /**Given some measurements as input, + filter info, calculates the
     *results and returnes them as a results object which can then be queried to retrieve the results
     *@param input is a set of QCInputs, which may have been created by a QCChart object
     *@param filterInfo is an array of boolean values. The value true indicating that that row has been
     *filtered out and should be ignored whem carrying out the calculation.
     *@param primary_sigLevel is the significance level w.r.t. which the control lines will be calculated
     *@param confidence_siglevel_mu confidence level of mean estimate, used to calculate the confidence interval for the CL
     *@param confidence_siglevel_sigma confidence level of var estimate, used to calculate the confidence interval for the UCL and LCL
     *return results object.
     */
    public QCResults calculateQCChart(QCInput[] input, boolean[] filterInfo, double primary_sigLevel, double confidence_siglevel_mu, double confidence_siglevel_sigma, int useMeanOrMedian) {        
        QCResults results = new QCResults();
        
        //fill with the inputs
        for (int i = 0 ; i < input.length ; i++) {
            results.addInput(input[i]);
        }
        
        //reorganise by group
        String[] groupNames = results.getGroupNames();
        
        //detect bad rows not already filtered out and create new filter accordingly
        double[][] data = results.getAllData();
        boolean[] badRows = detectBadRow(data);
        
        int numbadRowsToReport = 0;
        
        boolean[] newFilter = new boolean[filterInfo.length];
        for (int i = 0 ; i < filterInfo.length; i++) {
            newFilter[i] = (filterInfo[i] || badRows[i]);
            //if the row is bad and has not already been filtered out, report it
            if (badRows[i] && (!filterInfo[i])) {
                numbadRowsToReport++;
            }
        }
        results.setNumBadRows(numbadRowsToReport);
        
        
        //calculate groupwise
        for (int i = 0 ; i < groupNames.length ; i++) {
            String[] colNames = results.getCols(groupNames[i]);
            
            //calculates for this subset, and adds the answer to the results
            doCalculation(results, colNames, newFilter, useMeanOrMedian);
            
            
            
        }
        //
        //calculate the mean variance and stddev of the log 2 variances
        //using all groups together
        double[] log2vars = results.getAllLoggedValues(); 
        
        int n = log2vars.length;
        
        //record these values in the results object
        double meanOfLoggedVars = mean(log2vars);
        results.setMeanOfLoggedVars(meanOfLoggedVars);
        
        double varEstOfLoggedVars = varEst(log2vars);
        results.setVarOfLoggedVars(varEstOfLoggedVars);
        
        double stdevEst = Math.sqrt(varEstOfLoggedVars);
        
        //make the primary Control Line Set.
        QCControlLineSet primaryControlLineSet = new QCControlLineSet();
        primaryControlLineSet.sigLevel = primary_sigLevel;
        primaryControlLineSet.sigLevel_mu = confidence_siglevel_mu;
        primaryControlLineSet.sigLevel_sigma = confidence_siglevel_sigma;
        primaryControlLineSet.width = cern.jet.stat.Probability.normalInverse(1-primary_sigLevel/2);
        primaryControlLineSet.centerLine = meanOfLoggedVars;
        primaryControlLineSet.upperControlLine = upperControlLine(meanOfLoggedVars, stdevEst, primary_sigLevel);
        primaryControlLineSet.lowerControlLine = lowerControlLine(meanOfLoggedVars, stdevEst, primary_sigLevel);
        primaryControlLineSet.confidenceBarCentreLine = centerLineConfidenceInterval(meanOfLoggedVars, stdevEst, confidence_siglevel_mu, n);
        primaryControlLineSet.confidenceBarLCL = lowerConfidenceInterval(meanOfLoggedVars, stdevEst, primary_sigLevel, confidence_siglevel_sigma, n);
        primaryControlLineSet.confidenceBarUCL = upperConfidenceInterval(meanOfLoggedVars, stdevEst, primary_sigLevel, confidence_siglevel_sigma, n);
        
        
        //add the control line set to the results
        results.setPrimaryControlLineSet(primaryControlLineSet);
             
        //based on the results, each slide will be classified as good bad etc. update this
        //info to take into account the calculated control lines
        results.updateGoodBad();
        //done
        
        return results;
    }
    
    /**
     *@param filter info is a boolean[] where the value is true for those vals which should be filtered out
     *and false for those which should be kept
     *@param Input is an m X n data set. 
     *@return an m' X n data set where m' <= m. (less by the number of rows filtered out)
     *with the indicated rows removed.
     */
    public static double[][] filterData(double[][] data, boolean[] filterInfo) {
        //fiter on == true
        //filter off == false
        
        int numCols = data.length;
        int numdatapoints = data[0].length;
        
        
        //see how many of the datapoints should remain unfiltered
        int numUnfilteredDatapoints = 0;
        for (int i = 0 ; i < numdatapoints ; i++) {
            if (!filterInfo[i]) {
                numUnfilteredDatapoints++;
            }
        }
        
        //create a new data set of the correct size
        double[][] filteredData = new double[numCols][numUnfilteredDatapoints];
        
        //fill the dataset appropriately
        int counter = 0;
        for (int i =0 ; i < numdatapoints ; i++) {
            if (!filterInfo[i]) {
                for (int j = 0 ; j < numCols ; j++) {
                    filteredData[j][counter] = data[j][i];
                }
                counter++;
            }
        }
        return filteredData;
        
    }
    
    /**
     *Detects rows which contain NaN or +-Infinity
     *@param data an m x n array of data
     *@return  a boolean[] with value true = badrow, false= not bad row
     *
     */
         private  boolean[] detectBadRow(double[][] data) {
         int numCols = data.length;
         int numDatapoints = data[0].length;
         
         boolean[] badRow = new boolean[numDatapoints];
        int badRowCounter = 0;
        
        for (int i = 0 ; i < numDatapoints ; i++) {
            badRow[i] = false;
            for (int j = 0 ; j < numCols ; j++) {
                if (Double.isInfinite(data[j][i]) || Double.isNaN(data[j][i]))  {
                    
                    badRow[i] = true;
                    badRowCounter++;
                    break;
                }
            }
        }
        return badRow;
         }
    
    /**
     *Calculate the QC chart results and add them to the results object
     *
     */
    private void doCalculation(QCResults results, String[] colNames, boolean [] filterInfo, int useMeanOrMedian) {
        if (colNames.length < 1) {
            return;
        }
        
        //work out how many columns (slides) there are
        int numCols = colNames.length;
        
        //work out how many spots there are on each slide
        int numDatapoints = results.getData(colNames[0]).length;
        
        //set up the data matrix
        double[][] data = new double[numCols][numDatapoints];
        
        //fill the data matrix
        for (int i = 0 ; i < numCols ; i++) {
            QCInput input = results.getInput(colNames[i]);
            if (input.isForward()) {
                data[i] = results.getData(colNames[i]);
            } else {
                for (int j = 0 ; j < numDatapoints ; j++) {
                    data[i][j] = - results.getData(colNames[i])[j];
                }
            }
        }
        
        //apply the filter
        data = filterData(data, filterInfo);
        
        //perform the calculation
        double[] answer = qcValues(data, useMeanOrMedian);
        
        //record values in the results object
        for (int i = 0 ; i < numCols ; i++) {
            results.setValue(colNames[i], answer[i]);
        }
    }
    
    /**
     *Calculte the qc values of the data provided.
     *@return double array. Each double corresponds to a column of the data
     *and is the aQC value for that slide.
     */
    public double[] qcValues(double[][] data, int useMeanOrMedian) {
        int numCols = data.length;
        int numDataPoints = data[0].length;
        double[] answer = new double[numCols];
        
        //calculate the averages (mean or median accordingly);
        double[] averages = new double[numDataPoints];
        for (int i = 0 ; i < numDataPoints ; i++) {
            double[] vals = new double[numCols];
            for (int j= 0 ; j < numCols ; j++) {
                vals[j] = data[j][i];
            }
            if (useMeanOrMedian == USE_MEAN_FOR_CALCULATION) {
            averages[i] = mean(vals);
            } else { //default == median
                averages[i] = median(vals);
            }
            
        }
        
        
        //for each of the colcumns, calculate the difference from the mean, and
        //calculate the variance of this
        
        for (int i = 0 ; i < numCols ; i++) {
            double[] diffs = new double[numDataPoints];
            for (int j = 0 ; j < numDataPoints ; j++) {
                diffs[j] = data[i][j] - averages[j];
            }
            answer[i] = varEst(diffs);
        }
        return answer;
        
    }
    
    /**utility function for summing stuff
     */
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
    
    
    /**utlily function for calculating mean
     */
    private static double mean(double[] set) {
        if (set == null || set.length == 0) {
            return Double.NaN;
        }
        int len = set.length;
        return sum(set)/len;
    }
    
    /**utility function for calculating median
     **/
    private static double median(double[] set) {
        if (set == null || set.length == 0) {
            return Double.NaN;
        }
        int len = set.length;
        double[] sorted = set;
        Arrays.sort(sorted);
        
        /*Note: if len is even, this results in halfway = len/2
         *but if len is odd, it gives, halfway = (len-1)/2
         *because of the way ints are handled in java
         */
        int  halfway = len/2;
        
        //System.out.println("len = " + len);
        //System.out.println("halfway = "+halfway);
        
        double answer=0;
        
        if (len == 2*halfway) {
            answer = (sorted[halfway]+sorted[(halfway-1)])/2.0;
        } else {
            answer = sorted[halfway];
        }
         
        return answer;
        
    }
    
    /**utlily function for calculating estimate of variance
     */
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
    
    
    /**utility function for calculating sum of squares
     */
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
    
    /**
     *returns the square root of ((n-1) / chi2inv(n-1, 1-alpha/2))
     *where chi2inv(v,p) is the inverse of the cumulative density function of the chi squared distribution with v degrees of freedom.
     *This function is often used when calculating confidence intervals
     */
    public static double K1(int n, double alpha) {
        double numerator = (double) (n-1);
        double denominator = chisquaredinverse((double) (n-1), (1.0-alpha/2.0)); 
        return Math.sqrt(numerator/denominator);
    }
    
    /**
     *returns the square root of ((n-1) / chi2inv(n-1, alpha/2))
     *where chi2inv(v,p) is the inverse of the cumulative density function of the chi squared distribution with v degrees of freedom.
     *This function is often used when calculating confidence intervals.
     */
    public static double K2(int n, double alpha) {
        double numerator = (double) (n-1);
        double denominator = chisquaredinverse((double) (n-1),  alpha/2.0);
        return Math.sqrt(numerator/denominator);
    }
    
    /**
     *Returns the Cumulative density function of the chi squared distrubution with n degrees of freedom.
     *This has been calculated using the formula:
     *chisquareCHF(n,x) = incompleteGamma(n/2, x/2)
     *where incompleteGamma(v, r) is the incomplete Gamma function with v degrees of freedom.
     */
    public static double chisquaredCDF(double n, double x) {
       // this is calculated in terms of incomplete gamma function
        return cern.jet.stat.Gamma.incompleteGamma(n/2, x/2);
    }
    
    /**
     *This is the inverse of the cumulativ density function of the chi squared distribution
     *with n degrees of freedom.
     *Note: this function does not calculate the result directly, 
     *but rather, uses numerical searching to find the inverse of 
     *chisquaredCDF
     */
    public static double chisquaredinverse(double n, double p) {

        //upper and lower start place to begin looking
        double lowerStart = p;
        double upperStart = n;
        
        //first of all, make sure start values really are either side of the hoped for value.
        //The cumulative density function is monotonic increasing, so we know that there is only one answer
 
        //make sure the lower start value is lower than our answer
        while (chisquaredCDF(n, lowerStart) > p) {
            lowerStart=lowerStart/2;
        }
        
        //make sure the upper start value is higher than our answer
        while (chisquaredCDF(n, upperStart) < p) {
            upperStart=2*upperStart;
        }
        
        //set the middle value to between the two
        double middleVal=(lowerStart+upperStart)/2;
        
        //value of chi squared cum dist function
        double chi2m;
               
        while (upperStart - lowerStart > 0.0000001) { // our desired accuracy
            middleVal = (lowerStart+upperStart)/2;
             chi2m = chisquaredCDF(n, middleVal);
            
             //halve our search area, picking the upper or lower half appropriately
             if (chi2m > p) {
                 upperStart = middleVal;
             } else {
                 lowerStart = middleVal;
             }
             
        }
        double answer = middleVal;
        return answer;
        
    }
    
    /**
     *This function just tells you the value you gave it. 
     *Its included for consistency with the other control line functions
     *@param meanEst the estimate of the mean of the sample
     */
    public static double centerLine(double meanEst) {
        return meanEst;
    }
    
    /**
     *Calculates the upper control line in terms of
     *@param meanEst the estimate of the mean of the sample
     *@param stdevEst the estimate of the standard deviation of the sample
     *alpha the desired significance level (e.g. 0.01).
     *The formula is
     *meanEst + Phi(1.0-alpha/2.0)* stdevEst
     *where Phi is the normal inverse function.
     */
    public static double upperControlLine(double meanEst, double stdevEst, double alpha) {
        return meanEst + cern.jet.stat.Probability.normalInverse(1.0-alpha/2.0)* stdevEst;
    }
    
    /**
     *Calculates the lower control line in terms of
     *@param meanEst the estimate of the mean of the sample
     *@param stdevEst the estimate of the standard deviation of the sample
     *alpha the desired significance level (e.g. 0.01)
     *The formula is
     *meanEst - Phi(1.0-alpha/2.0)* stdevEst
     *where Phi is the normal inverse function.
     */
    public static double lowerControlLine(double meanEst, double stdevEst, double alpha) {
        return meanEst - cern.jet.stat.Probability.normalInverse(1.0-alpha/2.0)* stdevEst;
    }
    
        /**
     *Calculates the Confidence interval around the Lower Control Line.
     *@param meanEst is the estimate of the mean of the sample.
     *@param stdevEst is the estimate of the standard deviation of the sample
     *@param alpha is the significance level chosen to calculate the control lines,
     *and should be small, e.g. 0.01, 0.05 or 0.0027
     *@param alpha_mu is the significance level of the variation of the mean 
     *and is used to calculate the confidence interval around the CL.
     *Typically value of about 0.2 would be sensible.
     *@param n is the number of slides.
     *@return an array of two doubles, these being the lower and upper values of the confidence range around
     *the Centre Line with respect to the parameters alpha and alpha_sigma provided
     */
    public static double[] centerLineConfidenceInterval(double meanEst, double stdevEst, double alpha_mu, int n) {
        double wd = cern.jet.stat.Probability.normalInverse(1.0 - alpha_mu/2.0);
        double[] answer =  {meanEst - wd*stdevEst/Math.sqrt((double) n),
                             meanEst + wd*stdevEst/Math.sqrt((double) n)   };
        return answer;
    }
    
        /**
     *Calculates the Confidence interval around the Upper Control Line.
     *@param meanEst is the estimate of the mean of the sample.
     *@param stdevEst is the estimate of the standard deviation of the sample
     *@param alpha is the significance level chosen to calculate the control lines,
     *and should be small, e.g. 0.01, 0.05 or 0.0027
     *@param alpha_sigma is the significance level of the variation of stdev chosen, 
     *and is used to calculate the confidence interval around the UCL.
     *Typically value of about 0.2 would be sensible.
     *@param n is the number of slides.
     *@return an array of two doubles, these being the lower and upper values of the confidence range around
     *the Upper Control Line with respect to the parameters alpha and alpha_sigma provided
     */
    public static double[] upperConfidenceInterval(double meanEst, double stdevEst, double alpha, double alpha_sigma, int n) {
        double wd = cern.jet.stat.Probability.normalInverse(1.0 - alpha/2.0);
        double[] answer = {meanEst + wd * K1(n, alpha_sigma) * stdevEst,
                                meanEst + wd * K2(n, alpha_sigma) * stdevEst };
        return answer;
    }
    
    /**
     *Calculates the Confidence interval around the Lower Control Line.
     *@param meanEst is the estimate of the mean of the sample.
     *@param stdevEst is the estimate of the standard deviation of the sample
     *@param alpha is the significance level chosen to calculate the control lines,
     *and should be small, e.g. 0.01, 0.05 or 0.0027
     *@param alpha_sigma is the significance level of the variation of stdev chosen, 
     *and is used to calculate the confidence interval around the LCL.
     *Typically value of about 0.2 would be sensible.
     *@param n is the number of slides.
     *@return an array of two doubles, these being the lower and upper values of the confidence range around
     *the Lower Control Line with respect to the parameters alpha and alpha_sigma provided
     */
    public static double[] lowerConfidenceInterval(double meanEst, double stdevEst, double alpha, double alpha_sigma, int n) {
        double wd = cern.jet.stat.Probability.normalInverse(1.0 - alpha/2.0);
        double[] answer = {
            meanEst - wd * K2(n, alpha_sigma) * stdevEst ,
            meanEst - wd * K1(n, alpha_sigma) * stdevEst
        };
        return answer;
    }
    
    public static void main (String[] args) {
        
        //for testing only - 
        
        System.out.println("testing if 'median' works");
        System.out.println("double[] things = { 1.0, 0.8, 0.8, 0.1, 1.1}");
        double[] things = {1.0, 0.8, 0.8, 0.1, 1.1};
        System.out.println("median = " + median(things));
        
         System.out.println("double[] things = {1.2, 1.0, 0.8, 0.8, 0.1, 1.1}");
        things = new double[]{1.2, 1.0, 0.8, 0.8, 0.1, 1.1};
        System.out.println("median = " + median(things));
        
                 System.out.println("double[] things = {1.2, 1.0, 0.8,0.1, 0.8, 0.1, 1.1}");
        things = new double[]{1.2, 1.0, 0.8, 0.1, 0.8, 0.1, 1.1};
        System.out.println("median = " + median(things));
        
        //to verify that the values given correspond to answers gotten from 
        //matlab, stats tables, and yonxiangs paper, in order to test that the
        //functions here are right.
        
        
        /*
        System.out.println("Try to reproduce the matlab result:");
        System.out.println("gaminv(0.4, 4, 6) ans = 19.2679");
        System.out.println("cern prob gamma (0.4, 6, 4) ");
        System.out.println(cern.jet.stat.Probability.gamma(0.4,6,4));
        
        System.out.println("cern prob gamma (0.4, 4, 6) ");
        System.out.println(cern.jet.stat.Probability.gamma(0.4,4,6));
        
        System.out.println("cern prob gamma (4, 6, 0.44) ");
        System.out.println(cern.jet.stat.Probability.gamma(4, 6, 0.4));
        
        System.out.println("cern prob gamma (4, 0.4, 6) ");
        System.out.println(cern.jet.stat.Probability.gamma(4,0.4,6));
        
               System.out.println("cern prob gamma (6, 0.4, 4) ");
        System.out.println(cern.jet.stat.Probability.gamma(6,0.4,4));
        
           System.out.println("cern prob gamma (6, 4, 0.4) ");
        System.out.println(cern.jet.stat.Probability.gamma(6,4,0.4));
        */
        
        
        System.out.println("cumdens(4, 0.2070)=" + chisquaredCDF(4, 0.2070));
        System.out.println("cumdens(6, 2.204)=" + chisquaredCDF(6, 2.204));
        System.out.println("Chi2_cumul_inv(4, 0.005)=" + chisquaredinverse(4, 0.005));
        System.out.println("Chi2_cumul_inv(6, 0.1)=" + chisquaredinverse(6, 0.1));
        
        int[] ns = new int[100];
        double[] K1s = new double[100];
        double[] K2s = new double[100];
        double alph = 0.2;
        
        for (int i = 4 ; i <=10 ; i++) {
            ns[i] = i;
            K1s[i] = QCChartCalculator.K1(i,alph);
            K2s[i] = QCChartCalculator.K2(i, alph);
            System.out.println("n="+i+"\tK1("+ i + "," + alph + ")="+ QCChartCalculator.K1(i,alph) + "\tK2("+ i + "," + alph + ")="+ QCChartCalculator.K2(i, alph));
            
        }
        System.out.println("done");
        return;
    }
    
}
