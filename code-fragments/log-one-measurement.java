
public void doIt()
{
  try
  {
    int m_id = mview.getMeasurementChoice( );

    double[] data = new double[ edata.getNumSpots() ];

    double shift_value = 0.0;

    double very_small_number = 0.000000001;
    
    if(Double.isNaN( edata.getMeasurementMinEValue( m_id )  ))
    {
       mview.alertMessage("Data contains NaN values which cannot be logged");
       return;
    }

    if ( edata.getMeasurementMinEValue( m_id ) <= .0 )
       shift_value = edata.getMeasurementMinEValue( m_id ) + very_small_number;

    for(int s=0; s < edata.getNumSpots(); s++)
    {
       double in = edata.eValue( m_id, s );

       in += shift_value;

       data[s]= Math.log(in);
     }

     edata.addOrderedMeasurement( edata.new Measurement("logged " + edata.getMeasurementName(m_id), 
                                  ExprData.ExpressionAbsoluteDataType, 
                                  data ) );

   }
   catch(UserInputCancelled uic)
   {
   }
}
