
public void doIt()
{
  try
  {
    ExprData.Cluster cl = mview.getCluster("Pick a cluster to rename");
    String prefix = mview.getString("New prefix for name");

    renameCluster(cl, prefix);

    edata.generateClusterUpdate(ExprData.ElementsAdded);
  }
  catch(UserInputCancelled uic)
  { }
}

public void renameCluster(ExprData.Cluster cl, String name)
{
  cl.setName( name );
  Vector ch = cl.getChildren();
  if(ch != null)
  {
    for(int c=0; c< ch.size(); c++)
    {
       ExprData.Cluster clc = (ExprData.Cluster) ch.elementAt(c);
       renameCluster(clc, name + "." + String.valueOf(c+1));
    }

  }
   
}
