    class SpotPickerNode
    {
	final int max_spots_per_box = 64;
	
	// each node is either a split point or a spot container
	
	boolean is_split;
	
	// things for split node
	SpotPickerNode[] children;
	int x_split, y_split;
	
	// things for container node
	int entries;
	
	int x, y;
	int w, h;
	
	int[] px;
	int[] py;
	int[] id;
	
	// construct a container
	public SpotPickerNode(int x_, int y_, int w_, int h_)
	{
	    x = x_; 
	    y = y_;
	    w = w_;
	    h = h_;

	    is_split = false;
	    
	    px = new int[max_spots_per_box];
	    py = new int[max_spots_per_box];
	    id = new int[max_spots_per_box];
	    
	    //System.out.println("created node[" + w + "x" + h + " @ " + x + "," + y + "]");

	    entries = 0;
	}
	
	public int findSpot(int sx, int sy, int range)
	{
	
	    //System.out.println("checking " + sx + "," + sy +" in node[" + 
	    //	       w + "x" + h + " @ " + x + "," + y + "]");

	    if(is_split)
	    {
		// delegate the search to the correct child
		int child = 0;
		if(sx >= x_split)
		    child += 1;
		if(sy >= y_split)
		    child += 2;
		return children[child].findSpot(sx, sy, range);
	    }
	    else
	    {
		// search these entries
		int best_id = -1;
		int best_dist_sq = (range * range) + 1;
		for(int e=0; e < entries; e++)
		{
		    int dist_sq = ((px[e] - sx) * (px[e] - sx)) + ((py[e] - sy) * (py[e] - sy));
		    if(dist_sq < best_dist_sq)
		    {
			best_dist_sq = dist_sq;
			best_id = id[e];
		    }
		}
		return best_id;
	    }
	}
	
	public void storeSpot(int sx, int sy, int sid)
	{
	    if(is_split)
	    {
		// delegate the storing to the correct child
		int child = 0;
		if(sx >= x_split)
		    child += 1;
		if(sy >= y_split)
		    child += 2;
		children[child].storeSpot(sx, sy, sid);
	    }
	    else
	    {
		if(entries == max_spots_per_box)
		{
		    // this container is full,  convert it into a split point
		    
		    if((w < 2) && (h < 2))
		    {
			// too small for splitting, ignore this spot

			// System.out.println("  container is full, but too small to split....");
		    }
		    else
		    {
			//System.out.println("  splitting node[" + w + "x" + h + 
			//		   " @ " + x + "," + y + ":" + entries + "]");
			
			
			// convert it into a split point and distribute the children
			//
			splitNode();

			// and try the storeSpot metho again
			storeSpot(sx, sy, sid);
		    }
		}
		else
		{
		    // store this entry
		    px[entries] = sx;
		    py[entries] = sy;
		    id[entries] = sid;
		    entries++;

		    /*
		    System.out.println(sid + " @ " + sx + "," + sy + 
				       " stored in node[" + w + "x" + h + 
				       " @ " + x + "," + y + ":" + entries + "]");
		    */

		}
	    }
	}
	
	private void splitNode()
	{
	    is_split = true;
	    
	    // pick x_split and y_split
	    
	    x_split = (w / 2);     // relative to 0,0
	    y_split = (h / 2);
	    
	    int w_1 = x_split;
	    int w_2 = w-x_split;
	    int h_1 = y_split;
	    int h_2 = h-y_split;

	    if(w == 1)
		x_split = 0;
	    if(h == 1)
		y_split = 0;
	    
	    x_split += x;          // make relative to x,y
	    y_split += y;

	    // create the children
	    
	    children = new SpotPickerNode[4];
	    
	    children[0] = new SpotPickerNode(x, y, w_1, h_1);
	    children[1] = new SpotPickerNode(x_split, y, w_2, h_1);
	    children[2] = new SpotPickerNode(x, y_split, w_1, h_2);
	    children[3] = new SpotPickerNode(x_split, y_split, w_2, h_2);
	    
	    // distribute the entries of this node to the children
	    
	    for(int e=0; e < entries; e++)
	    {
		int child = 0;
		if(px[e] >= x_split)
		    child += 1;
		if(py[e] >= y_split)
		    child += 2;
		children[child].storeSpot(px[e], py[e], id[e]);
	    }
	    
	    // free up the now unused arrays of this node
	    
	    px = py = id = null;
	    entries = 0;
	}

	public void dumpStats(final String pad)
	{
	    if(is_split)
	    {
		final String ipad = " " + pad;
		for(int c=0; c < 4; c++)
		    children[c].dumpStats(ipad);
	    }
	    else
	    {
		System.out.println(pad + " [" + 
				   w + "x" + h + " @ " + 
				   x + "," + y + ":" + 
				   entries + "]");
	    }
	}

	public void drawNode(Graphics g)
	{

	    g.setColor(Color.white);
	    
	    if(is_split)
	    {
		for(int c=0; c < 4; c++)
		    children[c].drawNode(g);
	    }
	    else
		g.drawRect(x, y, w-1, h-1);
	}
	
    }
