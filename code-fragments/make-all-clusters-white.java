
public void doIt()
{
  ExprData.ClusterIterator clit = edata.new ClusterIterator();
  ExprData.Cluster clust = clit.getCurrent();
  while(clust != null)
  {
     clust.setColour(Color.white);
     clust = clit.getNext();
  }
}
