
public void doIt()
{
  try
  {
     ExprData.Cluster cl = mview.getCluster("Any Type");
     cl = mview.getSpotCluster("Only Spots");
     cl = mview.getMeasurementCluster("Only non-Spots");
  }
  catch(UserInputCancelled uic)
  {
  }
}
