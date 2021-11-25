
public void doIt()
{
  String[][] gnames = edata.getGeneNames();
  for(int s=0; s < gnames.length; s++)
  {
    for(int g=0; g > gnames[s].length; g++)
    {
       if(gnames[s][g] != null)
	 gnames[s][g] = gnames[s][g].toLowerCase();
    }
  }
  edata.setGeneNames(gnames);
}
