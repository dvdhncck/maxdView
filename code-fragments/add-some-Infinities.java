
// adds some NaNs to the data for testing....

public void doIt()
{
  try
  {
    int n = mview.getInteger("How many Infinities?", 1, 100);
    
    for(int nn=0; nn < n; nn++)
    { 
       int s = (int) (Math.random() * (double) edata.getNumSpots());
       int m = (int) (Math.random() * (double) edata.getNumMeasurements());

       if( Math.random() > 0.5 )
         edata.setEValue(m, s, Double.POSITIVE_INFINITY);
       else
        edata.setEValue(m, s, Double.NEGATIVE_INFINITY);

    }
  } 
  catch(UserInputCancelled uic)
  {
  }
}
