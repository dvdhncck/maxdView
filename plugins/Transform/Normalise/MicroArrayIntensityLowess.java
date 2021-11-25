//Wrapper class to allow lowess normalization of two colour arrays
//Class passes two channels of data to a LowessForMaxD object 
//Majority of parameters are set at defaults for time being


import java.util.Arrays;

public class MicroArrayIntensityLowess
{
    public static double[][] normalize( double[] red, double[] green )
    {

	//Initialize M and A arrays
	int countValid = 0;
	for( int i = 0; i < red.length; i++ )
	{
	    if( !Double.isNaN( red[i] ) && !Double.isNaN( green[i] ) )
	    {
		if( red[i] > 0.0 && green[i] > 0.0 )
		{
		    countValid++;
		}
	    }
	}
	double[] a = new double[countValid];
	double[] m = new double[countValid];

	int count = 0;
	for( int i = 0; i < red.length; i++ )
	{
	    if( !Double.isNaN( red[i] ) && !Double.isNaN( green[i] ) )
	    {
		if( red[i] > 0.0 && green[i] > 0.0 )
		{
		    a[count] = 0.5 * ( Math.log( red[i] ) + Math.log( green[i] ) );
		    m[count] =  Math.log( red[i] ) - Math.log( green[i] );
		    count++;
		}
	    }
	}
		    
		

	//Instantiate kernel ( on independent variable ) for locally weighted regression
	TriCube xkernel = new TriCube();
	xkernel.set_scale( 1.0 );

	//Set no. of iterations for robust regression
	int iter = 3;

	//Set span for kernel
	double f = 0.3;

	//Set new median of dependent variable
	double delta = 0.0;

	LowessForMaxD lowess = new LowessForMaxD();
	double[] localTrend = lowess.smooth( a, m, xkernel, f, iter );

	double[] smooothedM    = new double[red.length];
	double[] uncorrectedM  = new double[red.length];
	double[] correctedM    = new double[red.length];
	double[] correctedA    = new double[red.length];

	count = 0;
	for( int i = 0; i < red.length; i++ )
	{
	    if( !Double.isNaN( red[i] ) && !Double.isNaN( green[i] ) )
	    {
		if( red[i] > 0.0 && green[i] > 0.0 )
		{
		    uncorrectedM[i] = m[count];

		    correctedM[i] = m[count] - localTrend[count];
		    correctedA[i] = a[ count ];
		    
		    smooothedM[i] = localTrend[count];

		    count++;
		}
		else
		{
		   uncorrectedM[i] = smooothedM[i] = correctedA[i] = correctedM[i] = Double.NaN;
		}
	    }
	    else
	    {
		uncorrectedM[i] = smooothedM[i] = correctedA[i] = correctedM[i] = Double.NaN;
	    }
	}

	
	double[] correctedMCopy = new double[correctedM.length];
	for( int i = 0; i < correctedM.length; i++ )
	{
	    correctedMCopy[i] = correctedM[i];
	}
	Arrays.sort( correctedMCopy );

	int nmedian = (int) Math.floor( 0.5 * ((double) correctedMCopy.length ));
	nmedian--;
	nmedian = nmedian <0 ? 0 : nmedian;
	double medianM = correctedMCopy[nmedian];

	//Adjust median to desired value
	medianM += delta;

	for( int i = 0; i < correctedM.length; i++ )
	{
	    correctedM[i] -= medianM;
	}


	double[][] result = new double[4][];
	
	result[0] = correctedA;
	result[1] = uncorrectedM;
	result[2] = correctedM;
	result[3] = smooothedM;

	return result;
    }
}
