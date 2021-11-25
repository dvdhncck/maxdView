
public void doIt()
{
  try
  { 
    String[] mnames = mview.getListOfMeasurementNames();

    if(mnames == null)
    {
      mview.errorMessage("No data");
      return;
    }

    int src = mview.getChoice("Pick a Measurement", mnames);

    int m_id = edata.getMeasurementFromName(mnames[src]);
 
    int[] sorted = edata.getTraversal(m_id, 0);
 
    int len = (sorted.length >= 10) ? 10 : sorted.length;
    
    int[] sel = new int[len];

    for(int s=0; s< len; s++)
      sel[s] = sorted[s];
 
    edata.setSpotSelection(sel);
  }
  catch(UserInputCancelled e)
  {
  }
}
