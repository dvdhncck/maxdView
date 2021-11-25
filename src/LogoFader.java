import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import javax.swing.*;

public class LogoFader
{
    // ------------------------------------------------------

    private long start_time;
    private void startTimer()
    {
	start_time = (new java.util.Date()).getTime();
    }
    private long elapsedTime()
    {
	return (new java.util.Date()).getTime() - start_time;
    }
    private void reportElapsedTime()
    {
	long now = (new java.util.Date()).getTime();
	System.out.println( ( now - start_time )  + "ms" );
    }

    // ------------------------------------------------------

    public LogoFader( JPanel panel_, int xp_, int yp_, Color init_col_ )
    {
	panel = panel_;
	x_pos = xp_;
	y_pos = yp_;
	init_col = init_col_;
    }

    private void init()
    {
	getNativeFormat();

	loadBackground();

	int red_i = init_col.getRed();
	int grn_i = init_col.getGreen();
	int blu_i = init_col.getBlue();

	top_bg_col = new MovingColor(true,  red_i, grn_i, blu_i, 0,0,0);
	top_fg_col = new MovingColor(false, red_i, grn_i, blu_i, 255,255,255);
	bot_bg_col = new MovingColor(true,  red_i, grn_i, blu_i, 0,0,0);
	bot_fg_col = new MovingColor(false, red_i, grn_i, blu_i, 255,255,255);
	
	buf_data = new int[s_width * s_height];

	mem_image = new MemoryImageSource(s_width,s_height,buf_data,0,s_width);
	mem_image.setAnimated(true);
	buf = panel.createImage( mem_image );
    }

    // ------------------------------------------------------

    public void start()
    {
	init();

	update_thread = new UpdateThread();
	
	update_thread.start();
	
    }

    public void stop()
    {
	update_thread.stopping = true;
    }


    // ------------------------------------------------------

    private void loadBackground()
    {
	back = Toolkit.getDefaultToolkit().createImage("maxdView.jpg");

	MediaTracker tracker = new MediaTracker(panel);
	tracker.addImage(back, 0);
	try 
	{
	    tracker.waitForID(0);
	}
	catch (InterruptedException e) 
	{
	    System.err.println("interrupted waiting for load");
	    return;
        }

	s_width  = back.getWidth(null);
	s_height = back.getHeight(null);
	
	back_data = new int[s_width * s_height];

	PixelGrabber pg = new PixelGrabber(back, 0, 0, s_width, s_height, back_data, 0, s_width);

	try 
	{
            pg.grabPixels();
        } 
	catch (InterruptedException e) 
	{
            System.err.println("interrupted waiting for pixels!");
            return;
        }
	catch (Exception ee) 
	{
            ee.printStackTrace();
        }
	
	if ((pg.getStatus() & ImageObserver.ABORT) != 0) 
	{
            System.err.println("image fetch aborted or errored");
            return;
        }

	System.out.println("loaded " + s_width + "x" + s_height );
    }
    
    private void getNativeFormat()
    {
	ColorModel cm = Toolkit.getDefaultToolkit().getColorModel();
	ColorSpace cs = cm.getColorSpace();

	if(cs.getType() == ColorSpace.TYPE_RGB)
	    System.out.println("RGB");
	
	if(cm instanceof DirectColorModel)
	{
	    System.out.println("direct");

	    System.out.println("r=" + ((DirectColorModel)cm).getRedMask());
	    System.out.println("g=" + ((DirectColorModel)cm).getGreenMask());
	    System.out.println("b=" + ((DirectColorModel)cm).getBlueMask());
	}
	if(cm instanceof PackedColorModel)
	{
	    System.out.println("packed");
	    
	    int[] masks = ((PackedColorModel) cm).getMasks();
	    for(int m=0; m < masks.length; m++)
		System.out.println(masks[m]);
	}
	if(cm instanceof IndexColorModel)
	    System.out.println("index");

	System.out.println("bpp:" + cm.getPixelSize());
	System.out.println("alpha?:" + cm.hasAlpha());
	
    }



