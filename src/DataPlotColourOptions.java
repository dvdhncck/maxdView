import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Arrays;
import java.util.Hashtable;

public class DataPlotColourOptions extends JFrame implements ExprData.ExprDataObserver
{
 
    public DataPlotColourOptions(maxdView m_viewer)
    {

	super("Colourisers");

	colours_options = new JPanel();

	addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	mview = m_viewer;
	dplot = mview.getDataPlot();
	edata = mview.getExprData();

	mview.decorateFrame(this);
	
	colours_options.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	colours_options.setPreferredSize(new Dimension(500,400));

	getContentPane().add(colours_options, BorderLayout.CENTER);

	GridBagLayout outer_gridbag = new GridBagLayout();
	colours_options.setLayout(outer_gridbag);

	int line = 0;

	// ---------------- --------------- --------------- 
	// all of the colourisers in a scroll pane

	{
	    colourisers_panel = new JPanel();
	    
	    addColouriserPanels();
	    
	    /*
	    colourisers_panel.setDropAction(new DragAndDropPanel.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			System.out.println("dropped");
		    }
		});
	    */

	    /*
	    colourisers_panel.setDragAction(new DragAndDropPanel.DragAction()
		{
		    public DragAndDropEntity getEntity(java.awt.dnd.DragGestureEvent event)
		    {
			Point pt = event.getDragOrigin();
			
			System.out.println("dragging at " + pt.getX() + "," + pt.getY());

			if(colouriser_by_panel != null)
			{
			    // which colouriser is under the mouse?
			    JPanel panel = (JPanel) getComponentAt( (int) pt.getX(), (int) pt.getY() );

			    Colouriser col = (Colouriser) (colouriser_by_panel.get(panel));
			    
			    if(col != null)
			    {
				DragAndDropEntity dnde =  DragAndDropEntity.createColouriserEntity(col);
				return dnde;
			    }
			}
			
			return null;
		    }
		});
	    */

	    JScrollPane jsp = new JScrollPane(colourisers_panel);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weighty = c.weightx = 5.0;
	    c.fill = GridBagConstraints.BOTH;
	    outer_gridbag.setConstraints(jsp, c);
	
	    colours_options.add(jsp);
	}


	// ---------------- --------------- --------------- 
	// Other Colours
	    
	{
	    JPanel other_col_options = new JPanel();
	    other_col_options.setBorder(BorderFactory.createEmptyBorder(0,0,20,0));
	    GridBagLayout gridbag = new GridBagLayout();
	    other_col_options.setLayout(gridbag);
	    {

		JLabel label = new JLabel("Background");
		other_col_options.add(label);
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.ipady = 10;
		gridbag.setConstraints(label, c);
	    }
		

	    {
		final JButton jb = new JButton();
		jb.setPreferredSize(new Dimension(100,20));
		jb.setMinimumSize(new Dimension(100,20));
		other_col_options.add(jb);

		jb.setBackground(mview.getBackgroundColour());
		    
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    Color newColor = JColorChooser.showDialog(DataPlotColourOptions.this,
								      "Choose Background Colour",
								      mview.getBackgroundColour());
			    if (newColor != null) 
			    {
				jb.setBackground(newColor);
				mview.setBackgroundColour(newColor);
			    }
			}
		    });
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(jb, c);
	    }    

	    {

		JLabel label = new JLabel("Text");
		other_col_options.add(label);
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.ipady = 10;
		gridbag.setConstraints(label, c);
	    }
		

	    {
		final JButton jb = new JButton();
		jb.setPreferredSize(new Dimension(100,20));
		jb.setMinimumSize(new Dimension(100,20));
		other_col_options.add(jb);

		jb.setBackground(mview.getTextColour());
		    
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     Color newColor = JColorChooser.showDialog(DataPlotColourOptions.this,
										       "Choose Text Colour",
										       mview.getTextColour());
					     if (newColor != null) 
					     {
						 jb.setBackground(newColor);
						 mview.setTextColour(newColor);
					     }
					 }
				     });
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(jb, c);
	    }    

	    {
		colours_options.add(other_col_options);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		c.weighty = 1.0;
		c.weightx = 5.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		outer_gridbag.setConstraints(other_col_options, c);
	    }

	}

	// ---------------- --------------- --------------- 
	// close/new/help buttons
	{
	    {
		JPanel buttons_panel = new JPanel();
		GridBagLayout inner_gridbag = new GridBagLayout();
		buttons_panel.setLayout(inner_gridbag);
		
		{   
		    final JButton jb = new JButton("Help");
		    buttons_panel.add(jb);
		    jb.addActionListener(new ActionListener() 
					 {
					     public void actionPerformed(ActionEvent e) 
					     {
						 mview.getHelpTopic("ViewerColours");
					     }
					 });
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 0;
		    c.weightx = 1.0;
		    inner_gridbag.setConstraints(jb, c);
		}
		{   
		    final JButton jb = new JButton("Add New");
		    buttons_panel.add(jb);
		    jb.setToolTipText("Add a new Colouriser");
		    jb.addActionListener(new ActionListener() 
					 {
					     public void actionPerformed(ActionEvent e) 
					     {
						 addNewColouriser();
					     }
					 });
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 0;
		    c.weightx = 1.0;
		    inner_gridbag.setConstraints(jb, c);
		}
		{   
		    final JButton jb = new JButton("Close");
		    buttons_panel.add(jb);
		    
		    jb.addActionListener(new ActionListener() 
					 {
					     public void actionPerformed(ActionEvent e) 
					     {
						 cleanUp();
					     }
					 });
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 2;
		    c.gridy = 0;
		    c.weightx = 1.0;
		    inner_gridbag.setConstraints(jb, c);
		}
		colours_options.add(buttons_panel);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		//c.weighty = 1.5;
		c.weightx = 5.0;
		//c.fill = GridBagConstraints.HORIZONTAL;
		outer_gridbag.setConstraints(buttons_panel, c);
	    }

	}

	pack();
	setVisible(true);
	edata.addObserver(this);
    }

    private void addColouriserPanels()
    {
	if( colourisers_panel==null)
	    return;

	//
	// build an array of Colourisers...
	//

	Colouriser[] colourisers = dplot.getColouriserArray();

	colourisers_panel.removeAll();

	colouriser_by_panel = new Hashtable();

	GridBagLayout gridbag = new GridBagLayout();
	colourisers_panel.setLayout(gridbag);

	/*
	Array.sort(colourisers, new Compartor()
	    {
		public int compare(Object one, Object two) {  return 0; }
		public boolean equals(Object com) { return false; }
	    });
	*/

	int line = 0;
	
	Dimension fillsize = new Dimension(16,16);

	for(int c=0; c < colourisers.length; c++)
	{
	    {
		JPanel infowrap = new JPanel();
		infowrap.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
		GridBagLayout iwbag = new GridBagLayout();
		infowrap.setLayout(iwbag);
		
		/*
		final JTextField name_jtf = new JTextField(10);
		name_jtf.setText(colourisers[c].getName());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = gbc.weightx = 5.0;
		gbc.fill = GridBagConstraints.BOTH;
		iwbag.setConstraints(name_jtf, gbc);
		infowrap.add(name_jtf);
		*/


		// try to make the buttons as small as possible....
		//

		Insets ins = new Insets(1,4,1,4);

		JButton jb = new JButton("Delete");
		jb.setToolTipText("Remove this Colouriser");
		Font f = jb.getFont();
		Font small_font = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
		jb.setFont(small_font);
		jb.setMargin(ins);
		jb.addActionListener(new ColouriserControlListener(0, colourisers[c]));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		//gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		iwbag.setConstraints(jb, gbc);
		infowrap.add(jb);


		jb = new JButton("Rename");
		jb.setToolTipText("Rename this Colouriser");
		jb.setMargin(ins);
		jb.setFont(small_font);
		jb.addActionListener(new ColouriserControlListener(1, colourisers[c]));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		//gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		iwbag.setConstraints(jb, gbc);
		infowrap.add(jb);

		jb = new JButton("Clone");
		jb.setToolTipText("Make a copy of this Colouriser");
		jb.setMargin(ins);
		jb.addActionListener(new ColouriserControlListener(2, colourisers[c]));
		jb.setFont(small_font);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		//gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		iwbag.setConstraints(jb, gbc);
		infowrap.add(jb);

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = line;
		gbc.weighty = 1.0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(infowrap, gbc);
		colourisers_panel.add(infowrap);
	    }
	    
	    {
		JPanel col_p =  colourisers[c].getEditorPanel(mview, edata);
		
		colouriser_by_panel.put(col_p, colourisers[c]);

		Color title_colour = new JLabel().getForeground().brighter();
		TitledBorder title = BorderFactory.createTitledBorder(" " + colourisers[c].getName() + " ");
		title.setTitleColor(title_colour);
		col_p.setBorder(title);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = line++;
		gbc.weighty = 1.0;
		gbc.weightx = 5.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(col_p, gbc);
		colourisers_panel.add(col_p);
	    }

	    {
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = line++;
		//gbc.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(filler, gbc);
		colourisers_panel.add(filler);
	    }

	}
	
	colourisers_panel.updateUI();

	System.gc();
    }

   
    class ColouriserControlListener implements ActionListener
    {
	public ColouriserControlListener(int com, Colouriser col)
	{
	    command = com;
	    colouriser = col;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    switch(command)
	    {
	    case 0:
		dplot.deleteColouriser(colouriser);
		addColouriserPanels();
		break;
	    case 1:
		dplot.renameColouriser(colouriser);
		addColouriserPanels();
		break;
	    case 2:
		dplot.cloneColouriser(colouriser);
		addColouriserPanels();
		break;
	    }
	}

	private int command;
	private Colouriser colouriser;
    }
    
    //
    // ---------------- --------------- --------------- 
    // 
    // ---------------- --------------- --------------- 
    // 
    

    
    public void cleanUp()
    {
	edata.removeObserver(this);
	setVisible(false);
    }


    public void addNewColouriser()
    {
	try
	{
	    String[] col_opts = { "Ramped", "Discrete", "Blender", "Equalising" };

	    int nc = mview.getChoice("Add what sort of colouriser?", col_opts);

	    switch(nc)
	    {
	    case 0:
		dplot.addColouriser(new RampedColouriser("(new Ramped)", 64, Color.green, Color.white, Color.red));
		break;
	    case 1:
		dplot.addColouriser(new DiscreteColouriser("(new Discrete)", 3));
		break;
	    case 2:
		dplot.addColouriser(new BlenderColouriser("(new Blender)", 100, 0));
		break;
	    case 3:
		dplot.addColouriser(new EqualisingColouriser("(new Equalising)"));
		break;
	    }
	    
	    addColouriserPanels();
	}
	catch(UserInputCancelled e)
	{
	}
    }


    /*
    private void setMinMaxLabels()
    {
	final int max_num_len = 8;

	String min_v = String.valueOf(mview.getExprData().getMinEValue());
	if(min_v.length() > max_num_len)
	    min_v = min_v.substring(0,max_num_len);
	min_exp_jtf.setText( " " + min_v  + " " );
	
	String max_v = String.valueOf(mview.getExprData().getMaxEValue());
	if(max_v.length() > max_num_len)
	    max_v = max_v.substring(0,max_num_len);
	max_exp_jtf.setText( " " + max_v + "  " );
	
	String min_e = String.valueOf(mview.getExprData().getMinErrorValue());
	if(min_e.length() > max_num_len)
	    min_e = min_e.substring(0,max_num_len);
	min_err_jtf.setText( " " + min_e + "  " );
	
	String max_e = String.valueOf(mview.getExprData().getMaxErrorValue());
	if(max_e.length() > max_num_len)
	    max_e = max_e.substring(0,max_num_len);
	max_err_jtf.setText( " " + max_e + "  " );
    }
    */

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

    private JPanel colours_options;
    private JPanel colourisers_panel;

    private Hashtable colouriser_by_panel;

    private RampEditor expr_pos_re, expr_neg_re;

    private RampEditor error_pos_re, error_neg_re;

    private JTextField[] prob_jtf;

    private JTextField min_exp_jtf;
    private JTextField max_exp_jtf;
    private JTextField max_err_jtf;
    private JTextField min_err_jtf;

    // handles the Probability colour chooser buttons
    //
    /*
    class ProbColActionListener implements ActionListener
    {
	public ProbColActionListener(int which_col) 
	{ super();
	  c = which_col;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    Color newColor = JColorChooser.showDialog(DataPlotColourOptions.this,
						      "Choose Color",
						      dplot.getProbColour(c));
	    if (newColor != null) 
	    {
		JButton jb = (JButton) e.getSource();
		jb.setBackground(newColor);
		dplot.setProbColour(c, newColor);
	    }
	}
	
	private int c;
    }
    */

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	// the colours vector have already been changed
	//
	switch(due.event)
	{
	case ExprData.RangeChanged:
	    System.out.println("colours need repainting");
	    //setMinMaxLabels();
	    colours_options.repaint();
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.RangeChanged:
	    System.out.println("colours need repainting");
	    //setMinMaxLabels();
	    colours_options.repaint();
	    break;
	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


}
