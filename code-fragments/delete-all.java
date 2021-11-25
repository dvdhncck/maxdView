
public void doIt()
{
    if(mview.infoQuestion("Really delete all data?", "Yes", "No") == 0)
    {
      edata.removeAllClusters();
      edata.removeAllMeasurements();
    }
}
