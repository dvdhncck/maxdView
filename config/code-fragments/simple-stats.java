
public void doIt()
{

  final int ns = edata.getNumSpots();

  String msg = edata.getNumMeasurements() + " Measurements, " + ns + " Spots\n";

  for(int m=0; m < edata.getNumMeasurements(); m++)
  {
     double acc = .0;
     double[] data = edata.getMeasurementData(m);

     int nans = 0;

     for(int s=0; s < ns; s++)
     {
       if(Double.isNaN(data[s]))
	 nans++;
       else
         acc += data[s];
     }

     if(nans < ns)
       acc /= (ns - nans);

     msg += (edata.getMeasurementName(m) + ": min=" + 
             edata.getMeasurementMinEValue(m) + " max=" +
	     edata.getMeasurementMaxEValue(m) + 
             " mean=" + acc + "\n");
  }
  mview.infoMessage(msg);
}
