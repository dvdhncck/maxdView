
public void doIt()
{
  for(int m=0; m < edata.getNumMeasurements(); m++)
     edata.setMeasurementName(m, String.valueOf(m+1));
}
