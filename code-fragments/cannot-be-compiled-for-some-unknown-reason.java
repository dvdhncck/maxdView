
public void doIt()
{
  try
  {
    int m_id = getMeasurementChoice(  );

    double[] data = new double[ edata.getNumSpots() ];

    for(int s=0; s < edata.getNumSpots(); s++)
    {
       double in = edata.eValue( m_id, s );

       if(in == 0)
          data[s]= 0;
       if(in > 0)
          data[s]= Math.log(in);
       else
	  data[s] = - Math.log(-in);
     }

     edata.addOrderedMeasurement( edata.new Measurement("logged " + edata.getMEasurementName(m_id), 
                                  ExprData.ExpressionAbsoluteDataType, 
                                  data ) );
}
}
