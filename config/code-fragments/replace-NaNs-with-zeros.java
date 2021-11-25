
// replaces any NaNs in the data with 0.0

public void doIt()
{
   for(int m=0; m < edata.getNumMeasurements(); m++)
     for(int s=0; s < edata.getNumSpots(); s++)
       if(Double.isNaN(edata.eValue(m, s)))
         edata.setEValue(m, s, .0);
}
