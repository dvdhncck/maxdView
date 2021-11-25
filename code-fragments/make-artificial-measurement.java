
public void doIt()
{
  try
  {
    double min = mview.getDouble("Min", -1000, 1000);
    double max = mview.getDouble("Max", -1000, 1000);

    double range = max - min;

    double[] data = new double[ edata.getNumSpots() ];

    for(int s=0; s < edata.getNumSpots(); s++)
    {
       data[ s ] = min + ( Math.random() * range );
    }

    edata.addOrderedMeasurement( edata.new Measurement( "Art1", ExprData.ExpressionAbsoluteDataType, data ) );

  }
  catch(UserInputCancelled uic)
  {
  }
}
