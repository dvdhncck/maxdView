
public void doIt()
{
  try
  {
     int m = mview.getMeasurementChoice();

     mview.infoMessage("You chose " + edata.getMeasurementName(m));

     int[] ms = mview.getMeasurementsChoice();

     String s = "You chose ";
     for(int i=0; i < ms.length; i++)
       s += edata.getMeasurementName(ms[i]) + " ";

     mview.infoMessage(s);

  }
  catch(UserInputCancelled uic)
  {
  }
}
