//
// make a cluster from the spots which
// are visible under the current filter
// 

public void doIt()
{
  if(edata.filterIsOn() == false)
  {
    mview.errorMessage("There is no filter enabled");
    return;
  }
  else
  {
    final int ns = edata.getNumSpots();
    
    java.util.Vector spot_ids = new java.util.Vector();

    for(int s=0; s < ns; s++)
      if(!edata.filter(s))
        spot_ids.addElement(new Integer(s));

    mview.infoMessage(spot_ids.size() + " spots currently visible with this filter");
    
    if(spot_ids.size() > 0)
      edata.addCluster("filter", ExprData.SpotIndex, spot_ids);

  }
}
