
public void doIt()
{
  try
  {
    int[] combine = mview.getMeasurementsChoice(); // "Merge which Measurements?"
    
    final int n_spots = edata.getNumSpots();

    double[] new_data = new double[ n_spots ];

    for(int s=0; s < n_spots; s++)
    {  
       for(int m=0; m < combine.length; m++)
       {
          double v = edata.eValue( combine[m], s );

	  if( Double.isNaN( v ) == false )
             new_data[s] = v;
       }
    }

    edata.addOrderedMeasurement( edata.new Measurement("Combination", ExprData.ExpressionAbsoluteDataType, new_data )); 
  }
  catch(UserInputCancelled uic)
  { 
  }
}
