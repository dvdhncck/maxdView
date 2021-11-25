
// randomly reorders the measurements

public void doIt()
{
   final int nmeas = edata.getNumMeasurements();

   int[] posn = new int[nmeas];
 
   for(int p=0; p < nmeas; p++)
     posn[p] = -1;

   for(int p=0; p < nmeas; p++)
   {
      boolean placed = false;
      while(!placed)
      {
        int pp = (int)(Math.random() * nmeas);
        if(posn[pp] == -1)
        {
           placed = true;
           posn[pp] = p;
        }
      }
   } 
   edata.setMeasurementOrder(posn);    
}
