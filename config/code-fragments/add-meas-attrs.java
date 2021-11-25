
public void doIt()
{
  for(int m=0; m < edata.getNumMeasurements(); m++)
  {
    for(int a=0; a < 10; a++)
    { 
      edata.getMeasurement(m).addAttribute("Att" + a, 
                                           "Synthetic",
	                                   "Val" + (int)(Math.random() * 100.0));
    }
  }
}
