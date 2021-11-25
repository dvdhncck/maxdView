import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.event.*;

// ======================================================================================
// 
//  KnobPanel provides a bunch of knobs for scaling, rotation
//
// ======================================================================================

public class KnobPanel extends JPanel implements MouseMotionListener, MouseListener, ComponentListener
{
    public KnobPanel( String image_dir, boolean enable_auto_spin_ )
    {
	enable_auto_spin = enable_auto_spin_;

	addMouseListener(this);
	addMouseMotionListener(this);
	addComponentListener(this);

	klv = new Vector();    // vector of KnobListeners
	kmlv = new Vector();   // vector of KnobMotionListeners

	knob_image = new ImageIcon(image_dir + "dimple.gif").getImage();

	knob_spin_image = new Image[4];
	knob_spin_image[0] = new ImageIcon(image_dir + "knobspin-2.gif").getImage(); // 2 == off
	knob_spin_image[1] = new ImageIcon(image_dir + "knobspin-0.gif").getImage(); // 0 == cw
	knob_spin_image[2] = knob_spin_image[0];
	knob_spin_image[3] = new ImageIcon(image_dir + "knobspin-1.gif").getImage(); // 1 == ccw


	if(enable_auto_spin)
	{
	    spin_thread = new SpinThread();
	    spin_thread.start();
	}
    }
    
    public void shutdown()
    {
	// System.out.println("finished....");
	if(spin_thread != null)
	{
	    // System.out.println("killing thread....");
	    spin_thread.alive = false;
	}
    }

    public void setLabels(String[] labels_)
    {
	labels = labels_;
	layoutKnobs();
	repaint();
    }

    public void setMinMax(int min_, int max_)
    {
	min_angle = min_;
	max_angle = max_;
	angle_scale = 1.0 / (360.0-(2.0*min_angle));

	repaint();
    }

    // === talking to outside world ==================

    public interface KnobListener
    {
	public void update(int knob, double value);
    }
    
    public void addKnobListener( KnobListener kl )
    {
	klv.addElement(kl);
    }
    
    public interface KnobMotionListener
    {
	public void update(int knob, double value);
    }
    
    public void addKnobMotionListener( KnobMotionListener kml )
    {
	kmlv.addElement(kml);
    }

    public void update(double[] new_data)
    {
	k_val = new_data;
	
	layoutKnobs();

	repaint();
    }
 

    // === mouse handling ==================

    private void maybeMoveKnob(final int ex, final int ey)
    {
	final int knob = (ey - k_top) / (k_diam + label_height + k_gap);
	
	if(knob < 0)
	    return;
	if(knob >= n_knobs)
	    return;
	if(ex < k_left)
	    return;
	if(ex >= (k_left + k_diam))
	    return;
	
	int knob_mx = k_left + k_mid;
	int knob_my = k_top + k_mid + (knob * (k_diam + label_height + k_gap));
	
	int dx = ex - knob_mx;
	int dy = ey - knob_my;
	
	int dist_sq = (dx * dx) + (dy * dy);
	
	if(dist_sq < (k_mid * k_mid))
	{
	    // inside the knob...
	    double angle = angleAt( new Point(0,0), new Point(dx,dy) );
	    
	    double old_angle = (k_val[ knob ] / angle_scale) - min_angle;

	    double angle_delta = angle - old_angle;

	    // angle += angle_delta;

	    if(angle < min_angle)
		angle = min_angle;
	    if(angle > max_angle)
		angle = max_angle;
	       
	    updated_knob = knob;
	    updated_value = (angle - min_angle) * angle_scale;
	    has_updated = true;
	    
	    k_val[ knob ] = updated_value;
	    
	    repaint();

	    for(int l=0; l < kmlv.size(); l++)
		((KnobMotionListener) kmlv.elementAt(l)).update(updated_knob, updated_value);
	}
    }

    private boolean maybeSpinKnob(final int ex, final int ey)
    {
	final int knob = (ey - k_top) / (k_diam + label_height + k_gap);
	
	if(knob < 0)
	    return false;
	if(knob >= n_knobs)
	    return false;
	if(ex < k_left)
	    return false;
	if(ex >= (k_left + k_diam))
	    return false;
	
	int knob_mx = k_left + k_mid;
	int knob_my = k_top + k_mid + (knob * (k_diam + label_height + k_gap));
	
	int dx = ex - knob_mx;
	int dy = ey - knob_my;
	
	int dist_sq = (dx * dx) + (dy * dy);
	
	if(dist_sq <= (dimple_off * dimple_off))
	{
	    // System.out.println("spin to win");

	    if(++spin_mode[knob] == 4)
		spin_mode[knob] = 0;
	    
	    repaint();

	    return true;
	}
	
	return false;
    }


