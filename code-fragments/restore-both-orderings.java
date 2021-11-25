
public void doIt()
{
   int[] order = new int[edata.getNumSpots()];
   for(int s=0; s < edata.getNumSpots(); s++)
     order[s] = s;
   edata.setRowOrder(order);

   order = new int[edata.getNumMeasurements()];
   for(int m=0; m < edata.getNumMeasurements(); m++)
     order[m] = m;
   edata.setMeasurementOrder(order);
}
