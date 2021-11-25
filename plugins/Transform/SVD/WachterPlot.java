public class WachterPlot
{
    //Public class to calculate quantiles of Random Matrix spectra 
    //corresponding to sample set of eigenvalues

    double[] eigenvalues;
    int N, M;
    double alpha;
    double sig2 = 0.0;  // Noise level estimate
    double[] quantiles;

    public double[][] doWachter( double[] lambda, int n1, int n2 )
    {

	eigenvalues = new double[lambda.length];
	N = n1;
	M = n2;
	
	for( int i = 0; i < lambda.length; i++ )
	{
	    eigenvalues[i] = lambda[i];
	}
	
	for( int i = 0; i < eigenvalues.length; i++ )
	{
	    sig2 += eigenvalues[i];
	}
	sig2 /= (double) N;
	alpha = ((double) M) / ((double) N);
	
	//Calculate cumulative probabilities of Wigner semi-circle law
	calc_quantiles( 20000 );
	double lambda_min = sig2 * Math.pow( (Math.sqrt(alpha) - 1.0), 2.0) / alpha;
	double lambda_max = sig2 * Math.pow( (Math.sqrt(alpha) + 1.0), 2.0) / alpha;
	double delta = (lambda_max - lambda_min) / 20000.0;

	//Assume eigenvalues ordered from highest to lowest
	double[][] coords = new double[2][eigenvalues.length]; 
	for( int i = 0; i < eigenvalues.length; i++ )
        {
	    int j = 0;
	    double q = ((double) (N - i - 1)) / ((double) N);
	    while( quantiles[j] < q )
	    {
		j++;
	    }
	    double lambda_q = lambda_min + (delta * ((double) j));
	    coords[0][i] = lambda_q;
	    coords[1][i] = eigenvalues[i];
	}

	return coords;
    
    }



    private void calc_quantiles( int n )
    {
	quantiles = new double[n];

	double lambda_min = sig2 * Math.pow( (Math.sqrt(alpha) - 1.0), 2.0) / alpha;
	double lambda_max = sig2 * Math.pow( (Math.sqrt(alpha) + 1.0), 2.0) / alpha;
	double delta = (lambda_max - lambda_min) / ((double) n);

	double lastOrdinateValue = 0.0;
	double ordinate = lambda_min + delta;
	double newOrdinateValue = pdf( ordinate );
	quantiles[0] = 0.5 * delta * (newOrdinateValue + lastOrdinateValue );
	quantiles[0] += (1.0 - alpha);

	for( int i = 1; i < quantiles.length; i++ )
	{
	    ordinate += delta;
	    newOrdinateValue = pdf( ordinate );
	    quantiles[i] = quantiles[i-1] + (0.5 * delta * (newOrdinateValue + lastOrdinateValue ));
	    lastOrdinateValue = newOrdinateValue;
	}


    }

    private double pdf( double x )
    {
	double lambda_min = sig2 * Math.pow( (Math.sqrt(alpha) - 1.0), 2.0) / alpha;
	double lambda_max = sig2 * Math.pow( (Math.sqrt(alpha) + 1.0), 2.0) / alpha;

	double density = Math.sqrt( (lambda_max - x ) * ( x - lambda_min) );
	density /= (x * sig2 * 2.0 * Math.PI);
	density *= alpha;

	return density;
    }
}

