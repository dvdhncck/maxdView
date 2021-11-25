public void doIt()
{
  try
  {
    ExprData.NameTagSelection nts = mview.getNameTagSelection("Which Name or Attribute?");
    


    Hashtable ht = new Hashtable();
    final int ns = edata.getNumSpots();
  
    for(int s=0; s < ns; s++)
    {
      String[] na = nts.getNameTagArray(s);
      if(na != null)
      {
        for(int n=0; n < na.length; n++)
        { 
           Vector v = (Vector) ht.get(na[n]);
           if(v == null)
           {
             v = new Vector();
             ht.put(na[n], v);
           }
           v.addElement(new Integer(s));
        }
      }
    }

    // mview.infoMessage("so far...");
    String cname = "Duplicated '" + nts.getNames() + "'";
    ExprData.Cluster top = edata.new Cluster(cname);

    Enumeration enum = ht.keys();
    while(enum.hasMoreElements())
    {
      String key = (String) enum.nextElement();
      Vector v = (Vector) ht.get(key);
      if((v != null) && (v.size() > 1))
      {
        String name = v.size() + " x " + key;
        ExprData.Cluster cl = edata.new Cluster(name, ExprData.SpotIndex, v);
        top.addCluster(cl);
      }
    }
    if(top.getNumChildren() > 0)
    {
      mview.infoMessage(top.getNumChildren() + " found");
      edata.addCluster(top);
    }
    else
      mview.infoMessage("No duplicates found");
  }
  catch(UserInputCancelled uic)
  {
  }
}
