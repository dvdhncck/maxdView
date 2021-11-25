
public void doIt()
{
  ExprData.ClusterIterator clit = edata.new ClusterIterator();
  ExprData.Cluster clust = clit.getCurrent();
  while(clust != null)
  {
     if(clust.getElements() == null)
     {
       edata.setClusterColour(clust, Color.white);
     }  
     clust = clit.getNext();
  }
}
