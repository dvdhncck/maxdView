import java.util.Arrays;
import java.util.Comparator;

public class Svdcmp
{
  private double[] w;
  private double[][] a;
  private double[][] v;

  /** Public class to evaluate the Singular Value Decomposition (SVD) 
   *  of a matrix <B>mat</B>. The matrix <B>mat</B> is specified as a 2-Dimensional array of 
   *  doubles. The SVD of the matrix is performed within the constructor. The 
   *  matrix is decomposed as <B>mat</B> = <B>UDV</B><SUP>T</SUP>. 
   *  The columns of the matrix <B>U</B> form an orthonormal 
   *  basis. The columns of the matrix <B>V</B> also from an orthonormal basis. The matrix <B>D</B> is 
   *  diagonal with the entries being the eigenvalues of mat. <B>U</B>, <B>D</B> and <B>V</B> 
   *  are obtained through public accessor methods. The matrix <B>mat</B> is assumed MxN with M >= N. 
   *  If M < N the SVD of <B>mat</B><SUP>T</SUP> is returned instead. 
   *  The public method <I>order_eigenvalues()</I>, 
   *  re-arranges the eigenvalues of <B>mat</B> from highest to lowest. If the  
   *  eigenvectors (columns of <B>U</B> and <B>V</B>) that correspond to the 
   *  ordered list of eigenvalues are required, the <I>order_eigenvalues</I> method should 
   *  be called before using the public accessor methods <I>get_U()</I> and <I>get_V()</I>.
   *
   *  The code is based upon that in Numerical Recipes in C 2nd Edition (W.H. Press et al, Cambridge 
   *  University Press, 1992), which in turn in based upon a routine by Forsythe et al., 
   *  Computer Methods For Mathematical Computations (Prentice-Hall, 1977) and is similar to that used 
   *  in LINPACK and EISPACK matrix computation packages.
   *
   *
   *  @author David. C. Hoyle
   *  @version 1.0
   */

   /* TO DO 
      1)Proper exception throwing when convergence of algorithm not reached in set number of iterations 
      (currently 30). Need to decide what class the exception should be.
      2)No checking of matrix elements of mat currently performed. Any NaNs will cause routine to fail. */
 
