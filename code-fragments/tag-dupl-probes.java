public void doIt()
{
  Hashtable ht = new Hashtable();

  for(int p=0; p < edata.getNumSpots(); p++)
  {
    Integer i = (Integer) ht.get(edata.getProbeName(p));
    if(i != null)
    {
      int ii = i.intValue() + 1;
      edata.getProbeTagAttrs().setTagAttr(edata.getProbeName(p), "Dupl", String.valueOf(ii));
      ht.put(edata.getProbeName(p), new Integer(ii));
    }
    else
      ht.put(edata.getProbeName(p), new Integer(1));
  }
}
