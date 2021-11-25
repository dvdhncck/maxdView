
// adds some NaNs to the data for testing....

public void doIt()
{
  try
  {
    int n = mview.getInteger("How many NaNs?", 1, 100);
    
    for(int nn=0; nn < n; nn++)
    { 
       int s = (int) (Math.random() * (double) edata.getNumSpots());
       int m = (int) (Math.random() * (double) edata.getNumMeasurements());

       edata.setEValue(m, s, Double.NaN);
    }
  } 
  catch(UserInputCancelled uic)
  {
  }
}
