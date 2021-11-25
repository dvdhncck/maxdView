
public void doIt()
{
  long before = Runtime.getRuntime().freeMemory();
  System.gc();
  long after = Runtime.getRuntime().freeMemory();

  int delta = (int)(after - before);

  if(delta > 0)
     mview.successMessage("Recovered " + delta + " bytes");
  else
     mview.errorMessage("Lost " + delta + " bytes");
}
