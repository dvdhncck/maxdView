import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class RampEditor extends JPanel implements MouseListener, MouseMotionListener
{
    public RampEditor(ExprData ed_, RampedColouriser cr_, boolean is_reversed)
    {
	super();
	cr = cr_;
	edata = ed_;
	//System.out.println("ramp is " + (cr.ramp == null ? "null" : "ok") + "\n");

	reverse = is_reversed;

	setPreferredSize(new Dimension((cr.steps * 3), 20));

	addMouseListener(this);
	addMouseMotionListener(this);
    }
    
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);

	if(reverse)
	{
	    ramp = cr.neg_ramp;
	    point = cr.neg_point;
	}
	else
	{
	    ramp = cr.pos_ramp;
	    point = cr.pos_point;
	}

	
	//System.out.println("paint.... " + getWidth() + " x " +  getHeight());

	int gap = 1; // getHeight() / 5;
	    
	int height = getHeight() - (2*gap);

	if(ramp != null)
	{
	    int w = getWidth();
	    int ws = w / (ramp.length-1);
	    
	    
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
	
	int my = getHeight() / 2;
	int hdw = (int)(0.25 * (double)getHeight());   // half diamond width
	if(hdw < 1)
	    hdw = 1;
	 
	int[] xp = new int[4];
	int[] yp = new int[4];
	
	// draw little diamonds at the positions of the inner points
	//
	for(int p=1; p < (point.length-1); p++)
	{

	    //System.out.println("point " + p + " at " + point[p]);

	    // pick the colour from the other end of the ramp
	    // from where we're trying to draw the triangle
	    //
	    int ci = (int) (point[p] * (cr.steps-1));

	    if(reverse)
		ci = cr.steps - (ci+1);

	    //System.out.println(point[p] + " ... " + ci);

	    Color c = ramp[ci];

	    
	    //if(((c.getRed() + c.getGreen() + c.getBlue()) / 3) < 64)

	    g.setColor(Color.white);
	    
	    int pp = (int)(point[p] * (double)getWidth());
	    
	    if(reverse)
		pp = getWidth() - pp;
	    
	    xp[0] = pp;     yp[0] = my-hdw;
	    xp[1] = pp-hdw; yp[1] = my;
	    xp[2] = pp;     yp[2] = my+hdw;
	    xp[3] = pp+hdw; yp[3] = my;
	    
	    if(p == selected_point)
	    {
		g.drawPolygon(xp, yp, 4);
	    }
	    else
	    {
		g.fillPolygon(xp, yp, 4);
	    }

	    g.setColor(Color.black);

	    g.drawLine(pp, 0, pp, yp[0]);
	    g.drawLine(pp, yp[2], pp, getHeight());
	}
    }

    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

    public void mouseDragged(MouseEvent e)
    {
	if(!dragging)
	    return;

	double pn = (double) e.getX() / (double)getWidth();
	if(reverse)
	    pn = 1.0 - pn;

	cr.setPoint(!reverse, selected_point, pn);
	
	repaint();
    }

    public void mouseMoved(MouseEvent e) 
    {
    }

    public void mousePressed(MouseEvent e) 
    {
	// find nearest point
	//
	int np = -1;
	double nd = 1.0;

	double pn = (double) e.getX() / (double)getWidth();
	if(reverse)
	    pn = 1.0 - pn;

	//System.out.println("pick at " + pp);

	for(int p=1; p < (point.length-1); p++)
	{
	    double d = Math.abs(point[p] - pn);
	    //System.out.println("dist to " + point[p] + " = " + d);	    
	    if(d < nd)
	    {
		nd = d;
		np = p;
		//System.out.println("**best");
	    }
	}

	//System.out.println("nearest is " + np);
	
	if((np > 0) && (np < (point.length-1)))
	{
	    selected_point = np;
	    dragging = true;
	    repaint();
	    //System.out.println("point " + np + " selected");
	}
    }

    public void mouseReleased(MouseEvent e) 
    {
	if(dragging)
	{
	    dragging = false;
	    
	    // notify the world that the update has finished
	    edata.generateDataUpdate(ExprData.ColourChanged);
	}
    }
    
    public void mouseEntered(MouseEvent e) 
    {
    }
    
    public void mouseExited(MouseEvent e) 
    {
    }
    
    public void mouseClicked(MouseEvent e) 
    {
    }
    

    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

    public void addNode()
    {
	cr.addNode(!reverse);
	repaint();
    }
    
    public void deleteNode()
    {
	if(selected_point > 0)
	{
	    cr.deleteNode(!reverse, selected_point);
	    selected_point = -1;
	}
	repaint();
    }

    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

    private ExprData edata;
    private RampedColouriser cr;
    
    private boolean dragging;
    private int selected_point;
    
    Color[]   ramp;
    double[] point;

    private boolean reverse;
}
