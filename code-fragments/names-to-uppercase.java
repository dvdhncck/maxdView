
public void doIt()
{
   String[] pn = edata.getProbeName();
   for(int p=0; p < pn.length; p++) 
   { 
      if(pn[p] != null)
        pn[p] = pn[p].toUpperCase();
   }
   edata.setProbeName(pn);
}
