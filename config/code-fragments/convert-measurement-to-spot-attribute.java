
public void doIt()
{
  try
  { 
    boolean again = true;
 
    while(again)
    { 
      String[] mnames = mview.getListOfMeasurementNames();

      if(mnames == null)
      {
        mview.errorMessage("No data");
        return;
      }

      int src = mview.getChoice("Pick the source Measurement", mnames);

      int dest = mview.getChoice("Pick the destination for '" + mnames[src] + "'", mnames);

      if(src == dest)
        mview.errorMessage("don't be silly");
      else
      {
        ExprData.Measurement sm = edata.getMeasurement(src);
 
        ExprData.Measurement dm = edata.getMeasurement(dest);
        dm.addSpotAttribute(sm.getName(), "no unit", "DOUBLE", sm.getData());
    
        edata.removeMeasurement(src);
      }

 
      again = (mview.infoQuestion("Another?", "Yes", "No") == 0);
    }
  }
  catch(UserInputCancelled e)
  {
  }
}
