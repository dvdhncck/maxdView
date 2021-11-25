public void doIt()
{
  final int ns = edata.getNumSpots();
  
  String[] pns = new String[ns];

  for(int p=0; p < ns; p++)
  {
    String name = randNum() + randNum() + "/" + randCh() + randCh() + randCh() + randNum();

    pns[p] = name;
  }
  
  edata.setProbeName(pns);
}

public String randCh()
{
  final String az = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  int c = (int) (Math.random() * 25.0);
  return String.valueOf(az.charAt(c));
}
public String randNum()
{
  int i = (int) (Math.random() * 9.0);
  return String.valueOf(i);
}
