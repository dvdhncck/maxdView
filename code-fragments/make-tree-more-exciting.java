
public void doIt()
{
    final int stop_at = 5;  // cluster size to aim for
    boolean running = true;

    String res = "";
    
    int count = 0;

    //for(int s=0; s< n_splits; s++)
    while(running)
    {
	// find the biggest leaf
	int most_elems = 0;
	ExprData.Cluster biggest = null;
	
	ExprData.ClusterIterator clit = edata.new ClusterIterator();
	ExprData.Cluster clust = clit.getCurrent();
	while(clust != null)
	{
	    int[] el = clust.getElements();
	    if((el != null) && (el.length > most_elems))
	    {
		most_elems = el.length;
		biggest = clust;
	    }
	    clust = clit.getNext();
	}
	if((biggest != null) && (most_elems > stop_at))
	{
	    double frac = 0.3 + Math.abs(Math.random() * 0.4);  // somwehere between 30% and 70%
	    int split = (int) ((double)most_elems * frac);

	    int[] el = biggest.getElements();
            
	    Vector lower = new Vector();
	    for(int e=0; e< split; e++)
		lower.addElement(new Integer(el[e]));

	    int remain = el.length - split;
	    
	    Vector upper = new Vector();
	    for(int e=split; e< el.length; e++)
		upper.addElement(new Integer(el[e]));
	    
            int name_mode = biggest.getElementNameMode();
            int new_name_mode = biggest.getIsSpot() ? ExprData.SpotIndex : ExprData.MeasurementIndex;

	    ExprData.Cluster lower_clust = edata.new Cluster(biggest.getName() + ".1");
	    lower_clust.setElements(new_name_mode, lower);
	    
	    ExprData.Cluster upper_clust = edata.new Cluster(biggest.getName() + ".2");
	    upper_clust.setElements(new_name_mode, upper);
	    
	    biggest.setElements(name_mode, null);
	    
	    edata.addChildToCluster(biggest, lower_clust);
	    
	    edata.addChildToCluster(biggest, upper_clust);
	    
	    count++;
	}
	else
	{
	    running = false;
	}
    }
    res = (count + " clusters split");
    mview.informationMessage(res);
}
