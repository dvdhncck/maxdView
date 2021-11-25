
public void doIt()
{
   int[] order = new int[edata.getNumSpots()];
   for(int s=0;s< edata.getNumSpots(); s++)
     order[s] = s;
   edata.setRowOrder(order);
}
