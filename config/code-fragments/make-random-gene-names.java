public void doIt()
{
  final int ns = edata.getNumSpots();
  
  String[][] gns = new String[ns][];

  for(int p=0; p < ns; p++)
  {
    gns[p] = new String[1];

    String name = randCh() + randCh() + randCh() + randNum();

    gns[p][0] = name;
  }
  
  edata.setGeneNames(gns);
}

public String randCh()
{
  final String az = "AAABCCDEEEEFFGGHIIJKKLMMNOPQRSSSSTTTTUUUVWWXYZ";
  final double scale = (double)(az.length() - 1);

  int c = (int) (Math.random() * scale);
  return String.valueOf(az.charAt(c));
}
public String randNum()
{
  int i = (int) (Math.random() * 9.0);
  return String.valueOf(i);
}
