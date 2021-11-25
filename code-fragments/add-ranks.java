
public void doIt()
{
  double[] data = new double[ edata.getNumSpots() ];

  for(int s=0; s < edata.getNumSpots(); s++)
  {
    data[ s ] = edata.getIndexOfSpot( s );
  }

  edata.addOrderedMeasurement( edata.new Measurement( "Rank3", ExprData.UnknownDataType, data ) );
}