    private class UpdateThread extends Thread
    {
	public void run()
	{
	    stopping = false;

	    while( !stopped )
	    {
		if(update(stopping) == true)
		    stopped = true;
	    }

	    System.out.println( "UpdateThread has finished....");

	    System.out.println( "top bg = " + top_bg_col);
	    System.out.println( "top fg = " + top_fg_col);
	    System.out.println( "bot bg = " + bot_bg_col);
	    System.out.println( "bot fg = " + bot_fg_col);

	}

	private boolean stopping;
	private boolean stopped;
    }

    private boolean update(boolean is_stopping)
    {
	// System.out.println( "tick....");

	startTimer();
	
	top_bg_col.update(is_stopping);
	top_fg_col.update(is_stopping);
	bot_bg_col.update(is_stopping);
	bot_fg_col.update(is_stopping);
	
	// System.out.println( top_bg_col + "\t" + top_fg_col + "\t" + bot_bg_col + "\t" + bot_fg_col );

	bitblit();
	
	long pause = 100 - elapsedTime();
	
	if(pause > 0)
	{
	    // System.out.println("sleep for " + pause +  "ms");
	    
	    try 
	    {
		Thread.sleep(pause);
	    } 
	    catch (InterruptedException e) 
	    {
		System.err.println("interrupted whilst sleeping");
		return false;
	    }
	}

	boolean finished = is_stopping ? (top_bg_col.done() && top_fg_col.done() && bot_bg_col.done() && bot_fg_col.done()) : false;
	
	return finished;
    }

    private void bitblit()
    {
	try
	{
	    int d = 0;
	    int s = 0;
	    
	    double h_f = .0;
	    double h_f_d = 1.0 / (double)(s_height);
	    
	    for(int h=0; h < s_height; h++)
	    {
		double h_f_inv = 1.0 - h_f;

		double interp_bg_r = (h_f * top_bg_col.red) + (h_f_inv * bot_bg_col.red);
		double interp_bg_g = (h_f * top_bg_col.grn) + (h_f_inv * bot_bg_col.grn);
		double interp_bg_b = (h_f * top_bg_col.blu) + (h_f_inv * bot_bg_col.blu);

		double interp_fg_r = (h_f * top_fg_col.red) + (h_f_inv * bot_fg_col.red);
		double interp_fg_g = (h_f * top_fg_col.grn) + (h_f_inv * bot_fg_col.grn);
		double interp_fg_b = (h_f * top_fg_col.blu) + (h_f_inv * bot_fg_col.blu);

		double i_f = .0;
		double i_f_d = 1.0 / 255.0;

		for(int i=0; i < 256; i++)
		{
		    double i_f_inv = 1.0 - i_f;
		    
		    fade_r[i] = (int)((i_f * interp_fg_r) + (i_f_inv * interp_bg_r));
		    fade_g[i] = (int)((i_f * interp_fg_g) + (i_f_inv * interp_bg_g));
		    fade_b[i] = (int)((i_f * interp_fg_b) + (i_f_inv * interp_bg_b));

		    i_f += i_f_d;
		}

		for(int w=0; w < s_width; w++)
		{
		    int cur = back_data[s++];

		    int b0 = (cur & 0xff);
		    int b1 = ((cur >> 8) & 0xff);
		    int b2 = ((cur >> 16) & 0xff);

		    // int b3 = ((cur >> 24) & 0xff);
		    //buf_data[d++] = (fade[b3] << 24) | (fade[b2] << 16) | (fade[b1] << 8) | (fade[b0]);

		    buf_data[d++] = (0xff << 24) | (fade_r[b2] << 16) | (fade_g[b1] << 8) | (fade_b[b0]);
		    
		}

		h_f += h_f_d;

	    }

	    // takes about 170ms for 256x256 using this version
	    //
	    mem_image.newPixels(0,0,s_width,s_height);

	    panel.getGraphics().drawImage( buf, x_pos, y_pos, null );

	    // takes about 700ms using this version
	    /*
	    bbuf.setRGB( 0, 0, s_width, s_height, buf_data, 0, s_width );
	    contents.getGraphics().drawImage( bbuf, 0, 0, null );
	    */	    
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    System.out.println("bad array");
	}
    }


    // =================================================================================

    private int s_width;
    private int s_height;
    
    private JPanel panel;
    private int x_pos, y_pos;

