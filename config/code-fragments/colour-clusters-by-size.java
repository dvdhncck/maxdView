
public void doIt()
{
    ExprData.Cluster clust = edata.getRootCluster();

    int biggest = findBiggestChild(clust);

    if(biggest > 0)
    {
      mview.infoMessage("Biggest cluster has " + biggest + " elements");
    }
    else
    {
      mview.errorMessage("No clusters have any elements");
      return;
    }

    float delta = (1.0f / (float)biggest);

    String[] col_opts = { "Red", "Green", "Beige", "White", "Black" };

    try
    {
      int small_c = mview.getChoice("Small colour:", col_opts, 0);
      int big_c   = mview.getChoice("Big colour:", col_opts, 1);

      Color[] cols = { Color.red, 
                       Color.green, 
                       new Color(202,202,138),
                       Color.white,
	               Color.black };

      recursivelyColour(clust, biggest, cols[small_c], cols[big_c]);

      edata.generateClusterUpdate(ExprData.ColourChanged);
    }
    catch(UserInputCancelled e)
    {
    }
}

public int findBiggestChild(ExprData.Cluster cl)
{
    int local_max = cl.getSize();

    Vector ch = cl.getChildren();
    if(ch != null)
    {
	for(int c=0; c< ch.size(); c++)
	{
	    int child_max = findBiggestChild( (ExprData.Cluster) ch.elementAt(c) );
	    if(child_max > local_max)
		local_max = child_max;
	}
    }
    return local_max;
}

public void recursivelyColour(ExprData.Cluster cl, 
                              int biggest,
                              Color start, Color end)
{
    float base  = (float) cl.getSize() / (float) biggest;
    float ibase = 1.0f - base;

    float r = (((float) start.getRed()) * ibase) + (((float) end.getRed()) * base);
    float g = (((float) start.getGreen()) * ibase) + (((float) end.getGreen()) * base);
    float b = (((float) start.getBlue()) * ibase) + (((float) end.getBlue()) * base);

    cl.setColour(new Color((int)r, (int)g, (int)b));
    
    Vector ch = cl.getChildren();
    if(ch != null)
    {
	for(int c=0; c< ch.size(); c++)
	    recursivelyColour( (ExprData.Cluster) ch.elementAt(c), biggest, start, end);
    }
}
