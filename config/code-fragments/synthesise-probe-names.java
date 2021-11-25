public void doIt()
{
  final int ns = edata.getNumSpots();

  String[] pname = new String[ns];

  for(int s=0; s < ns; s++)
  {
     String name = "Y";
	
     name += (Math.random() > 0.5) ? "A" : "B";
    
     name += (Math.random() > 0.5) ? "L" : "R";

     int number = (int) (Math.random() * 500.0);

     String number_s = String.valueOf(number);

     if(number < 10)
       number_s = "0" + number_s;

     if(number < 100)
       number_s = "0" + number_s;

     name += number_s;

     name += (Math.random() > 0.5) ? "W" : "C";

     pname[s] = name;
  }

  edata.setProbeName( pname );
}
