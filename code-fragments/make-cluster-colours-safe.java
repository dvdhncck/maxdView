
public void doIt()
{
  Color c = mview.getBackgroundColour();

  ExprData.ClusterIterator clit = edata.new ClusterIterator();
  ExprData.Cluster clust = clit.getCurrent();
  int fixed = 0;
  while(clust != null)
  {
     Color cc = clust.getColour();
     	
     // get the distance between colours
     int rd = c.getRed()   - cc.getRed();
     int gd = c.getGreen() - cc.getGreen();
     int bd = c.getBlue()  - cc.getBlue();

     double d = Math.sqrt((rd * rd) + (gd * gd) + (bd * bd));
     if(d < 90)
     {
        fixed++;
        clust.setColour(Color.white);
     }
     clust = clit.getNext();
  }

  mview.infoMessage(fixed + " cluster colours changed");
}
