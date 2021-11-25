
public void doIt()
{
  String[][] gnames = edata.getGeneNames();
  for(int s=0; s < gnames.length; s++)
  {
    gnames[s] = null;
  }
  edata.setGeneNames(gnames);
}
