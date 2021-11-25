import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class RampViewer extends JButton
{
    public RampViewer(Color[] the_ramp, boolean is_reversed)
    {
	super();
	ramp = the_ramp;
	reverse = is_reversed;
	use_solid = false;
	setPreferredSize(new Dimension(80, 20));
    }

    public RampViewer()
    {
	this(null, false);
    }
	
    public RampViewer(Color[] the_ramp)
    {
	this(the_ramp, false);
    }

    public void setRamp(Color[] the_ramp)
    {
	ramp = the_ramp;
	repaint();
    }

    public void useColour(Color c)
    {
	use_solid = true;
	solid = c;
	repaint();
    }

    public void useRamp()
    {
	use_solid = false;
	repaint();
    }

    public Color[] getRamp() { return ramp; }

    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);

	if(ramp != null)
	{
	    if(use_solid)
	    {
		g.setColor(solid);
		g.fillRect(0, 0, getWidth(), getHeight());
	    }
	    else
	    {
		int w = getWidth();
		int ws = w / (ramp.length-1);
		
		int gap = getHeight() / 5;
		
		int height = getHeight() - (2*gap);
		
		g.setColor(ramp[reverse ? 0 : (ramp.length - 1)]);
		g.fillRect(0, gap, w, height);
		
		int xp = 0;
		
		for(int s=0; s < ramp.length; s++)
		{
		    g.setColor(ramp[reverse ?  (ramp.length - (s+1)) : s]);
		    g.fillRect(xp, gap, ws, height);
		    xp += ws;
		}
	    }
	}

    }

    public static Color[] makeRamp(Color from_colour, Color to_colour, int steps)
    {
	double from_d_r = (double)(from_colour.getRed());
	double from_d_g = (double)(from_colour.getGreen());
	double from_d_b = (double)(from_colour.getBlue());
	
	double to_d_r = (double)(to_colour.getRed());
	double to_d_g = (double)(to_colour.getGreen());
	double to_d_b = (double)(to_colour.getBlue());
	
	double d_step = 1.0 / (double)steps;
	double d = 0.0;
	
	Color[] cvec = new Color[steps];

	for(int c=0; c < steps; c++)
	{
	    cvec[c] = new Color((int)(((1.0-d) * from_d_r) + (d * to_d_r)),
				(int)(((1.0-d) * from_d_g) + (d * to_d_g)),
				(int)(((1.0-d) * from_d_b) + (d * to_d_b)));
	    d += d_step;
	}
	return cvec;
    }

    boolean reverse;
    boolean use_solid;
    private Color solid;
    private Color[] ramp;
}
