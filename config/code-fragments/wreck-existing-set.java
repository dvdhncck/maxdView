
public void doIt()
{
  double[] tmp = new double[edata.getNumGenes()];
 
  for(int g=0; g <edata.getNumGenes(); g++)
    tmp[g] = ((g % 2) == 0) ? 5.0: -5.0;

  edata.setSetData("Set_2", tmp);
}
