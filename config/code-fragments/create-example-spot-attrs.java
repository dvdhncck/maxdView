
public void doIt()
{
  for(int m=0; m < edata.getNumMeasurements(); m++)
  {
    ExprData.Measurement ms = edata.getMeasurement(m);
    char[] data = new char[ edata.getNumSpots() ];
    for(int s=0; s < edata.getNumSpots(); s++)
    {  
      data[s] = 'P';
      if(Math.random() > 0.9)
        data[s] = 'A';

      if(Math.random() > 0.9)
        data[s] = 'M';
    }
    ms.addSpotAttribute("present", "no-unit", "CHAR", data);
  }
}
