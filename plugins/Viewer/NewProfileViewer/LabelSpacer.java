import java.util.Vector;

public class LabelSpacer
{
    public LabelSpacer( int width, int height)
    {
	resetLabelMap(width, height);
		
    }

    public void storeLabelExtent( int lw, int lh, int lx, int ly )
    {
	int mx = (int)((double) lx * scale_x);
	int my = (int)((double) ly * scale_y);

	//System.out.println("storeLabelExtent: " + lw + "x" + lh + " @ " + lx + "," + ly + " ... " + mx + "," + my);

	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    if(label_map[mx][my] == null)
		label_map[mx][my] = new Vector();
	    label_map[mx][my].addElement(new LabelExtent( lw, lh, lx, ly ));
	}
    }
    
    // returns true if the specified label will fit into the map without overlapping anything
    //
    public boolean spaceForLabel( int scale ,int lw, int lh, int lx, int ly )
    {
	//if(!auto_space)
	//    return true;
	
	int mx = (int)((double) lx * scale_x);
	int my = (int)((double) ly * scale_y);

	//System.out.println("spaceForLabel: " + lw + "x" + lh + " @ " + lx + "," + ly + " ... " + mx + "," + my);

	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    //System.out.println("  spaceForLabel: searching from " + my);

	    int tp = my;
	    int bp = my + 1;

	    while((tp >= 0) || (bp < map_y))
	    {
		if(tp >= 0)
		{
		    if(searchMapLine( scale, mx, tp, lw, lh, lx, ly ))
			return false;
		    tp--;
		}
		if(bp < map_y)
		{
		    if(searchMapLine( scale, mx, bp, lw, lh, lx, ly ))
			return false;
		    bp++;
		}
	    }
	    
	    //System.out.println("  spaceForLabel: onscreen and there is space!");
	    return true;
	}
	return false;
    }
    
    // returns true if the specified label will overlap a label in this cell of the map
    //
    private boolean searchMapCell( int scale, int col, int row, int lw, int lh, int lx, int ly )
    {
	Vector lm = label_map[col][row];
	if(lm == null)
	    return false;

	for(int le=0; le < lm.size(); le++)
	{
	    LabelExtent lex = (LabelExtent) lm.elementAt(le);
	    if(labelsOverlap( scale, lw, lh, lx, ly, lex.lw, lex.lh, lex.lx, lex.ly ))
		return true;
	}
	return false;
    }

    // returns true if the specified label will overlap a label in the map on this line
    //
    private boolean searchMapLine( int scale, int col, int row, int lw, int lh, int lx, int ly )
    {
	//System.out.println("  searchMapLine:  col=" + col  + " row=" + row);

	int lp = col;
	int rp = col + 1;
	while((lp >= 0) || (rp < map_x))
	{
	    if(lp >= 0)
	    {
		if(searchMapCell( scale, lp, row, lw, lh, lx, ly ))
		    return true;
		lp--;
	    }
	    if(rp < map_x)
	    {
		if(searchMapCell( scale, rp, row, lw, lh, lx, ly ))
		    return true;
		rp++;
	    }
	}
	//System.out.println("  searchMapLine: not found");

	return false;
    }

    private void resetLabelMap(int w, int h)
    {
	// width = w; height = h;
	scale_x = (double) map_x / (double) w;
	scale_y = (double) map_y / (double) h;

	for(int x=0; x < map_x; x++)
	    for(int y=0; y < map_y; y++)
		if(label_map[x][y] != null)
		    label_map[x][y].clear();
    }

    private double scale_x = 1.0;
    private double scale_y = 1.0;

    private final int map_x = 10;
    private final int map_y = 10;

    private Vector[][] label_map = new Vector[map_x][map_y];
    
    private class LabelExtent
    {
	public int lw, lh, lx, ly;
	public LabelExtent(int lw_, int lh_, int lx_, int ly_)
	{
	    lw=lw_; lh=lh_; lx=lx_; ly=ly_;
	}
    }

    // scale should be a number in the range 16...32
    // (higher numbers give a large overlap)
    //
    private boolean labelsOverlap( int scale, 
				   int lw1, int lh1, int lx1, int ly1,
				   int lw2, int lh2, int lx2, int ly2 )
    {
	final int hlw1 = (lw1 * scale) / 32;
	final int hlh1 = (lh1 * scale) / 32;
	
	final int hlw2 = (lw2 * scale) / 32;
	final int hlh2 = (lh2 * scale) / 32;

	/*
	  final int hlw1 = lw1 / 2;
	  final int hlh1 = lh1 / 2;
	  
	  final int hlw2 = lw2 / 2;
	  final int hlh2 = lh2 / 2;
	*/

	final int tlx1 = lx1 - hlw1;
	final int tly1 = ly1 - hlh1;
	final int brx1 = lx1 + hlw1;
	final int bry1 = ly1 + hlh1;
	
	final int tlx2 = lx2 - hlw2;
	final int tly2 = ly2 - hlh2;
	final int brx2 = lx2 + hlw2;
	final int bry2 = ly2 + hlh2;
	
	// check the corners
	boolean h_ok = (((tlx1 >= tlx2) && (tlx1 <= brx2)) || ((brx1 >= tlx2) && (brx1 <= brx2)));
	boolean v_ok = (((tly1 >= tly2) && (tly1 <= bry2)) || ((bry1 >= tly2) && (bry1 <= bry2)));
	
	// check for containment
	if(!h_ok)
	    h_ok = ((tlx1 < tlx2) && (brx1 > brx2));
	if(!v_ok)
	    v_ok = ((tly1 < tly2) && (bry1 > bry2));
	
	return (h_ok && v_ok);
    }

}