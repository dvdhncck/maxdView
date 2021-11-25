import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class ProgressOMeter extends Thread
{
    Dimension size = null;

    public ProgressOMeter(String str)
    {
	this(str, false);
    }

    public ProgressOMeter(String str, int n_message_lines_)
    {
	this(str, n_message_lines_, false);
    }

    public ProgressOMeter(String str, boolean show_percent_)
    {
	this(str, 1, show_percent_);
    }
    
    public ProgressOMeter(String str, int n_message_lines_, boolean show_percent_)
    {
	running = false;
	title_label = str;
	n_message_lines = n_message_lines_;
	label = null;
	show_percent = show_percent_;

	frame = new JFrame("Progress");

	if(logo_image != null)
	    frame.setIconImage( logo_image );

	size = new Dimension(300, 100 + (n_message_lines * 20));

	outer_panel = new JPanel();
	frame.getContentPane().add(outer_panel);

	GridBagLayout gridbag = new GridBagLayout();
	outer_panel.setLayout(gridbag);
	outer_panel.setMinimumSize(size);
	outer_panel.setPreferredSize(size);

	{
	    draw_panel = new JPanel();
	    draw_panel.setPreferredSize(new Dimension(200, 20));
	    outer_panel.add(draw_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    //c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(draw_panel, c);
	}

	label = new JLabel[n_message_lines];
	for(int i=0; i< n_message_lines; i++)
	{
	    label[i] = new JLabel();
	    if(i == 0)
		label[i].setText(title_label);
	    outer_panel.add(label[i]);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = i+1;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    //c.anchor = GridBagConstraints.CENTER;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(label[i], c);
	}

	if(show_percent)
        {
	    jsl = new JSlider(JScrollBar.HORIZONTAL, 0, 100, 0);
	    jsl.setPreferredSize(new Dimension(200, 20));
	    jsl.setEnabled(false);
	    outer_panel.add(jsl);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = n_message_lines+2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsl, c);
	}

	{
	    cancel_button = new JButton("Cancel");
	    outer_panel.add(cancel_button);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = n_message_lines+3;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    //c.anchor = GridBagConstraints.CENTER;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(cancel_button, c);

	    cancel_button.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			stopIt();
			cancel_al.actionPerformed(e);
		    }
		});
	    
	    
	}


	//frame.getContentPane().setLayout(new BorderLayout());
	
	led_val = new int [leds];
	for(int l=0;l < leds; l++)
	{
	    led_val[l] = 0;
	}

	led_col = new Color[cols];
	led_col[0] = new Color(120, 75, 75);
	led_col[1] = new Color(135, 75, 75);
	led_col[2] = new Color(157, 75, 75);
	led_col[3] = new Color(183, 75, 75);
	led_col[4] = new Color(208, 75, 75);
	led_col[5] = new Color(255, 75, 75);

	frame.pack();

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    running = false;
		}
	    });
	
	Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
	
	frame.setLocation((int)((screen_size.getWidth() - size.getWidth()) / 2.0),
			  (int)((screen_size.getHeight() - (size.getHeight()/2)) / 2.0));
	    
		
    }

    public synchronized void startIt()
    {
	//System.out.println("ProgressOMeter started");
	if(!running)
	{
	    running = true;
	    stopped = false;

	    // launch a window
	    
	    //if( frame == null )
	    {
		if(cancel_al != null)
		{
		    cancel_button.setVisible(true);
		    //System.out.println("cancel enabled");
		}
		else
		{
		    cancel_button.setVisible(false);
		    //System.out.println("cancel disabled");
		}
		
	    }

	    frame.setVisible(true);
	    
	    start();
	}
    }

    public synchronized void stopIt()
    {
	stopped = false;

	running = false;

	while(!stopped)
	{
	    try 
	    {
		Thread.sleep(250);
	    }
	    catch (InterruptedException e)
	    {
		// the VM doesn't want us to sleep anymore,
		System.out.println("ProgressOMeter interrupted");
	    }
	}

	frame.setVisible(false);
	
	//System.out.println("ProgressOMeter stopped");
    }

    public void setMessage(String s)
    {
	if(label != null)
	    label[0].setText(s);
	//outer_panel.repaint();
	//outer_panel.updateUI();
    }

    public void setMessage(int message_number, String s)
    {
	if((s != null) && (label != null) && (message_number < label.length))
	    label[message_number].setText(s);
	//outer_panel.repaint();
	//outer_panel.updateUI();
    }

    public void setCancelAction(ActionListener al_)
    {
	cancel_al = al_;
	
	if(frame.isVisible())
	    cancel_button.setVisible(true);
    }

    public void setProgress(int p)
    {
	if(p < 100)
	    jsl.setValue(p);
	else
	    stopIt();
    }

    public void run()
    {
	while(running == true)
	{
	    //System.out.println("hello");

	    value += incr;
	    if(value == 0)
		incr = 1;
	    if(value == (leds-1))
		incr = -1;

	    Graphics g = draw_panel.getGraphics();

	    int w = draw_panel.getWidth();
	    int h = draw_panel.getHeight();

	    //g.setColor(Color.white);
	    //g.fillRect(0,0,w,h);
	    
	    int lw = w / leds;
	    int lh = (lw * 3) / 5;
	    int xp = (w - (lw * leds)) / 2;
	    int yp = (h - lh) / 2;

	    for(int l=0; l< leds; l++)
	    {
		if(l == value)
		    led_val[l] = (cols-1);
		else
		    if(led_val[l] > 0)
			led_val[l]--;

		g.setColor(led_col[led_val[l]]);
		
		g.fillRect(xp, yp, lw-2, lh);

		//g.fillRoundRect(xp, yp, lw-2, lh, 6, 6);

		xp += lw;
	    }

	    //jsb.setValue(value);

	    // this is really annoying as it keeps grabbing the focus

	    // frame.toFront();

	    try 
	    {
		Thread.sleep(250);
	    }
	    catch (InterruptedException e)
	    {
		// the VM doesn't want us to sleep anymore,
		System.out.println("ProgressOMeter interrupted");
	    }

	}

	stopped = true;
    }

    private boolean running = false;
    private boolean stopped = false;

    private boolean show_percent;
    
    private ActionListener cancel_al = null;;
    private JButton cancel_button;

    private final int leds = 12;
    private final int cols = 6;

    private int xp = 0;
    private int yp = 0;

    private int[] led_val;
    private Color[] led_col;

    private int value = 0;
    private int incr = 1;

    private int n_message_lines;

    private String title_label;
    private JPanel draw_panel = null;
    private JPanel outer_panel = null;
    private JFrame frame = null;
    private JSlider jsl = null;
    private JLabel[] label = null;


    public static void setLogoImage( Image image_ ) { logo_image = image_; }

    private static Image logo_image = null;
}