    public void mouseMoved(MouseEvent e) 
    {
	
    }
    
    public void mouseDragged(MouseEvent e) 
    {	    
	maybeMoveKnob(e.getX(), e.getY());
    }
    
    public void mouseReleased(MouseEvent e) 
    {
	if(has_updated)
	{
	    for(int l=0; l < klv.size(); l++)
		((KnobListener) klv.elementAt(l)).update(updated_knob, updated_value);
	    has_updated = false;
	}
    }
    
    public void mousePressed(MouseEvent e) 
    {
	if(!maybeSpinKnob(e.getX(), e.getY()))
	    maybeMoveKnob(e.getX(), e.getY());
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) 
    {
    }
    
    
    // === graphics ==================

    public void paintComponent(Graphics graphic)
    {
	if(graphic == null)
	    return;
	
	graphic.setColor(Color.white);
	
	graphic.fillRect(0, 0, ww, wh);
	
	if(n_knobs == 0)
	    return;
	
	if(k_val == null)
	    return;
	
	if((ww != getWidth()) || (wh != getHeight()))
	    layoutKnobs();

	if(n_labels > 0)
	{
	    int lh = graphic.getFontMetrics().getAscent();
	    if(lh != label_height)
	    {
		label_height = lh;
		layoutKnobs();
	    }
	}
	
	// System.out.println("painted " + n_knobs + " knobs, " + n_labels + " labels, height=" + label_height);

	int kx = k_left;
	int ky = k_top;
	    
	int kh = (wh / n_knobs);
	
	final Color inner = new Color(180,180,180);
	final Color outer = new Color(80,80,80);
	
	
	for(int k=0; k < n_knobs; k++)
	{
	    
	    graphic.setColor(inner);
	    graphic.fillOval(kx, ky, k_diam, k_diam);
	    
	    graphic.setColor(outer);
	    graphic.drawOval(kx, ky, k_diam, k_diam);
	    
	    // ticks: minus sign
	    graphic.drawLine(kx-2+k_gap, ky+k_diam-k_gap, kx+2+k_gap, ky+k_diam-k_gap);
	    
	    // ticks: plus sign
	    graphic.drawLine(kx+k_diam-ticks_size-k_gap, ky+k_diam-k_gap, kx+k_diam+ticks_size-k_gap, ky+k_diam-k_gap);
	    graphic.drawLine(kx+k_diam-k_gap,ky+k_diam-ticks_size-k_gap, kx+k_diam-k_gap, ky+k_diam+ticks_size-k_gap);
	    
	    final double angle =  (k_val[k] / angle_scale) + min_angle;

	    final Point2D.Double knob_e = rotatePoint(new Point2D.Double(.0,(k_rad*.75)), angle);
	    
	    //final Point2D.Double knob_s = rotatePoint(new Point2D.Double(.0,(k_rad*.5)), angle);
	    //graphic.drawLine(kx+(int)knob_s.x+k_mid, ky+(int)knob_s.y+k_mid, 
	    //		 kx+(int)knob_e.x+k_mid, ky+(int)knob_e.y+k_mid);
	    
	    // dimple indicating position
	    graphic.drawImage(knob_image, 
			      kx+k_mid+(int)knob_e.x-dimple_off, ky+k_mid+(int)knob_e.y-dimple_off, 
			      dimple_size, dimple_size, 
			      null);

	    
	    if(enable_auto_spin)
	    {
		graphic.drawImage(knob_spin_image[spin_mode[k]], 
				  kx+k_mid-dimple_off, ky+k_mid-dimple_off, 
				  dimple_size, dimple_size,
				  null);
	    }

	    ky += k_diam;

	    if(k < n_labels)
	    {
		graphic.setColor(Color.black);
		graphic.drawString(labels[k], kx, ky+label_height);
	    }
	    
	    ky += k_gap + label_height;
	}

    }
    
