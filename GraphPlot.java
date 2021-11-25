import java.io.*;
import java.awt.*;

public class GraphPlot
{
    public GraphPlot( int type_, String name_, double[] xdata_, double[] ydata_, String[] label_, Color col_, int glyph_, boolean copy )
    {
	type = type_;
	name = name_;
	glyph = glyph_;
	label = label_;
	xdata = copy ? (double[]) xdata_.clone() : xdata_;
	ydata = copy ? (double[]) ydata_.clone() : ydata_;
	col = col_;

	if( label != null )
	    System.out.println( label.length + " labels defined...");

    }

    public GraphPlot( int type_, String name_, double[] xdata_, double[] ydata_, Color col_, int glyph_ )
    {
	this( type_, name_, xdata_, ydata_, null, col_, glyph_, true );
    }

    public GraphPlot( int type_, String name_, double[] xdata_, double[] ydata_, Color col_ )
    {
	this( type_, name_, xdata_, ydata_, null, col_, BOX_GLYPH, true );
    }

    public int type;
    public String name;
    public double[] xdata;
    public double[] ydata;
    public String[] label;
    public Color col;
    public int glyph;

    public final static int NO_GLYPH             = 0;
    public final static int BOX_GLYPH            = 1;
    public final static int CROSS_GLYPH          = 2;
    public final static int CIRCLE_GLYPH         = 3;
    public final static int DIAMOND_GLYPH        = 4;
    public final static int FILLED_BOX_GLYPH     = 5;
    public final static int FILLED_CIRCLE_GLYPH  = 6;
    public final static int FILLED_DIAMOND_GLYPH = 7;

    public final static int ALIGN_TOP    = 0;
    public final static int ALIGN_MIDDLE = 1;
    public final static int ALIGN_BOTTOM = 2;
    public final static int ALIGN_LEFT   = 0;
    public final static int ALIGN_RIGHT  = 2;

}
