public void doIt()
{
  Hashtable ht = new Hashtable();
  final int ns = edata.getNumSpots();
  
  for(int p=0; p < ns; p++)
  {
    Vector v = (Vector) ht.get(edata.getProbeName(p));
    if(v == null)
    {
      v = new Vector();
      ht.put(edata.getProbeName(p), v);
    }
    v.addElement(new Integer(p));
  }
  
  // mview.infoMessage("so far...");
  
  ExprData.Cluster top = edata.new Cluster("Duplicate Probes");

  for(int p=0; p < ns; p++)
  {
    Vector v = (Vector) ht.get(edata.getProbeName(p));
    if((v != null) && (v.size() > 1))
    {
       String name = v.size() + " x " + edata.getProbeName(p);
       ExprData.Cluster cl = edata.new Cluster(name, ExprData.SpotIndex, v);
       top.addCluster(cl);

       //System.out.println(edata.getProbeName(p));
       ht.remove(edata.getProbeName(p));
    }
  }

  edata.addCluster(top);

  // mview.infoMessage("...so good");
}