    public void componentResized(ComponentEvent e) 
    {
	layoutKnobs();
    }

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e)  {}
    public void componentShown(ComponentEvent e)  {}

    private void layoutKnobs()
    {
	n_knobs = (k_val == null) ? 0 : k_val.length;
	

	spin_mode = new int[n_knobs];

	if(n_knobs == 0)
	    return;
	
	n_labels = (labels == null) ? 0 : labels.length;

	if(n_labels > n_knobs)
	    n_labels = n_knobs;
	
	ww = getWidth();
	wh = getHeight();
	
	// System.out.println("updated to " + n_knobs + " knobs, " + n_labels + " label height=" + label_height);

	int space = wh / n_knobs;
	
	if(space > (70+label_height))
	    space = (70+label_height);
	if(space > ww)
	    space = ww;
	
	k_gap = (int) ((double) space * 0.1);
	if(k_gap < 1)
	    k_gap = 1;
	
	k_diam = space - label_height - k_gap;
	k_rad = (double) k_diam * 0.5;
	k_mid = (int) k_rad;
	
	k_left = (ww - k_diam) / 2;
	k_top = (wh - (n_knobs * (k_diam+label_height+k_gap))) / 2;
	
	dimple_size = (int)(k_rad / 2.5);
	
	
	if(dimple_size>10)
	    dimple_size = 10;
	if(dimple_size<1)
	    dimple_size = 1;
	
	dimple_off = dimple_size / 2;
	
	ticks_size = dimple_off;
	if(ticks_size > 2)
	    ticks_size = 2;
	if(ticks_size < 1)
	    ticks_size = 1;

    }

   
	// ======================================================================================
	// 
	// rotate a point
	//
	Point2D.Double rotatePoint(Point2D.Double p, double angle)
	{
	    final double ang = angle * 0.017452;   // deg -> rad
	    
	    final double c_ang = Math.cos(ang);
	    final double s_ang = Math.sin(ang);
	    
	    return new Point2D.Double( (p.x * c_ang) - (p.y * s_ang), (p.x * s_ang) + (p.y * c_ang) );
	}
	
	// generates an angle (0..359) between the 2 points
	//   with 0 degs at 12 o'clock, 90 degs at 3 o'clock, 180 degs at 6 o'clock and so on...
	//
	double angleAt(Point p, Point o)
	{
	    Point vec = new Point(p.x - o.x, p.y - o.y);
	    if(vec.x == 0)
	    {
		return (vec.y > 0) ? 180 : 0;
	    }
	    else
	    {
		double o_over_a = (double) vec.y / (double) vec.x;
		double ang = (Math.atan(o_over_a) * 57.29578); // rad -> deg
		return (vec.x >= 0) ? (ang + 90.0) : (ang + 270.0);
	    }
	}
	
    // ======================================================================================

    // SpinThread does the auto spinning...

    private class SpinThread extends Thread
    {
	public boolean alive = true;

	public void run()
	{
	    final double spin_delta = 4.5 * angle_scale;

	    while(alive)
	    {
		int last_updated_knob = -1;

		for(int k=0; k < n_knobs; k++)
		{
		    if(spin_mode[k] == 1)
		    {
			k_val[k] += spin_delta;
			if(k_val[k] > 1.0)
			    k_val[k] -= 1.0;
			last_updated_knob = k;
		    }
		    if(spin_mode[k] == 3)
		    {
			k_val[k] -= spin_delta;
			if(k_val[k] < .0)
			    k_val[k] += 1.0;
			last_updated_knob = k;
		    }
		}

		if(last_updated_knob >= 0)
		{
		    repaint();

		    // notify listeners

		    for(int l=0; l < klv.size(); l++)
			// Note: 'value' arg is WRONG but not used...
			((KnobListener) klv.elementAt(l)).update(last_updated_knob, .0); 

		    // System.out.println("SpinThread update!");
		}
	    
		try 
		{
		    Thread.sleep(250);
		}
		catch (InterruptedException e)
		{
		}
	    }

	    // System.out.println("SpinThread has died");
	}
    }

    // ======================================================================================
    
    double min_angle = 35.0;
    double max_angle = 360.0-min_angle;
    double angle_scale = 1.0 / (360.0-(2.0*min_angle));

    // ======================================================================================

    private int ww = -1;
    private int wh = -1;

    boolean enable_auto_spin;
    private int[] spin_mode = new int[0];
    SpinThread spin_thread;

    private int n_knobs = 0;
    private int k_gap = 3;
    private int k_top = 3;
    private int k_left = 3;
    private int k_diam = 48;
    private int k_mid = 24;
    private int ticks_size;
    private int dimple_size;
    private int dimple_off;
    private double k_rad = (double) k_diam * 0.5;
    

    private boolean has_updated;
    private double updated_value;
    private int updated_knob;
    
    private String[] labels;    // can be null for 0 labels, otherwise should be at least n_knobs long
    private int label_height = 0;
    private int n_labels = 0;

    private Image   knob_image;
    private Image[] knob_spin_image;

    private Vector klv;   // vector of KnobListeners
    private Vector kmlv;  // vector of KnobMotionListeners
    
    private double[] k_val = null;   // data array to display and modify
}

