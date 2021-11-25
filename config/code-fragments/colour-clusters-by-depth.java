
public void doIt()
{
    ExprData.Cluster clust = edata.getRootCluster();

    int deepest = findDeepestChild(clust);

    mview.infoMessage("Longest path has " + deepest + " branches");

    float delta = (1.0f / (float)deepest);

    String[] col_opts = { "Red", "Green", "Beige", "White", "Black" };

    int start_c = mview.getChoice("Root colour:", col_opts);
    int end_c   = mview.getChoice("Leaf colour:", col_opts);

    Color[] cols = { Color.red, 
                     Color.green, 
                     new Color(202,202,138),
                     Color.white,
	             Color.black };

    recursivelyColour(clust, 0.0f, delta, cols[start_c], cols[end_c]);

    edata.generateClusterUpdate(ExprData.ColourChanged);

}

public int findDeepestChild(ExprData.Cluster cl)
{
    int local_max = 0;

    Vector ch = cl.getChildren();
    if(ch != null)
    {
	for(int c=0; c< ch.size(); c++)
	{
	    int child_depth = findDeepestChild( (ExprData.Cluster) ch.elementAt(c) );
	    if(child_depth > local_max)
		local_max = child_depth;
	}
    }
    return local_max + 1;
}

public void recursivelyColour(ExprData.Cluster cl, 
                              float base, float delta,
                              Color start, Color end)
{
    float ibase = 1.0f - base;

    float r = (((float) start.getRed()) * ibase) + (((float) end.getRed()) * base);
    float g = (((float) start.getGreen()) * ibase) + (((float) end.getGreen()) * base);
    float b = (((float) start.getBlue()) * ibase) + (((float) end.getBlue()) * base);

    cl.setColour(new Color((int)r, (int)g, (int)b));
    
    base += delta;
    
    Vector ch = cl.getChildren();
    if(ch != null)
    {
	for(int c=0; c< ch.size(); c++)
	    recursivelyColour( (ExprData.Cluster) ch.elementAt(c), base, delta, start, end);
    }
}
