public void doIt()
{
  final int range = 1000;
  final int min = 1;

  try
  {

    int new_m = mview.getInteger("How many Measurements?", 1, 100, 1);

    int new_s = mview.getInteger("How many Spots?", 1, 10000, 100);

    int dupl_pc = mview.getInteger("Percentage of duplicate Probes?", 1, 100, 10);

    edata.removeAllMeasurements();
    edata.removeAllClusters();

    String[] sname = new String[new_s];
    String[] pname = new String[new_s];

    for(int d=1; d <= new_s; d++)
    {
  
       sname[d-1] = ("spot" + d);
       pname[d-1] = ("probe" + d);
    }

   // make  N% of the probes into duplicates

   double percent_dupls = (double) dupl_pc;

   int n_dupls = (int)((double) new_s * percent_dupls);
	
   for(int d=0; d < n_dupls; d++)
   {
     // pick two random indices
 
     int p1 = (int)(Math.random() * new_s);
     int p2 = (int)(Math.random() * new_s);

     pname[p2] = pname[p1];
   }
 
   for(int m=1; m <= new_m; m++)
   {
      double[] data = new double[new_s];

      for(int s=0; s < new_s; s++)
      {
        data[s] = (Math.random() * range) + min;
      }

      ExprData.DataTags dt = edata.new DataTags(sname, pname, null);

      ExprData.Measurement nm = edata.new Measurement("Synth-" + m, ExprData.ExpressionAbsoluteDataType, data);

      nm.setDataTags(dt);

      if(m == 1)
        edata.addMeasurement(nm);
      else
        edata.addOrderedMeasurement(nm);
   }

  }
  catch(UserInputCancelled e)
  {
  }
}
