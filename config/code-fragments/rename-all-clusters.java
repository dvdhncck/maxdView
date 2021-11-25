
public void doIt()
{
  ExprData.Cluster cl = edata.getRootCluster();

  Vector ch = cl.getChildren();
  if(ch != null)
  {
    for(int c=0; c< ch.size(); c++)
    {
       ExprData.Cluster clc = (ExprData.Cluster) ch.elementAt(c);
       renameCluster(clc, String.valueOf(c+1));
    }

  }
  edata.generateClusterUpdate(ExprData.ElementsAdded);
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
