
public void doIt()
{
  double[] tmp = new double[edata.getNumGenes()];
  double[] s1 = edata.getSetData("Set_1");
  double[] s2 = edata.getSetData("Set_2");
  double[] s3 = edata.getSetData("Set_3");

  for(int g=0; g < edata.getNumGenes(); g++)
  {
    tmp[g] = (s1[g] + s2[g] + s3[g]) / 3.0;
  }
  edata.addSet("Mean:1..3", tmp); 
}
