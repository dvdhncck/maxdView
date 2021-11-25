
// example:
//
// adds some SpotAttributes to each Measurement

public void doIt()
{
  
   final int ns = edata.getNumSpots();

   for(int m=0; m < edata.getNumMeasurements(); m++)
   {
     double[] da = new double[ ns ];
  
     for(int s=0; s < ns; s++)
     { 
       da[s] = (double) (m+s);
     }

     edata.getMeasurement(m).addSpotAttribute("sid", "no-unit", "DOUBLE", da);

     int[] ia = new int[ ns ];
  
     for(int s=0; s < ns; s++)
     { 
       ia[s] = m+(ns - s);
     }

     edata.getMeasurement(m).addSpotAttribute("isid", "no-unit", "INTEGER", ia);

      char[] ca = new char[ edata.getNumSpots() ];
 
      for(int s=0; s < ns; s++)
      {  
        ca[s] = 'P';
        if(Math.random() > 0.9)
          ca[s] = 'A';

        if(Math.random() > 0.9)
          ca[s] = 'M';
      }
      edata.getMeasurement(m).addSpotAttribute("present", "no-unit", "CHAR", ca);

      String[] sa = new String[ edata.getNumSpots() ];
 
      String[] owners = {"Susan","Eric","Sandra","Derek","Sheila","Fred","Julie","Mario","Jane","Luigi"};
      for(int s=0; s < ns; s++)
      {  
        sa[s] = owners[(int)(Math.random() * 10)];
      }
      edata.getMeasurement(m).addSpotAttribute("owner", "name", "TEXT", sa);
   }
}
