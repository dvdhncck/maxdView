
public void doIt()
{
  int[] ids = edata.addSpots(4,"Test");

  for(int s = 0; s < 4; s++)
    for(int m=0; m < edata.getNumMeasurements(); m++)
    {
       edata.getMeasurement(m).setEValue(ids[s], (m*s));
    }

  edata.generateDataUpdate(ExprData.ValuesChanged);
}
