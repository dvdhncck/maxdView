
public class CodeWrapper
{
    public CodeWrapper(maxdView mview_) 
    {  mview = mview_; 
       dplot = mview.getDataPlot();
       edata = mview.getExprData();
    }
    
    public void doIt() { }
    
    protected ExprData edata;
    protected DataPlot dplot;
    protected maxdView mview;
}

