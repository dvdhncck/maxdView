
public void doIt()
{
  try
  {
    int m_id = mview.getMeasurementChoice( );

    double noise_scale = mview.getDouble("Noise scale level (%age)", 0,100,10);

    double[] data = new double[ edata.getNumSpots() ];

    noise_scale *= 0.01;
    double twice_noise_scale = 2.0 * noise_scale;

    for(int s=0; s < edata.getNumSpots(); s++)
    {
       double in = edata.eValue( m_id, s );

       double noise = in * ((Math.random() * twice_noise_scale) - noise_scale);

       data[s]= in + noise;
     }

     edata.addOrderedMeasurement( edata.new Measurement("noisy " + edata.getMeasurementName(m_id), 
                                  ExprData.ExpressionAbsoluteDataType, 
                                  data ) );

   }
   catch(UserInputCancelled uic)
   {
   }
}