  public Svdcmp( double[][] mat )
  {

	int m, n;
	int itemp1, itemp2;
	int flag, i, its, j, jj, k, l, nm;
	double anorm, c, f, g, h, s, scale, x, y, z;
	double temp1;
	double[] rv1;

          l = nm = 0;

          //Assign dimensions of array
          m = mat.length;
          n = mat[0].length;

          //Make copy of array mat into array a
          //If m < n then take transpose of matrix

          if( m >= n )
          {
            a = new double[m][n];
            for( i = 0; i < m; i++ )
            {
              for( j = 0; j < n; j++ )
              {
                a[i][j] = mat[i][j];
              }
            }
          }
          else
          {
            a = new double[n][m];
            for( i = 0; i < n; i++ )
            {
              for( j = 0; j < m; j++ )
              {
                a[i][j] = mat[j][i];
              }
            }
            itemp1 = m;
            m = n;
            n = itemp1;
          }


          w = new double[n];
          v = new double[n][n];

	rv1 = new double[n];

	g = scale = anorm = 0.0;
	for(i = 0; i < n; i++ )
	{
	  	l = i + 1;
	  	rv1[i] = scale * g;
		g = s = scale = 0.0;
		if( i < m )
		{
			for( k = i; k < m; k++ )
			{
			  scale += Math.abs( a[k][i] );
			}
			if( scale != 0.0 )
			{
				for( k = i; k < m; k++ )
				{
					a[k][i] /= scale;
					s += a[k][i] * a[k][i];
				}
				f = a[i][i];
				temp1 = Math.sqrt( s );
				g = ( f >= 0.0 ? -Math.abs( temp1 ) : Math.abs( temp1 ) );
				h = ( f * g ) - s;
				a[i][i] = f - g;
				for( j = l; j < n; j++ )
				{
					for( s = 0.0, k = i; k < m; k++ )
					{
					  s += a[k][i] * a[k][j];
					}
					f = s / h;
					for( k = i; k < m; k++ )
					{
					  a[k][j] += f * a[k][i];
					}
				}
				for( k = i; k < m; k++ )
				{
				  a[k][i] *= scale;
				}
			}
		}
		w[i] = scale * g;
		g = s = scale = 0.0;
		if( i < m && i != n - 1 )
		{
			for( k = l; k < n; k++ )
			{
			  scale += Math.abs( a[i][k] );
			}
			if( scale != 0.0 )
			{
				for( k = l; k < n; k++ )
				{
					a[i][k] /= scale;
					s += a[i][k] * a[i][k];
				}
				f = a[i][l];
				temp1 = Math.sqrt( s );
				g = ( f >= 0.0 ? -Math.abs( temp1 ) : Math.abs( temp1 ) );
				h = ( f * g ) - s;
				a[i][l] = f - g;
				for( k = l; k < n; k++ )
				{
				  rv1[k] = a[i][k] / h;
				}
				for( j = l; j < m; j++ )
				{
					for( s = 0.0, k = l; k < n; k++ )
					{
					  s += a[j][k] * a[i][k];
					}
					for( k = l; k < n; k++ )
					{
					  a[j][k] += s * rv1[k];
					}
				}
				for( k = l; k < n; k++ )
				{
				  a[i][k] *= scale;
				}
			}
		}
		anorm = Math.max( anorm, ( Math.abs( w[i] ) + Math.abs( rv1[i] ) ) );
	}
	for(i = n - 1; i >= 0; i-- )
	{
	  if( i < n - 1 )
	  {
			if( g != 0.0 )
			{
				for( j = l; j < n; j++ )
				{
					v[j][i] = ( a[i][j] / a[i][l] ) / g;
				}
				for( j = l; j < n; j++ )
				{
					for( s = 0.0, k = l; k < n; k++ )
					{
					  s += a[i][k] * v[k][j];
					}
					for( k = l; k < n; k++ )
					{
					  v[k][j] += s * v[k][i];
					}
				}
			}
			for( j = l; j < n; j++ )
			{
			  v[i][j] = v[j][i] = 0.0;
			}
		}
		v[i][i] = 1.0;
		g = rv1[i];
		l = i;
	}
	for( i = Math.min( m, n ) - 1; i >= 0; i-- )
	{
		l = i + 1;
		g = w[i];
		for( j = l; j < n; j++ )
		{
		  a[i][j] = 0.0;
		}
		if( g != 0.0 )
		{
			g = 1.0 / g;
			for( j = l; j < n; j++ )
			{
				for( s = 0.0, k = l; k < m; k++ )
				{
				  s += a[k][i] * a[k][j];
				}
				f = ( s / a[i][i] ) * g;
				for( k = i; k < m; k++ )
				{
				  a[k][j] += f * a[k][i];
				}
			}
			for( j = i; j < m; j++ )
			{
			  a[j][i] *= g;
			}
		}
		else
		{
		  for( j = i; j < m; j++ )
		  {
		    a[j][i] = 0.0;
		  }
		}
		++a[i][i];
	}
	for( k = n - 1; k >= 0; k-- )
	{
		for( its = 1; its <= 30; its++ )
		{
			flag = 1;
			for( l = k; l >= 0; l-- )
			{
				nm = l - 1;
				if( (double) ( Math.abs( rv1[l] ) + anorm ) == anorm )
				{
					flag = 0;
					break;
				}
				if( (double) ( Math.abs( w[nm] )+ anorm ) == anorm )
				{
				  break;
				}
			}
			if( flag != 0 )
			{
				c = 0.0;
				s = 1.0;
				for( i = l; i <= k; i++ )
				{
					f = s * rv1[i];
					rv1[i] = c * rv1[i];
					if( (double) ( Math.abs( f ) + anorm ) == anorm )
					{
					  break;
					}
					g = w[i];
					h = pythag( f, g );
					w[i] = h;
					h = 1.0 / h;
					c = g * h;
					s = -f * h;
					for( j = 0; j < m; j++ )
					{
						y = a[j][nm];
						z = a[j][i];
						a[j][nm] = (y * c) + (z * s);
						a[j][i] = (z * c) - (y * s);
					}
				}
			}
			z = w[k];
			if( l == k )
			{
				if( z < 0.0 )
				{
					w[k] = -z;
					for( j = 0; j < n; j++ )
					{
					  v[j][k] = -v[j][k];
					}
				}
				break;
			}
			if( its == 30 )
			{
			  System.out.println( "no convergence in 30 svdcmp iterations" );
			}
			x = w[l];
			nm = k - 1;
			y = w[nm];
			g = rv1[nm];
			h = rv1[k];
			f = ( ( y - z ) * ( y + z ) + ( g - h ) * ( g + h ) ) / ( 2.0 * h * y );
			g = pythag( f, 1.0 );
			temp1 = ( f >= 0.0 ? Math.abs( g ) : -Math.abs( g ) );
			f = ( ( x - z ) * ( x + z ) + h * ( ( y / ( f + temp1 ) ) -h ) ) / x;
			c = s = 1.0;
			for( j = l; j <= nm; j++ )
			{
				i = j + 1;
				g = rv1[i];
				y = w[i];
				h = s * g;
				g = c * g;
				z = pythag( f, h );
				rv1[j] = z;
				c = f / z;
				s = h / z;
				f = (x * c) + (g * s);
				g = (g * c) - (x * s);
				h = y * s;
				y *= c;
				for( jj = 0; jj < n; jj++ )
				{
					x = v[jj][j];
					z = v[jj][i];
					v[jj][j] = (x * c) + (z * s);
					v[jj][i] = (z * c) - (x * s);
				}
				z = pythag( f, h );
				w[j] = z;
				if( z != 0.0 )
				{
					z = 1.0 / z;
					c = f * z;
					s = h * z;
				}
				f = (c * g) + (s * y);
				x = (c * y) - (s * g);
				for( jj = 0; jj < m; jj++ )
				{
					y = a[jj][j];
					z = a[jj][i];
					a[jj][j] = (y * c) + (z * s);
					a[jj][i] = (z * c) - (y * s);
				}
			}
			rv1[l] = 0.0;
			rv1[k] = f;
			w[k] = x;
		}
	}
	rv1 = null;
  }


