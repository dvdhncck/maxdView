import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.io.File;
import java.lang.reflect.*;

public class SVD implements ExprData.ExprDataObserver,Plugin
{
    static final String plugin_name = "SVD v1.1";

    public SVD(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(frame != null)
	{
	    mview.putBooleanProperty("SVD.use_filter", apply_filter_jchkb.isSelected( ) );
	    mview.putBooleanProperty("SVD.do_pca", do_pca_jrb.isSelected() );
	    mview.putBooleanProperty("SVD.do_svd", do_svd_jrb.isSelected() );

	    frame.setVisible(false);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	if( loadSVD() == false )
	    return;

	addComponents();
	edata.addObserver(this);
	frame.pack();
	frame.setVisible(true);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("SVD", "transform", 
					 "Singular Value Decomposition (for PCA)", 
					 "Developed with the kind assistance of Dr. David Hoyle",
					 1, 1, 0);
	return pinf;
    }
    
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];

	com[0] = new PluginCommand("start", null);

	com[1] = new PluginCommand("stop", null);
	
	String[] args = new String[] 
	{ 
	    // name             // type                  //default   // flag   // comment
	    "select",          "string",                 "all",      "m",     "either 'all' or 'list'",
	    "measurements",    "measurement_list",       "",         "",      "used when 'select'='list'",
	    "apply_filter",    "boolean",           	 "false",    "",      "", 
	    "direction",       "string",            	 "genes",    "m",     "either 'genes' or 'arrays'",
	    "n_components",    "integer",           	 "1",        "m",     "number of components to project",
	    "new_name_prefix", "string",            	 "PCA_",     "",      "prefix for names of new measurement"
	};

	com[2] = new PluginCommand("project", args);

	return com;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    {
	if(name.equals("stop"))
	{
	    if(frame != null)
		cleanUp();
	}
	else
	{
	    boolean started_this_time = false;

	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    if(name.equals("project"))
	    {
		String sel = mview.getPluginStringArg("select", args, "all");

		if(sel.startsWith("li"))
		{
		    String[] m_names = mview.getPluginMeasurementListArg( "measurements", args, null );
		    
		    meas_list.selectItems( m_names);
		}
		else
		{
		    meas_list.selectAll();
		}
		
		apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    
		if( doSVD() )
		{
		    count_jtf.setText( mview.getPluginArg("n_components", args, "1") );

		    String prefix_default = do_pca_jrb.isSelected() ? "PCA_" : "SVD_";

		    name_jtf.setText( mview.getPluginArg("new_name_prefix", args, prefix_default ) );

		    String dir =  mview.getPluginStringArg("direction", args, "genes");
		    
		    if(dir.startsWith("arr"))
		    {
			projectOntoEigenArrays();
		    }
		    else
		    {
			projectOntoEigenGenes();
		    }
		}
		else
		{
		    mview.alertMessage("Unable to perform projection");
		}

		if(started_this_time)
		    cleanUp();
	    }
	}
	
	if(done != null)
	    done.signal();
 
    } 

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
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	// reset the label?
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    meas_list.setModel(new MeasListModel());
	    // updateDisplay();
	    
	    break;
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private int mode = 0;

