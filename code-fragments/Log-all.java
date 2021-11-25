
public void doIt()
{
  edata.visitAll(edata.new ScalarFunc()
		 {
		   public double eval(double in)
		   {
		      if(in == 0)
                        return 0;
		      if(in > 0)
                        return Math.log(in);
	              else
	                return - Math.log(-in);
		   }
	         });
}
