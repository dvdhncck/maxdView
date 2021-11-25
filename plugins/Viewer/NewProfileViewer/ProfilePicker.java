import java.util.Vector;

public class ProfilePicker
{
    private final int map_x = 20;
    private final int map_y = 20;
	
    private double scale_x = 1.0;
    private double scale_y = 1.0;
	
    private Vector[][] label_map;

    public ProfilePicker()
    {
	label_map = new Vector[map_x][map_y];
    }

    public void addSegment( int sx, int sy, int ex, int ey, int spot_id )
    {
	LineSegment ls = new LineSegment( sx, sy, ex, ey, spot_id );
	    
	// rasterise this segment into the bins

	if(sx > ex)
	{
	    int tmp = sx; sx = ex; ex = tmp;
	}
	if(sy > ey)
	{
	    int tmp = sy; sy = ey; ey = tmp;
	}
	    
	int msx = (int)((double) sx * scale_x);
	int msy = (int)((double) sy * scale_y);
	
	int mex = (int)((double) ex * scale_x);
	int mey = (int)((double) ey * scale_y);

	if(msx < 0)
	    msx = 0;
	if(mex < 0)
	    mex = 0;
	if(msx < 0)
	    msx = 0;

	if(mey < 0)
	    mex = 0;
	if(msy < 0)
	    msy = 0;
	if(mey < 0)
	    mey = 0;

	if(msx >= map_x)
	    msx = map_x-1;
	if(mex >= map_x)
	    mex = map_x-1;

	if(msy >= map_y)
	    msy = map_y-1;
	if(mey >= map_y)
	    mey = map_y-1;
	    
	for(int mx=msx; mx <= mex; mx++)
	    for(int my=msy; my <= mey; my++)
	    {
		Vector vec = label_map[mx][my];
		if(vec == null)
		    vec = (label_map[mx][my] = new Vector());
		vec.addElement( ls );
	    }
    }

    public int findProfile( int x, int y )
    {
	    
	    
	int dist;
	int best_id = -1;
	int min_dist = Integer.MAX_VALUE;

	// which bin is the pointer in?
	int mx = (int)((double) x * scale_x);
	int my = (int)((double) y * scale_y);
	    
	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    // find the segment in this bin that is nearest to the pointer
	    Vector vec = label_map[mx][my];
	    if(vec != null)
	    {
		for(int seg=0; seg < vec.size(); seg++)
		{
		    LineSegment ls = (LineSegment) vec.elementAt(seg);
		    dist = ls.distanceFrom( x, y );
		    if(dist < min_dist)
		    {
			min_dist = dist;
			best_id = ls.id;
		    }
		}
	    }
	}
	return best_id;
    }

    public void setupPicker( int w, int h )
    {
	// width = w; height = h;
	scale_x = (double) map_x / (double) w;
	scale_y = (double) map_y / (double) h;
	    
	for(int x=0; x < map_x; x++)
	    for(int y=0; y < map_y; y++)
		if(label_map[x][y] != null)
		    label_map[x][y].clear();
    }
}

