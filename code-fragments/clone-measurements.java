
//
// make a looky-likey for each Measurement 
// 
public void doIt()
{
  final int n_meas = edata.getNumMeasurements();

  for(int m=0; m < n_meas; m++)
  {
     double[] data = edata.getMeasurementData(m);
  
     double[] ll = new double[data.length];

     for(int d=0; d < data.length; d++)
     {
        double delta = 0.66 + (Math.random() * 0.66);
        ll[d] = data[d] * delta;
     }

     edata.addOrderedMeasurement(edata.new Measurement("clone", edata.getMeasurementDataType(m), ll));
  }
}