  /** Public method to order (from highest to lowest) the eigenvalues of the matrix passed to the 
   *  constructor. The corresponding columns of the <B>U</B> and <B>V</B> are also re-ordered.
   */
  public void orderEigenvalues()
  {

	//Sort eigenvalues into descending order and move columns of
	//a and v correspondingly

	MyDoublePairsComparator mdpc = new MyDoublePairsComparator();


	MyDoublePairs[] eigval_copy = new MyDoublePairs[w.length];
	for( int i = 0; i < w.length; i++ )
	{
	  eigval_copy[i] = new MyDoublePairs();
	  eigval_copy[i].setDoublePair( w[i], i );
	}

	Arrays.sort( eigval_copy, mdpc );

	double ustore;
	double vstore;
	int[] store_list = new int[w.length];
	int itemp1, itemp2;

	for( int i = 0; i < w.length; i++ )
	{
	  w[i] = eigval_copy[i].a;
	  store_list[i] = i;
	}

	for( int i = 0; i < w.length; i++ )
	{
	  itemp1 = eigval_copy[i].b;
	  if( itemp1 != i )
	  {
	    itemp2 = store_list[itemp1];

	    for( int j = 0; j < a.length; j++ )
	    {
	      ustore = a[j][i];
	      a[j][i] = a[j][itemp2];
	      a[j][itemp2] = ustore;
	    }

	    for( int j = 0; j < v.length; j++ )
	    {
	      vstore = v[j][i];
	      v[j][i] = v[j][itemp2];
	      v[j][itemp2] = vstore;
	    }

	    int j = 0;
	    while( store_list[j] != i )
	    {
	      j++;
	    }
	    store_list[j] = itemp2;
	    store_list[itemp1] = i;
	  }
        }
  }



  private double pythag( double a, double b )
  {
 	double absa, absb;
	double temp1, temp2;

	absa = Math.abs( a );
	absb = Math.abs( b );
	if( absa > absb )
	{
	  temp1 = absb / absa;
	  temp2 = ( temp1 == 0.0 ? 0.0 : Math.pow( temp1, 2.0 ) );
	  return  ( absa * Math.sqrt( 1.0 + temp2 ) );
	}
	else
	{
	  temp1 = absa / absb;
	  temp2 = ( temp1 == 0.0 ? 0.0 : Math.pow( temp1, 2.0 ) );
	  return ( absb == 0.0 ? 0.0 : absb * Math.sqrt( 1.0 + temp2 ) );
	}
  }


  /** Public accessor method to obtain the eigenvalues of the matrix passed to the 
   *  class constructor, i.e. the diagonal elements of matrix <B>D</B>. */ 
  public double[] getEigenvalues()
  {
    int i;
    double[] eigval = new double[w.length];

    for( i = 0; i < w.length; i++ )
    {
      eigval[i] = w[i];
    }

    return eigval;
  }

  /** Public accessor method to obtain the matrix <B>U</B> of the SVD of the matrix 
   *  passed to the class constructor. */
  public double[][] getU()
  {
    int i,j;

    double[][] U_copy = new double[a.length][a[0].length];
    for( i = 0; i < U_copy.length; i++ )
    {
      for( j = 0; j < U_copy[0].length; j++ )
      {
        U_copy[i][j] = a[i][j];
      }
    }

    return U_copy;
  }

  /** Public accessor method to obtain the matrix <B>V</B> of the SVD of the matrix 
   *  passed to the class constructor. */
  public double[][] getV()
  {
    int i,j;

    double[][] V_copy = new double[v.length][v[0].length];
    for( i = 0; i < V_copy.length; i++ )
    {
      for( j = 0; j < V_copy[0].length; j++ )
      {
        V_copy[i][j] = v[i][j];
      }
    }

    return V_copy;
  }


  class MyDoublePairs
  {
     public double a;
     public int b;

     MyDoublePairs()
     {
       a = 0.0;
       b = 0;
     }

     public void setDoublePair( double atmp, int btmp )
     {
       a = atmp;
       b = btmp;
     }
  }

  public class MyDoublePairsComparator implements Comparator
  {

     public int compare( Object o1, Object o2 )
     {

       MyDoublePairs my1 = (MyDoublePairs) o1;
       MyDoublePairs my2 = (MyDoublePairs) o2;

       if( my1.a < my2.a )
       {
         return 1;
       }
       else if( my1.a > my2.a )
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



