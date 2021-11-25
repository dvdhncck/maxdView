
public void doIt()
{
  edata.visitAll(edata.new ScalarFunc()
		 {
		   public double eval(double in)
		   {
		      return -in;
		   }
	          });
}