    private void addComponents()
    {
	frame = new JFrame("SVD");
	
	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	JPanel o_panel = new JPanel();
	o_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout o_gridbag = new GridBagLayout();
	o_panel.setLayout(o_gridbag);

	final JTabbedPane tabbed = new JTabbedPane();
	tabbed.setEnabled(false);

	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	o_gridbag.setConstraints(tabbed, c);
	o_panel.add(tabbed);

	{
	    // the measurement picking panel
	    
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    DragAndDropPanel panel = new DragAndDropPanel();
	    
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    
	    panel.setLayout(gridbag);
	    panel.setPreferredSize(new Dimension(450, 300));
	    
	    int n_cols = 2;
	    int line = 0;
	    
	    meas_list = new DragAndDropList();
	    JScrollPane jsp = new JScrollPane(meas_list);
	    meas_list.setModel(new MeasListModel());
	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 3;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    line++;

	    apply_filter_jchkb = new JCheckBox("Apply filter");

	    apply_filter_jchkb.setSelected( mview.getBooleanProperty("SVD.use_filter", false) );

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(apply_filter_jchkb, c);
	    panel.add(apply_filter_jchkb);

	    do_pca_jrb = new JRadioButton("PCA of covariance matrix");
	    do_pca_jrb.setToolTipText("Calculates principal components of covariance matrix");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(do_pca_jrb, c);
	    panel.add(do_pca_jrb);

	    do_svd_jrb = new JRadioButton("SVD of Wishart matrix");
	    do_svd_jrb.setToolTipText("Calculates SVD of data matrix, data is not mean centered");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(do_svd_jrb, c);
	    panel.add(do_svd_jrb);

	    ButtonGroup bg = new ButtonGroup();
	    bg.add(do_svd_jrb);
	    bg.add(do_pca_jrb);

	    
	    do_pca_jrb.setSelected( mview.getBooleanProperty("SVD.do_pca", true) );
	    do_svd_jrb.setSelected( mview.getBooleanProperty("SVD.do_svd", false) );
	    
	    tabbed.add(" Pick Measurements ", panel);
	    
	}
	    
	// ===== View Eigenvectors ======================================================

	{
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    JPanel panel = new JPanel();

	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    panel.setLayout(gridbag);
	    
	    JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    
	    eigenval_graph = new GraphPanel();
	    eigenval_graph.getContext().setBackgroundColour( mview.getBackgroundColour() );
	    eigenval_graph.getContext().setForegroundColour( mview.getTextColour() );
	    
	    wachter_graph  = new GraphPanel();
	    wachter_graph.getContext().setBackgroundColour( mview.getBackgroundColour() );
	    wachter_graph.getContext().setForegroundColour( mview.getTextColour() );

	    jsp.setLeftComponent( eigenval_graph );
	    jsp.setRightComponent( wachter_graph );

	    c = new GridBagConstraints();
	    c.weightx = c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints( jsp, c);
	    panel.add( jsp );

	    tabbed.add(" Preview Results ", panel);
	}

	// ======== Project Data =========================================================
	
	{
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    JPanel panel = new JPanel();

	    tabbed.add(" Project Data ", panel);

	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    
	    panel.setLayout(gridbag);

	    JLabel label = new JLabel("Number of principal components ");
	    c.anchor = GridBagConstraints.EAST;
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);

	    count_jtf =  new JTextField(10);
	    count_jtf.setText("0");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints( count_jtf, c);
	    panel.add( count_jtf);

	    label = new JLabel("New name prefix ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    panel.add(label);

	    name_jtf =  new JTextField(10);
	    name_jtf.setText("PCA_");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints( name_jtf, c);
	    panel.add( name_jtf);

	    final JButton project_g_jb = new JButton("Project onto Genes");
	    final JButton project_a_jb = new JButton("Project onto Arrays");
	    
	    project_g_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			project_g_jb.setEnabled(false);
			project_a_jb.setEnabled(false);
			projectOntoEigenGenes();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.weighty = 1.0;
	    c.anchor= GridBagConstraints.SOUTH;
	    c.gridwidth = 2;
	    gridbag.setConstraints( project_g_jb, c);
	    panel.add( project_g_jb );
	    
	    label = new JLabel("(this will generate new Measurements)");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.anchor= GridBagConstraints.NORTH;
	    //c.weighty = .1;
	    c.gridx = 0;
	    c.gridy = 3;
	    c.gridwidth = 2;
	    gridbag.setConstraints(label, c);
	    panel.add( label );
	   

	    project_a_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			project_g_jb.setEnabled(false);
			project_a_jb.setEnabled(false);
			projectOntoEigenArrays();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 4;
	    c.weighty = 1.0;
	    c.anchor= GridBagConstraints.SOUTH;
	    c.gridwidth = 2;
	    gridbag.setConstraints( project_a_jb, c);
	    panel.add( project_a_jb );
	    
	    label = new JLabel("(this will generate new Spots)");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.anchor= GridBagConstraints.NORTH;
	    c.weighty = .1;
	    c.gridx = 0;
	    c.gridy = 5;
	    c.gridwidth = 2;
	    gridbag.setConstraints(label, c);
	    panel.add( label );
	}
	
	// ======= buttons =============================================================

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    wrapper.setLayout(w_gridbag);
	    final JButton nbutton  = new JButton("Next");
	    final JButton bbutton  = new JButton("Back");
	    bbutton.setEnabled(false);

	    {
		wrapper.add(bbutton);
		
		bbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    nbutton.setEnabled(true);
			    if(mode == 2)
			    {
				// jump back from the 'project' panel to the 'pick' panel
				// because the data will have changed....
				meas_list.setModel(new MeasListModel());
				mode = 0;
			    }
			    else
			    {
				if(--mode <= 0)
				{
				    mode = 0;
				    bbutton.setEnabled(false);
				}
				if(mode == 1)
				{
				    if(doSVD() == false)
				    {
					mode = 0;
					// tabbed.setSelectedIndex(mode);
				    }
				}
			    }

			    tabbed.setSelectedIndex(mode);
			    
			    //if(mode == 0)
				//doNothing...();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(bbutton, c);
	    }
	    {
		wrapper.add(nbutton);
		
		nbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    bbutton.setEnabled(true);
			    if(++mode >= 2)
			    {
				mode = 2;
				nbutton.setEnabled(false);
			    }
			    if(mode == 1)
			    {
				if(doSVD() == false)
				{
				    mode = 0;
				}
			    }
			    
			    tabbed.setSelectedIndex(mode);

			    if(mode == 2)
				updateProjectOpts();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(nbutton, c);
	    }
	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
						   
	    }
	    {
		JButton button = new JButton("Close");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cleanUp();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("SVD", "SVD");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    o_gridbag.setConstraints(wrapper, c);
	    o_panel.add(wrapper);
	    
	}

