
public void doIt()
{
   final double min = edata.visitAll(edata.new ScalarRedFunc()
		{
		   public double eval(double red, double in)
		   {
		      return (in < red) ? in : red;
		   }
	        });
  edata.visitAll(edata.new ScalarFunc()
		 {
		   public double eval(double in)
		   {
		      return in-min;
		   }
	          });
}
