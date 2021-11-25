
public void doIt()
{
  //
  // offers the user a choice of measurement and
  // the sets the selection to be the 10 spots 
  // with the highest values in that measurement
  // 

  try
  { 
    int m_id = mview.getMeasurementChoice();
 
    int[] sorted = edata.getTraversal(m_id, 1);
 
    // are there at least 10 spots to pick?
    int len = (sorted.length >= 10) ? 10 : sorted.length;
    
    int[] sel = new int[len];

    for(int s=0; s< len; s++)
      sel[s] = sorted[s];
 
    // set the selection based on the array of spot ids
    edata.setSpotSelection(sel);
  }
  catch(UserInputCancelled e)
  {
  }
}
