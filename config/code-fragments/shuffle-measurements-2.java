
// randomly reorders the measurements

public void doIt()
{
   final int nmeas = edata.getNumMeasurements();

   final int n_iterations = nmeas * 50;
 
   int[] posn = new int[nmeas];
 
   for(int p=0; p < nmeas; p++)
     posn[p] = p;

   for(int i=0; i < n_iterations; i++)
   {
     // pick two random indices
 
     int p1 = (int)(Math.random() * nmeas);
     int p2 = (int)(Math.random() * nmeas);
 
     // and swap them

     int temp = posn[p1];
     posn[p1] = posn[p2];
     posn[p2] = temp; 
   }

   // install the new ordering as we proceed

   edata.setMeasurementOrder(posn); 
}
