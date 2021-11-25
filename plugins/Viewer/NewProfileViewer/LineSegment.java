public class LineSegment
{
    public LineSegment( int sx_, int sy_, int ex_, int ey_, int id_ )
    {
	sx = sx_; sy = sy_; ex = ex_; ey = ey_; id = id_;

	m_inf = (sx == ex);

	if(!m_inf)
	{
	    m = (double)( ey-sy ) / (double)( ex-sx );
	    c = ((double) sy) - (m * ((double) sx));

	    m_intersect = 1.0 / m;
	}
    }

    int sx, sy, ex, ey, id;
    double m, c;
    boolean m_inf;    // vertical lines have an infinite slope

    double m_intersect;

    public int distanceFrom( int x, int y )
    {
	if(m_inf)
	{
	    return (x - sx) * (x - sx);
	}
	else
	{
	    // find the equation of the line that 
	    //  (a) is perpendicular to this segment
	    //  (b) passes through (x,y)
	    //
		
	    // slope is known: m_intersect
		
	    double c_intersect = ((double) y) - (m_intersect * ((double) x));

	    // from:
	    //
	    //   1.this segment:    y1 = m1 x1 + c1;
	    //   2.the intersect:   y2 = m2 x2 + c2;
	    //
	    //   the intersection point is (p,q)
	    //
	    //   y1, x1, y2, x2, m1, m2 and c1 are known
	    //
	    //   solving intersect for p: 
	    //      m1 p + c1 = m2 p + c2
	    //      p = (c2 - c1) / (m1 - m2);
	    //
		
	    double p = (c_intersect - c) / (m - m_intersect);

	    double q = (m * p) + c;

	    int pi = (int) p;
	    int qi = (int) q;

	    return ((x-pi) * (x-pi)) + ((y-qi) * (y-qi));
	}
    }
}

