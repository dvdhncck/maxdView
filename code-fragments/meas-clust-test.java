
public void doIt()
{
  Vector mnames = new Vector();
  mnames.addElement("1");
  mnames.addElement("2");
  mnames.addElement("3");

  ExprData.Cluster c1 = edata.new Cluster("c1", ExprData.MeasurementName, mnames);

  mnames = new Vector();
  mnames.addElement("4");
  mnames.addElement("5");

  ExprData.Cluster c2 = edata.new Cluster("c2", ExprData.MeasurementName, mnames);

  mnames = new Vector();
  mnames.addElement("7");
  mnames.addElement("8");
  mnames.addElement("9");
  mnames.addElement("10");

  ExprData.Cluster c3 = edata.new Cluster("c3", ExprData.MeasurementName, mnames);

  ExprData.Cluster p = edata.new Cluster("Meas Cluster Parent", ExprData.MeasurementName);
  p.addCluster(c1);
  p.addCluster(c2);
  p.addCluster(c3);

  edata.addCluster(p);


}