    private Color init_col;
    private MovingColor top_bg_col, top_fg_col, bot_bg_col, bot_fg_col;

    private int[] fade_r = new int[256];
    private int[] fade_g = new int[256];
    private int[] fade_b = new int[256];

    private UpdateThread update_thread;

    private MemoryImageSource mem_image;
    private Image buf;
    private Image back;

    private int[] back_data;
    private int[] buf_data;


    // ============================================================================================================

    private class MovingColor
    {
	public MovingColor(boolean dark_, int i_red, int i_grn, int i_blu, int r_red_, int r_grn_, int r_blu_)
	{
	    ticks_to_go = pickTicks() / 2;

	    dark = dark_;

	    red_d = (double) i_red;
	    grn_d = (double) i_grn;
	    blu_d = (double) i_blu;

	    r_red = r_red_;
	    r_grn = r_grn_;
	    r_blu = r_blu_;

	    setupDeltas();
	}

	
	public boolean done()
	{
	    return ( (red == r_red) && (grn == r_grn)  && (blu == r_blu) );
	}
	
	public String toString()
	{
	    return red + "," + grn + "," + blu;
	}

	public void update(boolean use_real)
	{
	    if(--ticks_to_go < 0)
	    {
		ticks_to_go = pickTicks();

		//scale = .0;
		//scale_d = 1.0 / (double) ticks_to_go;

		if(use_real)
		{
		    t_red = r_red;
		    t_grn = r_grn;
		    t_blu = r_blu;
		}
		else
		{
		    float h = (float) Math.random();
		    float s = (float) ( dark ? ((Math.random() * 0.7) + 0.3) : ((Math.random() * 0.1) + 0.2) );
		    float b = (float) ( dark ? ((Math.random() * 0.2) + 0.3) : ((Math.random() * 0.5) + 0.5) );
		    Color tmp = new Color( Color.HSBtoRGB( h, s, b ) );
		    
		    t_red = tmp.getRed();
		    t_grn = tmp.getGreen();
		    t_blu = tmp.getBlue();
		}

		setupDeltas();
	    }
	    else
	    {
		red_d += d_red;
		red = (int)red_d;
		if(red > 255) { red = 255; } else if(red < 0) { red = 0; }

		grn_d += d_grn;
		grn = (int)grn_d;
		if(grn > 255)  {grn = 255; } else if(grn < 0) { grn = 0; }
		
		blu_d += d_blu;
		blu = (int)blu_d;
		if(blu > 255) { blu = 255; } else if(blu < 0) { blu = 0; }
	    }
	   
	}

	private int pickTicks() { return 20 + (int) (Math.random() * 40); }

	private void setupDeltas()
	{
	    double ttg = (double) (ticks_to_go-1);
	    
	    d_red = (double)(t_red - red) / ttg;
	    d_grn = (double)(t_grn - grn) / ttg;
	    d_blu = (double)(t_blu - blu) / ttg;
	}
	


	public int red, grn, blu;           // current colour
	public double red_d, grn_d, blu_d;  // current colour (as a double)

	private int t_red, t_grn, t_blu;    // target
	private int r_red, r_grn, r_blu;    // 'real' colour
	private double d_red, d_grn, d_blu; // delta from current to target

	private boolean dark;

	private int ticks_to_go;

    }

    // =========================================================================================================================================
    // test harness
    // =========================================================================================================================================

    public static void main(String[] args) 
    {
       	JFrame frame = new JFrame("LogoFader");
	
	JPanel contents = new JPanel();
	contents.setBackground( new Color(200,100,200) );
	contents.setPreferredSize(new Dimension(500, 200));

	LogoFader lf = new LogoFader( contents, 30, 30, new Color(200,100,200) );

	frame.getContentPane().add(contents, BorderLayout.CENTER);
	
	frame.pack();
	frame.setVisible(true);

	System.out.println( "starting...." );

	lf.start();
	
	int count = 0;
	while(++count < 800)
	{
	    try 
	    {
		Thread.sleep(50);
	    } 
	    catch (InterruptedException e) 
	    {
	    } 
	}

	System.out.println( "stopping...." );

	lf.stop();
	
    }



}
