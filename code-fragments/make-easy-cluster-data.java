
public void doIt()
{
  // makes the data easy to cluster for testing purposes...

  final int n_clust = 5;

  int ns = edata.getNumSpots();
 
  int step = ns / n_clust;
  double vs = 1.0 / n_clust;

  for(int m=0; m < edata.getNumMeasurements(); m++)
  {
    double[] data = new double[ns];

    int count = 0;
    double v = .0;
    for(int s=0; s < ns; s++)
    {
       data[s] = v + (Math.random() * 0.1);
       if(++count == step)
       {
          count = 0;
          v += vs;
       }
    }
    edata.setMeasurementData(m, data);
  }
}
