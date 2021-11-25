import java.util.Arrays;
import java.util.Comparator;

public class LowessForMaxD
{

    public double[] smooth( double[] x, double[] y, Kernel kernel, double f, int iter )
    {

	//################### Sort on independent variable #############################################

	//Create independent and dependent variables
	double[] sortedX;
	double[] sortedY;


	//Sort independent variable values 
	//whilst keeping track of dependent variable. To do this use MyDoublePairs class and 
	//MyDoublePairs_Comparator

	MyDoublePairs[] xPair = new MyDoublePairs[x.length];

	//Load x values and array indices into MyDoublePairs objects
	for( int i = 0; i < x.length; i++ )
	{
	    xPair[i] = new MyDoublePairs();
	    xPair[i].setDoublePair( x[i], i );
	}

	//Sort x values whilst keeping track of array indices
	MyDoublePairs_Comparator mdpc = new MyDoublePairs_Comparator();
	Arrays.sort( xPair, mdpc );

	//Create independent and dependent variables
	sortedX = new double[x.length];
	sortedY = new double[x.length];


	for( int i = 0; i < sortedX.length; i++ )
	{
	    sortedX[i] = xPair[i].a;
	    sortedY[i] = y[xPair[i].b];
	}

	//############################ End sort on independent variable #####################################



	//Calculate regression at grid points
	double x0;                                           //Center of kernel
	double k, kx, kx2, kxy, ky;                          //Sums for calculating straight line at x0 (also called x[n])
	double detA;                                         //determinant of 2x2 matrix used for straight line determination
	double ker = 0.0;                                    //kernel value
	double bisquare = 0.0;                               //Bisquare kernel value, used for robust regression
	double[] alpha = new double[99];                     //intercept of straight line through x0
	double[] beta = new double[99];                      //Slope of straight line through x0
	int n_low, n_upper;                                  //Array indices of data points defining span about x0
	double trial_scale1, trial_scale2, scale;            //Scale parameters for kernel
	double temp1 = 0.0;                                  //Temporary holding variable
	double[] smooth = new double[sortedX.length];        //Quantity to be returned
	double[] residual = new double[sortedX.length];      //Residuals from lowess values. Used for robust regression
	double[] residualCopy = new double[sortedX.length];  //Copy of residuals for use in sorting to determine MAD of dependent variable


	//Instantiate new BiSquare kernel for robust regression
	BiSquare ykernel = new BiSquare();
	ykernel.set_scale( 1.0 );
	
	//Set all residuals initially to zero explicitly
	for( int i = 0; i < residual.length; i++ )
	{
	    residual[i] = 0.0;
	    residualCopy[i] = Math.abs( sortedY[i] );
	}

	//Re set scale of BiSquare kernel to full range of dependent variable
	Arrays.sort( residualCopy );
	ykernel.set_scale( Math.abs( residualCopy[residualCopy.length -1] - residualCopy[0] ) );



	//Calculate No. of data points corresponding to fraction f
	int span = (int) Math.floor( 0.5 * f * ((double) sortedX.length) );

	double scale_factor = 1.0 / ((double) sortedX.length);

	for( int its = 1; its <= iter; its++ )
	{
	    for( int centile = 1; centile <= 99; centile++ )
	    {
		//Find corresponding spot to centile
		int iclosest = (int) Math.floor( 0.01 * ((double) centile * sortedX.length) );
		iclosest  = iclosest < 1 ? 1 : iclosest;

                //Set x value at which lowess to be performed
                x0 = sortedX[iclosest-1];
                

		//Initialize sums
		k = kx = kx2 = ky = kxy = 0.0;

		//Calculate max and min data points to be included at x[n] (i.e. x0)
		n_upper = (iclosest + span) < sortedX.length ? (iclosest + span) : sortedX.length;
		n_low = (iclosest - span ) > 0 ? (iclosest - span) : 0;

		//Trial scales are distance from x[n] to right and left hand edges of kernel respectively
		//Set with of kernel to larger of the two
		trial_scale1 = sortedX[n_upper -1] - sortedX[iclosest-1];
		trial_scale2 = sortedX[iclosest-1] - sortedX[n_low];

		//Set kernel scale appropriately
		scale = trial_scale1 > trial_scale2 ? trial_scale1 : trial_scale2;
		kernel.set_scale( scale );

		//Loop over data points allowed by kernel and weight appropriately
		//Include bi-square kernel weighting of residuals of dependent variable
		for( int i = n_low; i < n_upper; i++ )
		{
		    ker = kernel.value(x0, sortedX[i]);
		    bisquare = ykernel.value(0.0, residual[i]);
		    ker *= bisquare;
		    k += ker;
		    kx += ker * sortedX[i];
		    kx2 += ker * Math.pow( sortedX[i], 2.0 );
		    ky += ker * sortedY[i];
		    kxy += ker * sortedX[i] * sortedY[i];
		}
		k *= scale_factor;
		kx *= scale_factor;
		kx2 *= scale_factor;
		ky *= scale_factor;
		kxy *= scale_factor;

		detA = (k * kx2) - Math.pow( kx, 2.0 );
		alpha[centile-1] = (kx2 * ky) - (kx * kxy);
		alpha[centile-1] /= detA;

		beta[centile-1] = (k * kxy) - (kx * ky);
		beta[centile-1] /= detA;
	    }


	    //Loop over all spots finding closest centile point and using local fit at closest 
	    // centile to perform correction
	    for( int spot = 0; spot < sortedX.length; spot++ )
	    {
		double xn = sortedX[spot];
		double distance = 0.0;
		double min_distance = Double.MAX_VALUE;
		int id = 1;
		int id_guess_min = 1;
		int id_guess_max = 99;
		int id_guess = (int) Math.floor( 100.0 * ((double) (spot + 1)) / ((double) sortedX.length) );
		if( id_guess < 3 )
		{
		    id_guess_min = 1;
		}
		else
		{
		    id_guess_min = id_guess - 2;
		}
		if( id_guess > 97 )
		{
		    id_guess_max = 99;
		}
		else
		{
		    id_guess_max = id_guess + 2;
		}

		for( int centile = id_guess_min; centile <= id_guess_max; centile++ )
		{
		    //Find corresponding spot to centile
		    int ncentile = (int) Math.floor( 0.01 * ((double) centile * sortedX.length) );
		    ncentile = ncentile < 1 ? ncentile = 1 : ncentile;
		    double xncentile = sortedX[ncentile-1];
		    distance = Math.abs( xncentile - xn );
		    if( distance < min_distance )
		    {
			min_distance = distance;
			id = centile;
		    }
		}


		//Local trend is at x[n] is given by alpha + (beta*x[n])
		//Corrected ratio should be residual between original ratio y[n] and 
		//local trend, i.e. y[n] - (alpha + beta*x[n])

		smooth[spot] = alpha[id-1] + (beta[id-1] * xn);
		residual[spot] = sortedY[spot] - smooth[spot];
		residualCopy[spot] = Math.abs( residual[spot] );
	    }



	    //Find median value of absolute values of residuals
	    // and set scale of BiSquare kernel
	    Arrays.sort( residualCopy );
	    double mad = residualCopy[ (int) Math.floor( 0.5 * ((double) residualCopy.length) )];
	    ykernel.set_scale( 6.0 * mad );
	    
	}

	double[] returnedSmooth = new double[smooth.length];
	for( int i = 0; i < smooth.length; i++ )
	{
	    returnedSmooth[xPair[i].b] = smooth[i];
	}

	return returnedSmooth;
    }    



    //Class for sorting double values whilst keeping original indices
    class MyDoublePairs
    {
	public double a;
	public int b;

	MyDoublePairs()
	{
	    a = 0.0;
	    b = 0;
	}

	//Accessor method
	public void setDoublePair( double atmp, int btmp )
	{
	    a = atmp;
	    b = btmp;
	}
    }

    //Comparator for MyDoublePairs
    //Sorts on the a values (from lowest to highest) of MyDoublePairs object
    public class MyDoublePairs_Comparator implements Comparator
    {

	public int compare( Object o1, Object o2 )
	{

	    MyDoublePairs my1 = (MyDoublePairs) o1;
	    MyDoublePairs my2 = (MyDoublePairs) o2;

	    if( my1.a > my2.a )
	    {
		return 1;
	    }
	    else if( my1.a < my2.a )
	    {
		return -1;
	    }
	    else
	    {
		return 0;
	    }
	}
    }
}
