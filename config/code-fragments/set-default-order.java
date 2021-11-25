//
// returns the rows to their 'natural' order
// (i.e. the order they were originally loaded in)
//
public void doIt()
{
  int[] new_order = new int[edata.getNumSpots()];
  for(int s=0; s < edata.getNumSpots(); s++)
    new_order[s] = s;
  edata.setRowOrder(new_order);
}