	frame.getContentPane().add(o_panel);
    }

    public class MeasListModel extends DefaultListModel
    {
	public Object getElementAt(int index) 
	{
	    return edata.getMeasurementName( edata.getMeasurementAtIndex(index) );
	}
	public int getSize() 
	{
	    return edata.getNumMeasurements();
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  projectData
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void updateProjectOpts()
    {
	if(eigen_values != null)
	{
	    double max_g = .0;
	    int    max_g_p = 0;

	    for(int p=1; p < eigen_values.length; p++)
	    {
		double p_g = eigen_values[p-1] - eigen_values[p];
		if(p_g > max_g)
		{
		    max_g = p_g;
		    max_g_p = p;
		}
	    }
	    if(max_g_p < 1)
		max_g_p = 1;

	    count_jtf.setText(String.valueOf(max_g_p));
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    

 

    private boolean doSVDusingSVDCMP(double[][] data, double[][] cov)
    {
	try
	{
	    Svdcmp svd = new Svdcmp(cov);
	    
	    svd.orderEigenvalues();
	   


	    // get eigenvalues....

	    eigen_values = svd.getEigenvalues();


	    

	    {
		eigenval_graph.getContext().removeAllPlots();
		eigenval_graph.getContext().setTitle( "Eigenvalues" );
				
		double[] x_axis = new double[ eigen_values.length ];
		for(int x=0; x < eigen_values.length; x++)
		    x_axis[x] = x;

		//for(int e=0; e < eigen_values.length; e++)
		//    System.out.println( e + "=" + eigen_values[e] );

		eigenval_graph.getHorizontalAxis().setTicks( x_axis );
		eigenval_graph.getHorizontalAxis().setTickSigDigits( 0 );
		eigenval_graph.getHorizontalAxis().setMouseTracking( false );

		eigenval_graph.getContext().addBarChart( x_axis, eigen_values, mview.getTextColour().brighter() );
	    }


	    
	    {
		wachter_graph.getContext().removeAllPlots();
		wachter_graph.getContext().setTitle( "Wachter Plot" );
		
		double[][] w_data = new WachterPlot().doWachter( eigen_values, data[0].length, data.length );

		
		//for(int e=0; e < w_data[1].length; e++)
		//    System.out.println( e + "=" + w_data[1][e] );
		
		wachter_graph.getContext().addScatterPlot( w_data[0], w_data[1], 
							   mview.getTextColour().brighter(), 
							   GraphPlot.CIRCLE_GLYPH );


		double[] y_axis = new double[ w_data[0].length ];

		for(int x=0; x < w_data[0].length; x++)
		    y_axis[x] = w_data[0][x];

		wachter_graph.getContext().addLinePlot( w_data[0], y_axis, 
							Color.black );


	    }

	    // get V   (eigengenes)
	    
	    // (transpose to match Bottstein & Brown notation)

	    eigen_genes = svd.getV();

	    // get eigenarrays 

	    // covariance e.vals are the squares of the e.vals of the actual data

	    for(int e=0; e < eigen_values.length; e++)
		eigen_values[e] = Math.sqrt(eigen_values[e]);

	    eigen_arrays = new double[data[0].length][eigen_genes.length];
            double[] norm = new double[eigen_genes.length];
            for( int i = 0; i < norm.length; i++ )
		{
		    norm[i] = 0.0;
                }
	    for( int i = 0; i < data[0].length; i++ )
		{
		    for( int j = 0; j < eigen_genes.length; j++ )
			{
			    eigen_arrays[i][j] = 0.0;
			    for( int k = 0; k < eigen_genes.length; k++ )
				{
				    //eigen_arrays[i][j] += ( data[k][i] * eigen_genes[k][j] / eigen_values[j] );
				    if( center_data_matrix )
				    {
					eigen_arrays[i][j] += ( (data[k][i] - means[i]) * eigen_genes[k][j] );
				    }
				    else
				    {
					eigen_arrays[i][j] += ( data[k][i] * eigen_genes[k][j] );
				    }
				}
			    norm[j] += Math.pow( eigen_arrays[i][j], 2.0 );
			}
		}

            for( int i = 0; i < norm.length; i++ )
	    {
		norm[i] = Math.sqrt( norm[i] );
	    }
            for( int i = 0; i < eigen_arrays.length; i++ )
	    {
		for( int j = 0; j < eigen_arrays[0].length; j++ )
		{
		    eigen_arrays[i][j] /= norm[j];
		}
	    }
	    
	    return true;
	}
	catch (IllegalArgumentException e) 
	{
	    mview.alertMessage(e.toString());
	    return false;
	} 
	catch (Exception e)
	{
	    System.out.println("doSVDusingSVDCMP()");
	    e.printStackTrace();
	    return false;
	}
	
    }



    private boolean doSVD()
    {
	
	int n_meas = 0;
	for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m))
	    {
		n_meas++;
	    }
	}

	if(n_meas < 2)
	{
	    mview.alertMessage("At least two Measurements must be selected");
	    return false;
	}

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	// build an array mapping data index to 'real' Measurement index
	//
	data_id_to_meas_id = new int[n_meas];
	n_meas = 0;
	for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m))
	    {
		data_id_to_meas_id[n_meas++] = edata.getMeasurementAtIndex(m);
	    }
	}

	data = new double[n_meas][];

	if(apply_filter_jchkb.isSelected())
	{
	    // filtered, contruct new double[]s containing just the unfiltered spots
	    //
	    final int real_n_spots = edata.getNumSpots();

	    int unfiltered = 0;
	    for(int s=0; s < real_n_spots; s++)
		if(!edata.filter(s))
		    unfiltered++;

	    data_id_to_spot_id = new int[unfiltered];

	    // build an array mapping filtered index to 'real' index
	    //
	   
	    int spot_n = 0;
	    for(int s=0; s < real_n_spots; s++)
		if(!edata.filter(s))
		{
		    data_id_to_spot_id[spot_n++] = s;
		}
	
	    // System.out.println(unfiltered + " unfiltered (from " + real_n_spots + ")");

	    for(int m=0; m < data_id_to_meas_id.length; m++)
	    {
		double[] raw_data = edata.getMeasurementData( data_id_to_meas_id[m] );
		double[] filtered_data = new double[ unfiltered ];
		
		for(int s=0; s < unfiltered; s++)
		    filtered_data[s] = raw_data[data_id_to_spot_id[s]];
		
		data[ m ] = filtered_data;
	    }
	}
	else
	{
	    // not filtered, just grab the actual double[] s
	    //
	    
	    data_id_to_spot_id = null;

	    for(int m=0; m < data_id_to_meas_id.length; m++)
	    {
		data[m] = edata.getMeasurementData( data_id_to_meas_id[m] );
	    }
	}


	if(containsNaNs(data))
	{
	    mview.alertMessage("The selected data contains NaN values which cannot be handled at this time.\n" +
			       "(You can use a Filter to remove these values)");
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    return false;
	}

	// ===============================================================
	//
	// construct a covariance matrix for measurements
	//

	center_data_matrix = do_pca_jrb.isSelected();

	double[][] cov = new double[n_meas][];
	
	int n = data.length;
	
	if(n == 0)
	{
	    mview.alertMessage("No Measurements have been selected");
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    return false;
	}
	int m = data[0].length;

	int i,j,k;

	
	means = null;
	if( center_data_matrix )
	{
	    means = new double[m];
	    double scaleFactor = 1.0 / ((double) n);
	    for( i = 0; i < m; i++ )
	    {     
		means[i] = 0.0;
		for( j = 0; j < n; j++ )
		{
		    means[i] += data[j][i];
		}
		means[i] *= scaleFactor;
	    }
	}
	
	cov = new double[n][n];
	for( i = 0; i < n; i++ )
	{
	    for( j = i; j < n; j++ )
	    {
		cov[i][j] = 0.0;
		
		if( center_data_matrix )
		{
		    for( k = 0; k < m; k++ )
		    {
			cov[i][j] += ((data[i][k] - means[k]) * (data[j][k] - means[k]));
		    }
		}
		else
		{
		    for( k = 0; k < m; k++ )
		    {
			cov[i][j] += data[i][k]  * data[j][k];
		    }
		}
		
		cov[j][i] = cov[i][j];
	    }
	}
	
	// ===============================================================
	//

	doSVDusingSVDCMP( data, cov );

	// ===============================================================
	//
	// finished....
	//

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	return true;
    }

    private boolean containsNaNs(double[][] data)
    {
	final int n = data.length;

	if(n == 0)
	    return false;

	final int m = data[0].length;
	
	for(int i=0; i < n; i++)
	    for(int j=0; j < m; j++)
		if(Double.isNaN( data[i][j] ))
		    return true;

	return false;
    }

    private boolean loadSVD()
    {
	return true;
    }

    public void projectOntoEigenArrays( )
    {
	int n_comps = new Integer(count_jtf.getText()).intValue();
	    
	double[][] pdata = new double[n_comps][data.length];
	
	double norm1 = .0;	
	double norm2 = .0;	
	

	for( int i = 0; i < data.length; i++ )
        {
	    for( int j = 0; j < n_comps; j++ )
	    {
		norm1 = .0;	
		norm2 = .0;	
		
		pdata[j][i] = .0;	
		
		
		for( int k = 0; k < eigen_arrays.length; k++ )
		{
		    if( center_data_matrix )
		    {
			pdata[j][i] += eigen_arrays[k][j] * ( data[i][k] - means[k] );
		    }
		    else
		    {
			pdata[j][i] += eigen_arrays[k][j] * data[i][k];
		    }
		}
	    }
	}
	
	

	String prefix_default = do_pca_jrb.isSelected() ? "PCA" : "SVD";
	
	String name_prefix = (name_jtf.getText().length() > 0) ? name_jtf.getText() : prefix_default;
	
	int[] new_spots = edata.addSpots( n_comps, name_prefix );
	
	for( int i = 0; i < data.length; i++ )
	{
	    ExprData.Measurement ms = edata.getMeasurement( data_id_to_meas_id[i] );

	    for( int j = 0; j < n_comps; j++ )
	    {
		ms.setEValue( new_spots[j], pdata[j][i] );
	    }
	}
	
	 edata.updateRanges();

	 edata.generateDataUpdate(ExprData.ValuesChanged);
    }

    public void projectOntoEigenGenes( )
    {
	try
	{
	    int n_comps = new Integer(count_jtf.getText()).intValue();
	    
	    double[][] pdata = new double[data[0].length][n_comps];
	    
	    for( int i = 0; i < data[0].length; i++ )
	    {
		for( int j = 0; j < n_comps; j++ )
		{
		    pdata[i][j] = .0;	
		    
		    for( int k = 0; k < eigen_genes[0].length; k++ )
			{
			    if( center_data_matrix )
			    {
				pdata[i][j] += eigen_genes[j][k] * ( data[k][i] - means[i] );
			    }
			    else
			    {    
				pdata[i][j] += eigen_genes[j][k] * data[k][i];
			    }
			}
		    
		    
		    
		    
		}
	    }
	    
	    // squirt the projected data back as Measurements

	    
	    final String name_prefix = name_jtf.getText();
	    
	    ExprData.Measurement new_m = null;

	    for( int j = 0; j < n_comps; j++ )
	    {
		if(data_id_to_spot_id == null)
		{
		    // not filtered, easy....
		    //
		    double[] new_m_data = new double[data[0].length];
		    for(int spot=0; spot < data[0].length; spot++)
			new_m_data[spot] = pdata[spot][j];
		    
		    
		    new_m = edata.new Measurement( name_prefix + (j+1),
						   ExprData.ExpressionAbsoluteDataType,
						   new_m_data );
		}
		else
		{
		    // filtered, construct a double[] with NaNs in the filtered spots
		    //
		    final int real_n_spots = edata.getNumSpots();
		    final int n_spots = data_id_to_spot_id.length;
		    
		    double[] unfiltered_data = new double[ real_n_spots ];
		    
		    for(int s=0; s < real_n_spots; s++)
			unfiltered_data[s] = Double.NaN;
		    for(int s=0; s < n_spots; s++)
			unfiltered_data[ data_id_to_spot_id[s] ] = pdata[s][j];

		    new_m = edata.new Measurement( name_prefix + (j+1),
						   ExprData.ExpressionAbsoluteDataType,
						   unfiltered_data );
		}
		
		new_m.addAttribute("Projection",    plugin_name, "Component " + (j+1));
		
		// and add the new Measurement
		//
		edata.addOrderedMeasurement(new_m);

		if(j == 0)
		    mview.getDataPlot().displayMeasurement( new_m );
	    }
	}
	
	catch(NumberFormatException nfe)
	{
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    GraphPanel eigenval_graph, wachter_graph;

    JTextField name_jtf;
    JTextField count_jtf;

    JCheckBox apply_filter_jchkb;
    JRadioButton do_svd_jrb, do_pca_jrb;

    double[]   eigen_values;
    double[][] eigen_genes;
    double[][] eigen_arrays;
    double[][] data;
	double[] means;
	boolean center_data_matrix;

    int[] data_id_to_spot_id;     // used when filtering is enabled
    int[] data_id_to_meas_id;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

    private JFrame frame = null;

    private DragAndDropList meas_list;
}
