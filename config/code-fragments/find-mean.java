
public void doIt()
{
  try
  {
     int m = mview.getMeasurementChoice();

     double[] d = edata.getMeasurementData(m);

     double a = .0;
      
     for(int s=0; s < edata.getNumSpots(); s++)
       a += edata.eValue( m, s);

     a /= (double) edata.getNumSpots();

     mview.infoMessage(edata.getMeasurementName(m) + " mean = " + a);


  }
  catch(UserInputCancelled uic)
  {
  }
}
