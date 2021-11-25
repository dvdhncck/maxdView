
public void doIt()
{
   Vector sid = new Vector();
   sid.addElement(new Integer(0));
   sid.addElement(new Integer(1));
   
   ExprData.Cluster ch1 = edata.new Cluster("ch-1", ExprData.SpotIndex, sid);

   sid = new Vector();
   sid.addElement(new Integer(2));
   sid.addElement(new Integer(3));
   
   ExprData.Cluster ch2 = edata.new Cluster("ch-2", ExprData.SpotIndex, sid);


   sid = new Vector();
   sid.addElement(new Integer(4));
   //sid.addElement(new Integer(5));
   ExprData.Cluster par = edata.new Cluster("parent", ExprData.SpotIndex, sid);
   par.addCluster(ch1);
   par.addCluster(ch2);

   edata.addCluster(par);

}
