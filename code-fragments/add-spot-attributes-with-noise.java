
// example:
//
// adds some SpotAttributes to each Measurement

public void doIt()
{
  
   final int ns = edata.getNumSpots();

   for(int m=0; m < edata.getNumMeasurements(); m++)
   {
 
     double[] da = new double[ ns ];
  
     for(int s=0; s < ns; s++)
     { 
       double value = (double) edata.eValue( m , s );

	double noise = 0.95 + ( Math.random() * 0.1 );

	value *= noise;

	da[ s] = value;
     }

     edata.getMeasurement(m).addSpotAttribute("10%Noise", "no-unit", "DOUBLE", da);

    da = new double[ ns ];

     for(int s=0; s < ns; s++)
     { 
       double value = (double) edata.eValue( m , s );

	double noise = 0.90 + ( Math.random() * 0.2 );

	value *= noise;

	da[ s ] = value;
     }

     edata.getMeasurement(m).addSpotAttribute("20%Noise", "no-unit", "DOUBLE", da);
    }


}
