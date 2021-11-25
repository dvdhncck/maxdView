
public void doIt()
{
  int tc = 0;

  for(int s=0; s < edata.getNumSpots(); s++)
  { 
    AnnotationLoader anlo = mview.getAnnotationLoader();

    String a = anlo.loadAnnotation(null, edata.getProbeName(s));

    int st = a.indexOf("<B>Description:</B>") + 24;

    String a1 = a.substring(st);
 
    int se = a1.indexOf("</P>");

    if(se < 0)
      se = a1.length();

    String a2 = a1.substring(0, se);

    String[] gn = new String[1];
    gn[0] = a2;
  
    tc += gn[0].length();

    edata.setGeneNames(s, gn);
  }
  mview.infoMessage(tc + " chararacters");
}
