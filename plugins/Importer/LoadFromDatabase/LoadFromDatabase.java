import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import javax.swing.event.*;
import java.util.Date;
import javax.swing.table.*;

//
// uses JDBC to retrieve data from a maxdSQL database
//

public class LoadFromDatabase implements Plugin
{
    public final static String plugin_name = "LoadFromDatabase";

    public LoadFromDatabase(maxdView mview_)
    {
	mview = mview_;
    }

    public void cleanUp()
    {
	if( cntrl.getConnection() != null )
	    cntrl.getConnection().disconnect();

	/*
	JPanel p = (JPanel) frame.getContentPane();

	mview.putIntProperty("LoadFromDatabase.width", p.getWidth());
	mview.putIntProperty("LoadFromDatabase.height", p.getHeight());

	if(h_split_pane != null)
	{
	    mview.putIntProperty("LoadFromDatabase.hsplit", h_split_pane.getDividerLocation());
	    mview.putIntProperty("LoadFromDatabase.vsplit", v_split_pane.getDividerLocation());
	}

	mview.putBooleanProperty("LoadFromDatabase.load_spot_details",  load_name_details[0]);
	mview.putBooleanProperty("LoadFromDatabase.load_probe_details", load_name_details[1]);
	mview.putBooleanProperty("LoadFromDatabase.load_gene_details",  load_name_details[2]);

	mview.putBooleanProperty("LoadFromDatabase.optimise_for_small_joins",  optimise_for_small_joins);
	*/

	frame.setVisible(false);

	// updateAndSaveDatabaseList();
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
	edata = mview.getExprData();

	System.out.println("LoadFromDatabase(): hello there!");

	cntrl = new Controller( mview, null, true );

	cntrl.setConnectionManager( new ConnectionManager_m2( cntrl, mview.getProperties() ) );

	frame = new JFrame("Load From maxdLoad2 Database");

	top = new JPanel();
	top.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	top.setPreferredSize( new Dimension( 700, 570 ) );
	frame.getContentPane().add( top );


	displayConnectPanel();

	mview.decorateFrame( frame );
	frame.pack();
	frame.setVisible( true );
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Load From Database", 
					 "importer", 
					 "Import data from a 'maxdLoad2' database", "", 
					 1, 0, 4);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	return null;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(done != null)
	    done.signal();
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- the connect panel  -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 

    private void displayConnectPanel()
    {
	top.removeAll();

	GridBagLayout gridbag = new GridBagLayout();
	top.setLayout(gridbag);


	ConnectionManager_m2 cm2 = cntrl.getConnectionManager();
	
	System.out.println( cm2 );

	JPanel connect_panel = cntrl.getConnectionManager().getConnectPanel( frame );

	GridBagConstraints c = new GridBagConstraints();
	c.weightx = 10.0;
	c.weighty = 9.0;
	
	c.fill = GridBagConstraints.BOTH;
	c.anchor = GridBagConstraints.NORTH;
	gridbag.setConstraints( connect_panel, c );
	top.add( connect_panel );

	JPanel buttons_panel = new JPanel();

	final JButton connect_jb = new JButton( "Connect" );
	buttons_panel.add( connect_jb );
	
	connect_jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 attemptToConnect();
					     }
				     });

	final JButton help_jb = new JButton( "Help" );
	buttons_panel.add( help_jb );

	final JButton close_jb = new JButton( "Close" );
	buttons_panel.add( close_jb );
	
	close_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cleanUp();
		}
	    });

	c = new GridBagConstraints();
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.SOUTH;
	c.weightx = 10.0;
	gridbag.setConstraints( buttons_panel, c );
	top.add( buttons_panel );


	top.validate();
	frame.validate();
	top.repaint();
    }
    
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- the browse panel  -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    
    private JPanel browse_panel = null;

    private void displayBrowsePanel()
    {
	top.removeAll();

	GridBagLayout top_gridbag = new GridBagLayout();
	top.setLayout( top_gridbag );

	if( browse_panel == null )
	    createBrowsePanel();

	GridBagConstraints c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 10.0;
	top_gridbag.setConstraints( browse_panel, c );
	top.add( browse_panel );

	top.validate();
	frame.validate();
	top.repaint();
    }

    private void createBrowsePanel()
    {
	System.out.println( "displayBrowsePanel()" );

	browse_panel = new JPanel();
	GridBagLayout browse_gridbag = new GridBagLayout();
	browse_panel.setLayout( browse_gridbag );


	{
	    JPanel table_panel = new JPanel();
	    table_panel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 5 ) );
	    GridBagLayout table_gridbag = new GridBagLayout();
	    table_panel.setLayout(table_gridbag);

	    {
		JLabel label = new JLabel("Browse ");
		GridBagConstraints c = new GridBagConstraints();
		//c.anchor = GridBagConstraints.EAST;
		table_gridbag.setConstraints(label, c);
		table_panel.add(label);
	    }

	    {
		browse_table_jcb = new JComboBox( cntrl.getAllUserVisibleTables() );

		browse_table_jcb.setSelectedIndex(-1);

		//browse_table_jcb.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		//jcb.setSelectedIndex( edata.getSetDataType(s) );
		browse_table_jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    browseTableHasBeenSelected();
			}
		    });

		browse_table_jcb.setToolTipText( "Pick which database table to browse" );
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.weightx = 10.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		table_gridbag.setConstraints( browse_table_jcb, c );

		table_panel.add( browse_table_jcb );		
	    }

	    {
		browse_items_jl = new JList();
		browse_items_jl.setSelectedIndex(-1);
		browse_items_jl.setToolTipText( "Pick one or more instances to explore" );

		browse_items_jl.addListSelectionListener( new ListSelectionListener()
		    {
			public void valueChanged(ListSelectionEvent e) 
			{
			    if (e.getValueIsAdjusting())
				return;
			    
			    browseInstancesHaveBeenSelected();
			}
		    });
		    

		browse_items_jl.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 1;
		c.gridwidth = 2;
		c.weightx = c.weighty = 10.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;

		JScrollPane browse_items_jsp = new JScrollPane( browse_items_jl );

		table_gridbag.setConstraints( browse_items_jsp , c);
		table_panel.add( browse_items_jsp );
	    }


	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 5.0;
	    c.weighty = 10.0;
	    c.gridheight = 2;
	    //c.anchor = GridBagConstraints.SOUTH;
	    browse_gridbag.setConstraints( table_panel, c );
	    browse_panel.add( table_panel );
	}


	{
	    JPanel measurement_panel = new JPanel();
	    measurement_panel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 5 ) );
	    GridBagLayout measurement_gridbag = new GridBagLayout();
	    measurement_panel.setLayout(measurement_gridbag);
	    
	    {
		JLabel label = new JLabel( "Measurement(s)" );
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		//c.anchor = GridBagConstraints.EAST;
		measurement_gridbag.setConstraints( label, c );
		measurement_panel.add( label );
	    }


	    {
		browse_measurements_jl = new JList();
		browse_measurements_jl.setSelectedIndex(-1);
		browse_measurements_jl.setToolTipText( "Pick one or more Measurements to load" );

		browse_measurements_jl.addListSelectionListener( new ListSelectionListener()
		    {
			public void valueChanged(ListSelectionEvent e) 
			{
			    if (e.getValueIsAdjusting())
				return;
			    
			    measurementsHaveBeenSelected();
			}
		    });
		    

		browse_measurements_jl.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 1;
		c.weightx = c.weighty = 10.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;

		JScrollPane browse_measurements_jsp = new JScrollPane( browse_measurements_jl );

		measurement_gridbag.setConstraints( browse_measurements_jsp , c);
		measurement_panel.add( browse_measurements_jsp );
	    }

	    {
		JButton all_jb = new JButton( "All" );
		all_jb.setMargin( new Insets( 0,5,0,5 ) );
		all_jb.setFont( mview.getSmallFont() );
		all_jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 int total = browse_measurements_jl.getModel().getSize();
						 browse_measurements_jl.setSelectionInterval(0, total - 1 );
					     }
				     });

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		//c.fill = GridBagConstraints.BOTH;
		measurement_gridbag.setConstraints( all_jb, c);
		measurement_panel.add( all_jb );
	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.weightx = 5.0;
	    c.weighty = 7.5;
	    //c.anchor = GridBagConstraints.SOUTH;
	    browse_gridbag.setConstraints( measurement_panel, c );
	    browse_panel.add( measurement_panel );



	}


	{
	    JPanel property_panel = new JPanel();
	    property_panel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 5 ) );
	    GridBagLayout property_gridbag = new GridBagLayout();
	    property_panel.setLayout(property_gridbag);
	    
	    {
		JLabel label = new JLabel( "Property(s)" );
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		//c.anchor = GridBagConstraints.EAST;
		property_gridbag.setConstraints( label, c );
		property_panel.add( label );
	    }


	    {
		browse_properties_jl = new JList();
		browse_properties_jl.setSelectedIndex(-1);
		browse_properties_jl.setToolTipText( "Pick one or more Properties to load" );

		browse_properties_jl.addListSelectionListener( new ListSelectionListener()
		    {
			public void valueChanged(ListSelectionEvent e) 
			{
			    if (e.getValueIsAdjusting())
				return;
			    
			    propertiesHaveBeenSelected();
			}
		    });
		    

		browse_properties_jl.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 1;
		c.weightx = c.weighty = 10.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;

		JScrollPane browse_properties_jsp = new JScrollPane( browse_properties_jl );

		property_gridbag.setConstraints( browse_properties_jsp , c);
		property_panel.add( browse_properties_jsp );
	    }

	    {
		JButton all_jb = new JButton( "All" );
		all_jb.setFont( mview.getSmallFont() );
		all_jb.setMargin( new Insets( 0,5,0,5 ) );
		all_jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 int total = browse_properties_jl.getModel().getSize();
						 browse_properties_jl.setSelectionInterval(0, total - 1 );
					     }
				     });

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		//c.fill = GridBagConstraints.BOTH;
		property_gridbag.setConstraints( all_jb , c);
		property_panel.add( all_jb );
	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weightx = 5.0;
	    c.weighty = 2.5;
	    //c.anchor = GridBagConstraints.SOUTH;
	    browse_gridbag.setConstraints( property_panel, c );
	    browse_panel.add( property_panel );



	}

	
	{
	    JPanel buttons_panel = new JPanel();
	    GridBagLayout buttons_gridbag = new GridBagLayout();
	    buttons_panel.setLayout( buttons_gridbag );
	    buttons_panel.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );

	    {
		final JButton jb = new JButton("Back");
		
		jb.setToolTipText("Return to the Connection Manager");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 cntrl.disconnect();

						 displayConnectPanel();
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	   
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 1 );

	    {
		
		final JButton jb = new JButton("Next");

		jb.setToolTipText("Choose data loading options");


		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    // make sure at least one property has been selected

			    int[] sels = browse_properties_jl.getSelectedIndices();

			    if( ( sels == null ) || ( sels.length == 0 ) )
			    {
				mview.alertMessage( "At least one Property must be selected for loading" );
			    }
			    else
			    {
				displayLoadMethodPanel();
			    }
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }

	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 3 );

	    {
		
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("DatabaseLoader", "DatabaseLoader");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	    
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 5 );

	    {   
		final JButton jb = new JButton("Close");
		
		jb.setToolTipText("Close this window");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 cleanUp();
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 6;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }


	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.gridwidth = 2;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;

	    browse_gridbag.setConstraints( buttons_panel, c );
	    browse_panel.add( buttons_panel );
	    
	}
	
	browse_table_jcb.setSelectedItem( "Experiment" );

    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- the loading methods panel  -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 

    private void displayLoadMethodPanel()
    {
	//System.out.println( "displayLoadingPanel()" );

	top.removeAll();

	GridBagLayout top_gridbag = new GridBagLayout();
	top.setLayout( top_gridbag );

	GridBagConstraints c;

	{
	    JPanel properties_panel = new JPanel();
	    properties_panel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 5 ) );
	    GridBagLayout properties_gridbag = new GridBagLayout();
	    properties_panel.setLayout( properties_gridbag );

	    int row = 0;


	    JRadioButton all_as_meas_jrb = new JRadioButton( "Load each Property as a Measurement" );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( all_as_meas_jrb , c );
	    properties_panel.add( all_as_meas_jrb );	

	    JLabel label = new JLabel( "Create one Measurement for each Property." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( label , c );
	    properties_panel.add( label  );	

	    label = new JLabel( "Properties with a Data Type of STRING or CHARACTER cannot be loaded using this method." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( label , c );
	    properties_panel.add( label  );


	    JRadioButton use_spot_attrs_jrb = new JRadioButton( "Load Properties as a Spot Attributes" );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( use_spot_attrs_jrb , c );
	    properties_panel.add( use_spot_attrs_jrb );	

	    label = new JLabel( "Pick one Property to use for the Measurement values, all other Properties will be loaded as Spot Attributes of that Measurement." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( label , c );
	    properties_panel.add( label  );
	    
	    label = new JLabel( "Only Properties with a Data Type of INTEGER or DOUBLE can be used for Measurement values." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( label , c );
	    properties_panel.add( label  );


	    ButtonGroup bg = new ButtonGroup();
	    bg.add( all_as_meas_jrb );
	    bg.add( use_spot_attrs_jrb );
	    all_as_meas_jrb.setSelected( true );


	    cntrl.addFiller( 24, properties_panel, properties_gridbag, row++, 0 );

	    
	    label = new JLabel( "Select the Property to load as the Measurement values" );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    properties_gridbag.setConstraints( label , c );
	    properties_panel.add( label  );
	    

	    java.util.Vector data_vv = new java.util.Vector();

	    if( browsing_properties != null )
	    {
		int[] sels = browse_properties_jl.getSelectedIndices();
		
		for( int s = 0 ; s < sels.length ; s++ )
		{
		    MeasDataProp mdp = browsing_properties[ sels[ s ] ];

		    java.util.Vector data_v = new java.util.Vector();
		    
		    if( ( mdp.data_type_code == MeasDataProp.IntegerDataType ) || 
			( mdp.data_type_code == MeasDataProp.DoubleDataType ) )
		    {
			data_v.add( mdp.name );
			data_v.add( mdp.quant_type );
			data_v.add( mdp.scale );
			data_v.add( mdp.unit );
			data_v.add( MeasDataProp.getDataTypeName( mdp.data_type_code ) );
			
			data_vv.add( data_v );
		    }
		}
	    }

	    java.util.Vector col_names_v = new java.util.Vector();
	    col_names_v.add( "Name" );
	    col_names_v.add( "QuantitationType" );
	    col_names_v.add( "Scale" );
	    col_names_v.add( "Unit" );
	    col_names_v.add( "DataType" );

	    DefaultTableModel dtm = new DefaultTableModel( data_vv, col_names_v );
	    
	    JTable table = new JTable( dtm );

	    table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	    table.setRowSelectionAllowed( true );
	    table.setColumnSelectionAllowed( false );

	    JScrollPane jsp = new JScrollPane( table );

	    c = new GridBagConstraints();
	    c.gridy = row++;
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 8.0;
	    c.weightx = 10.0;
	    properties_gridbag.setConstraints( jsp, c );
	    properties_panel.add( jsp );	

	    
	    

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.weighty = 8.0;
	    c.weightx = 5.0;
	    c.fill = GridBagConstraints.BOTH;
	    top_gridbag.setConstraints( properties_panel, c );
	    top.add( properties_panel );


	    all_as_meas_jrb.setEnabled( false );
	    use_spot_attrs_jrb.setEnabled( false );
	    table.setEnabled( false );
	}



	{
	    JPanel buttons_panel = new JPanel();
	    GridBagLayout buttons_gridbag = new GridBagLayout();
	    buttons_panel.setLayout( buttons_gridbag );
	    buttons_panel.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );

	    {
		final JButton jb = new JButton("Back");
		
		jb.setToolTipText("Return to the Measurement Browser");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 displayBrowsePanel();
					     }
				     });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	   
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 1 );

	    {
		final JButton jb = new JButton("Next");
		
		jb.setToolTipText("Choose the loading options");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 displayLoadOptionsPanel();
					     }
				     });
		
		c = new GridBagConstraints();
		c.gridx = 2;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	   
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 3 );

	    {
		
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("DatabaseLoader", "DatabaseLoader");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 4;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	    
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 5 );

	    {   
		final JButton jb = new JButton("Close");
		
		jb.setToolTipText("Close this window");
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cleanUp();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 6;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }



	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weighty = 1.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.SOUTH;
	    top_gridbag.setConstraints( buttons_panel, c );
	    top.add( buttons_panel );
	    
	}



	top.validate();
	top.repaint();
	frame.validate();

    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- the loading options panel  -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 
    JCheckBox cluster_props_jcb;
    JCheckBox cluster_meas_jcb;
    JCheckBox expt_attrs_jrb;
    JCheckBox array_attrs_jrb;
    JRadioButton replace_jrb;
    JRadioButton merge_jrb;

    private void displayLoadOptionsPanel()
    {

	top.removeAll();

	GridBagLayout top_gridbag = new GridBagLayout();
	top.setLayout( top_gridbag );

	GridBagConstraints c;
	JLabel label;

	{
	    JPanel options_panel = new JPanel();
	    options_panel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ) );
	    GridBagLayout options_gridbag = new GridBagLayout();
	    options_panel.setLayout( options_gridbag );

	    int row = 0;

	    if( replace_jrb == null )
	    {
		replace_jrb = new JRadioButton( "Replace the current data" );
		replace_jrb.setSelected( true );
	    }

	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( replace_jrb , c );
	    options_panel.add( replace_jrb );	

	    if( merge_jrb == null )
	    {
		merge_jrb = new JRadioButton( "Merge with the current data" );
		merge_jrb.setSelected( false );
	    }

	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( merge_jrb , c );
	    options_panel.add( merge_jrb );	

	    ButtonGroup bg = new ButtonGroup();
	    bg.add( replace_jrb );
	    bg.add( merge_jrb );



	    cntrl.addFiller( 24, options_panel, options_gridbag, row++, 0 );

	    if( array_attrs_jrb == null )
	    {
		array_attrs_jrb = new JCheckBox( "Retrieve all array meta-data" );
		array_attrs_jrb.setSelected( true );
	    }
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( array_attrs_jrb , c );
	    options_panel.add( array_attrs_jrb );	

	    label = new JLabel( "Load all of the attribute values for the Spots, Probes and Genes." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( label , c );
	    options_panel.add( label  );
	    label = new JLabel( "(This may be quite time consuming for arrays with many Spots)" );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( label , c );
	    options_panel.add( label  );
	    
	    cntrl.addFiller( 12, options_panel, options_gridbag, row++, 0 );

	    if( expt_attrs_jrb == null )
	    {
		expt_attrs_jrb = new JCheckBox( "Retrieve all experiment meta-data" );
		expt_attrs_jrb.setSelected( true );
	    }

	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( expt_attrs_jrb , c );
	    options_panel.add( expt_attrs_jrb );	

	    label = new JLabel( "Load all available attribute values for things linked to each Measurement." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( label , c );
	    options_panel.add( label  );



	    cntrl.addFiller( 24, options_panel, options_gridbag, row++, 0 );

	    if( cluster_props_jcb == null )
	    {
		cluster_props_jcb = new JCheckBox( "Create clusters of related Properties" );
		cluster_props_jcb.setSelected( true );
	    }
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( cluster_props_jcb , c );
	    options_panel.add( cluster_props_jcb );	


	    int[] p_sels = browse_properties_jl.getSelectedIndices();
	    cluster_props_jcb.setEnabled( p_sels.length > 1 );


	    label = new JLabel( "Create a cluster for each of the Properties; it will contain the Measurements which represent that Property" );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( label , c );
	    options_panel.add( label  );


	    cntrl.addFiller( 12, options_panel, options_gridbag, row++, 0 );

	    if( cluster_meas_jcb == null )
	    {
		cluster_meas_jcb = new JCheckBox( "Create clusters of related Measurements" );
		cluster_meas_jcb.setSelected( true );
	    }
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( cluster_meas_jcb , c );
	    options_panel.add( cluster_meas_jcb );	


	    int[] m_sels = browse_measurements_jl.getSelectedIndices();
	    cluster_meas_jcb.setEnabled( m_sels.length > 1 );


	    label = new JLabel( "Create a cluster for each of the Measurements; it will contain all of the Properties of that Measurement" );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = row++;
	    c.anchor = GridBagConstraints.WEST;
	    options_gridbag.setConstraints( label , c );
	    options_panel.add( label  );


	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.weighty = 8.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    top_gridbag.setConstraints( options_panel, c );
	    top.add( options_panel );

	}

	{
	    JPanel buttons_panel = new JPanel();
	    GridBagLayout buttons_gridbag = new GridBagLayout();
	    buttons_panel.setLayout( buttons_gridbag );
	    buttons_panel.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );

	    {
		final JButton jb = new JButton("Back");
		
		jb.setToolTipText("Return to the Loading Method options");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 displayLoadMethodPanel();
					     }
				     });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	   
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 1 );

	    {
		final JButton jb = new JButton("Load");
		
		jb.setToolTipText("Start importing the data");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 //startLoadingInForeground();
						 startLoadingInBackground();
					     }
				     });
		
		c = new GridBagConstraints();
		c.gridx = 2;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	   
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 3 );

	    {
		
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("DatabaseLoader", "DatabaseLoader");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 4;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	    
	    cntrl.addFiller( 24, buttons_panel, buttons_gridbag, 0, 5 );

	    {   
		final JButton jb = new JButton("Close");
		
		jb.setToolTipText("Close this window");
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cleanUp();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 6;
		buttons_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }



	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weighty = 1.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.SOUTH;
	    top_gridbag.setConstraints( buttons_panel, c );
	    top.add( buttons_panel );
	    
	}

	top.validate();
	top.repaint();
	frame.validate();

    }


    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 


    private void attemptToConnect()
    {
	int result = cntrl.attemptConnect( );
	
	if(result > 0)
	{
	    // connection has been acheived...

	    displayBrowsePanel();
	}
	else
	{
	    // connection failed, do nothing....
	}
    
    }


    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
 

    public void browseTableHasBeenSelected()
    {
	int t = browse_table_jcb.getSelectedIndex();

	if(t < 0) 
	    return;
	
	final String browse_table = (String) browse_table_jcb.getSelectedItem();

	System.out.println("Browsing on " + browse_table );
	
	browsing_insts = cntrl.getConnection().getAllInstances( browse_table );

	if( ( browsing_insts == null ) || ( browsing_insts.length == 0 ) )
	{
	    // nothing available

	    browse_items_jl.setModel( new DefaultListModel() );

	    return;
	}

	final DefaultListModel dlm = new DefaultListModel();

	for( int i = 0; i < browsing_insts.length; i++ )
	{
	    dlm.addElement( browsing_insts[ i ].name );
	}

	browse_items_jl.setModel( dlm );


	/*
	browse_items_jcb.removeActionListener(browse_items_al);
	browse_items_jcb.removeAllItems();
	
	boolean first = true;
	*/

    }


    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
 

    public void browseInstancesHaveBeenSelected()
    {
	// find the Measurement(s) linked to the selected Instance(s)

	int[] sels = browse_items_jl.getSelectedIndices();

	if( ( sels == null ) || (  sels.length == 0 ) )
	{
	    browse_measurements_jl.setModel( new DefaultListModel() );

	    return;
	}
	

	Instance[] selected_insts = new Instance[ sels.length ];

	java.util.Vector combined_linked_measurements_v = new java.util.Vector();
	java.util.HashSet already_included_hs = new java.util.HashSet();

	final String browse_table = (String) browse_table_jcb.getSelectedItem();


	System.out.println( sels.length + " " + browse_table + " instance(s) selected...." );


	for( int s = 0 ; s < sels.length ; s++ )
	{
	    System.out.println( " checking " + browsing_insts[ sels[ s ] ].name + "..." );

	    Instance[] linked_measurements = cntrl.findLinkedInstances( browse_table, 
									"Measurement", 
									browsing_insts[ sels[ s ] ] );

	    
	    if( ( linked_measurements != null ) && ( linked_measurements.length > 0  ) )
	    {
		System.out.println( linked_measurements.length + " Measurements linked to #" + (s+1 ) );

		for( int l = 0; l < linked_measurements.length; l++ )
		{
		    if( already_included_hs.contains( linked_measurements[ l ].id ) == false )
		    {
			already_included_hs.add( linked_measurements[ l ].id );
			
			combined_linked_measurements_v.add( linked_measurements[ l ] );
		    }
		}
	    }
	}

	browsing_measurements = ( Instance[] ) combined_linked_measurements_v.toArray( new Instance[ combined_linked_measurements_v.size() ] );
	
	System.out.println( browsing_measurements.length + " Measurements found in total" );

	final DefaultListModel dlm = new DefaultListModel();

	for( int i = 0; i < browsing_measurements.length; i++ )
	{
	    dlm.addElement( browsing_measurements[ i ].name );
	}

	browse_measurements_jl.setModel( dlm );

    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public void measurementsHaveBeenSelected()
    {
	// figure out which Properties are available for the selected Measurements...

	int[] sels = browse_measurements_jl.getSelectedIndices();

	if( ( sels == null ) || (  sels.length == 0 ) )
	{
	    browse_properties_jl.setModel( new DefaultListModel() );

	    return;
	}
	

	Instance[] selected_insts = new Instance[ sels.length ];
	
	java.util.Vector combined_linked_properties_v = new java.util.Vector();
	java.util.HashSet already_included_hs = new java.util.HashSet();


	System.out.println( sels.length + " Measurement instance(s) selected...." );


	for( int s = 0 ; s < sels.length ; s++ )
	{
	    System.out.println( " checking " + browsing_measurements[ sels[ s ] ].name + "..." );
	    
	    MeasDataProp[] properties = cntrl.findPropertiesForMeasurement( browsing_measurements[ sels[ s ] ] );
	    
	    if( properties != null )
	    {
		System.out.println( properties.length + " Properties found..." );
	   
		for( int p = 0; p < properties.length; p++ )
		{
		    if( already_included_hs.contains( properties[ p ].type_id ) == false )
		    {
			already_included_hs.add( properties[ p ].type_id );

			combined_linked_properties_v.add( properties[ p ] );
		    }
		}
	    }
	    else
	    {
		System.out.println( "No Properties found..." );
	    }
	}
   
	browsing_properties = ( MeasDataProp[] ) combined_linked_properties_v.toArray( new MeasDataProp[ combined_linked_properties_v.size() ] );
	
	System.out.println( browsing_properties.length + " Properties found in total" );

	final DefaultListModel dlm = new DefaultListModel();

	for( int i = 0; i < browsing_properties.length; i++ )
	{
	    StringBuffer info = new StringBuffer();
	    
	    info.append( browsing_properties[ i ].quant_type );
	    
	    if( ( browsing_properties[ i ].scale != null )&& ( browsing_properties[ i ].scale.length() > 0 ) )
	    {
		if( info.length() > 0 )
		    info.append( " / " );
		info.append( browsing_properties[ i ].scale );
	    }

	    if(( browsing_properties[ i ].unit != null ) && ( browsing_properties[ i ].unit.length() > 0 ) )
	    {
		if( info.length() > 0 )
		    info.append( " / " );
		info.append( browsing_properties[ i ].unit );
	    }

	    if( info.length() > 0 )
		info.append( " / " );
	    info.append( browsing_properties[ i ].getDataTypeName( browsing_properties[ i ].data_type_code ) );

	    dlm.addElement( browsing_properties[ i ].name + "   (" + info + ")" );
	}

	browse_properties_jl.setModel( dlm );

    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public void propertiesHaveBeenSelected()
    {

    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public class AbortLoadingException extends Exception
    {
	public AbortLoadingException( String s ) { super( s ); }
    }


    public void startLoadingInForeground()
    {
	System.out.println( "Loading data in FOREGROUND for debugging" );

	final ProgressOMeter pm = new ProgressOMeter( "Retrieving data...", 2);

	try
	{
	    actuallyLoadData( pm );
	    if( pm != null )
		pm.stopIt();
	}
	catch( AbortLoadingException ale )
	{
	    ale.printStackTrace();

	    if( pm != null )
		pm.stopIt();

	    mview.alertMessage( ale.getMessage() );

	    return;
	}
	catch( java.lang.OutOfMemoryError oome )
	{
	    oome.printStackTrace();

	    System.out.println( oome.getMessage() );
	    
	    if( pm != null )
		pm.stopIt();

	    mview.alertMessage("Insufficient free memory to retrieve the selected data.\n" +
			       "\n" +
			       "Please see the help documentation for information on how\n" + 
			       "to allocate more memory to the application." );
	    return;
	}
	catch( Exception ex )
	{
	    ex.printStackTrace();

	    if( pm != null )
		pm.stopIt();

	    mview.alertMessage( "Unexpected exception. Loading aborted.\n\n" + ex.getMessage() );

	    return;
	}

	displayBrowsePanel();
    }

    public void startLoadingInBackground()
    {
	System.out.println( "Loading data in BACKGROUND thread" );

	final SwingWorker worker = new SwingWorker() 
	    {
		Instance result = null;
		
		public Object construct() 
		{
		    ProgressOMeter pm = new ProgressOMeter( "Retrieving data...", 2);
		    try
		    {
			actuallyLoadData( pm );
			if( pm != null )
			    pm.stopIt();
		    }
		    catch( AbortLoadingException ale )
		    {
			if( pm != null )
			    pm.stopIt();
			mview.alertMessage( ale.getMessage() );
			return null;
		    }
		    catch( java.lang.OutOfMemoryError oome )
		    {
			System.out.println( oome.getMessage() );

			if( pm != null )
			    pm.stopIt();
			mview.alertMessage("Insufficient free memory to retrieve the selected data.\n" +
					   "\n" +
					   "Please see the help documentation for information on how\n" + 
					   "to allocate more memory to the application." );
		    }
		    catch( Exception ex )
		    {
			if( pm != null )
			    pm.stopIt();
			ex.printStackTrace();
			mview.alertMessage( "Unexpected exception. Loading aborted.\n\n" + ex.getMessage() );
			return null;
		    }

		    return "ok";
		}

		public void finished() 
		{
		    String result = (String) getValue();

		    if( result != null  )
		    {
			// the load was a success!
		    }
		    else
		    {
			// the load failed!
		    }

		    // return to the browse panel so the user can load more from this database
		    
		    displayBrowsePanel();

		}
	    };

	try
	{
	    worker.start();  //required for SwingWorker 3
	}
	catch( java.lang.OutOfMemoryError oome )
	{
	    mview.alertMessage( "out of memory" );
	}

    }
    


    private void actuallyLoadData( ProgressOMeter pm ) throws AbortLoadingException
    {
	System.out.println( "actuallyLoadData() begins..." );


	final Instance array_type_instance = findCommonArrayTypeForMeasurements( );
	
	if( array_type_instance == null )
	{
	    throw new AbortLoadingException("The selected set of Measurements utilise more than one ArrayType\n" +
					    "Regretably, this situation can't be handled at the moment\n" +
					    "\n" + 
					    "(you can use the 'Merge' loading mode to combine different ArrayTypes)" );
	}
	else
	{
	    if( replace_jrb.isSelected() )
	    {
		if( mview.getExprData().getNumMeasurements() > 0 )
		{
		    if(mview.infoQuestion("Really replace existing data?", "Yes", "No") == 1)
		    {
			return;
		    }
		    mview.getDataPlot().removeAllNameCols();
		    mview.getExprData().removeAllMeasurements();
		}
		if( edata.getNumClusters() > 1 ) // there is always one 'root' cluster
		{
		    if(mview.infoQuestion("Remove existing clusters?", "Yes", "No") == 0)
		    {
			edata.removeAllClusters();
		    }
		}
	    }


	    pm.startIt();

	    pm.setMessage( 1, "Getting a minimal array description...");
			
	    try
	    {
		System.out.println( "loading array description..." );

		ArrayDescription array_description = cntrl.getArrayDescription( array_type_instance, array_attrs_jrb.isSelected() );
		
		
		System.out.println( "array description loaded..." );

		ExprData.DataTags dtags = edata.new DataTags( extractNamesFromInstances( array_description.features ), 
							      extractNamesFromInstances( array_description.reporters) , 
							      extractNamesFromInstances( array_description.genes ) ); 
		
		System.out.println( dtags.spot_name.length + " spots detected" );
		
		// for each of the selection measurements ....
		
		int[] sels = browse_measurements_jl.getSelectedIndices();
		
		
		// if we are merging, then we collect the Measurements in a vector until we have them all
		//
		java.util.Vector measurement_v = new java.util.Vector();
		
		
		
		// if we are creating a 'related properties' cluster, then we need a root node
		ExprData.Cluster props_cluster_root = edata.new Cluster( "Related Properties", ExprData.MeasurementName );
		
		// and a bunch of vectors into which to store the names of the measurements that correspond
		// to each of the possible properties that can be loaded
		java.util.Hashtable property_cluster_contents = new java.util.Hashtable();
		MeasDataProp[] requested_properties =  findRequestedProperties( );
		for(int p = 0 ; p < requested_properties.length; p++ )
		{
		    property_cluster_contents.put( requested_properties[ p ].name,  new java.util.Vector() );
		}
		
		
		
		// if we are creating a 'related measurements' cluster, then we need a root node
		ExprData.Cluster meas_cluster_root = edata.new Cluster( "Related Measurements", ExprData.MeasurementName );
		
		LinkedInfo[][] expt_meta_data_for_measurement = new LinkedInfo[ sels.length ][];
		
		for( int s = 0 ; s < sels.length ; s++ )
		{
		    pm.setMessage( 1, "Getting Measurement " + ( (s+1) + " of " + sels.length ) + "...");
		    
		    final Instance measurement = browsing_measurements[ sels[ s ] ];
		    
		    
		    if( expt_attrs_jrb.isSelected() )
		    {
			expt_meta_data_for_measurement[ s ] = getExperimentalMetaDataForMeasurement( measurement );
		    }
		    
		    
		    
		    MeasDataProp[] available_properties = cntrl.findPropertiesForMeasurement( measurement );
		    
		    // work out which of the selected set of properties are available for this measurement
		    
		    MeasDataProp[] required_properties = findRequiredProperties( available_properties );
		    
		    
		    MeasDataProp[] permitted_properties = findPermittedProperties( required_properties );
		    
		    String[][] data = cntrl.getMeasurementPropertyValues( array_description, 
									  browsing_measurements[ sels[ s ] ], 
									  permitted_properties );

		    /*
		    // this illustrates how the data is laid out
		    for( int p = 0 ; p < properties.length; p++ )
		    {
		    System.out.print( properties[ p ].name );
		    System.out.print( "," );
		    }
		    System.out.println();
		    
		    for( int f = 0; f < array_description.features.length; f++ )
		    {
		    System.out.print( array_description.features[ f ].name );
		    System.out.print( "," );
		    
		    for( int p = 0 ; p < properties.length; p++ )
		    {
		    System.out.print( data[ p ][ f ] );
		    System.out.print( "," );
		    }
		    System.out.println();
		    }
		    */
		    
		    
		    // create a 'maxdView.Measurement' object for each of the Properties
		    
		    java.util.Vector props_cluster_elements_v = new java.util.Vector();
		    
		    
		    for( int p = 0 ; p < required_properties.length; p++ )
		    {
			ExprData.Measurement m = mview.getExprData().new Measurement();
			
			final String full_name = measurement.name + ":" + required_properties[ p ].name;
			
			props_cluster_elements_v.add( full_name );
			
			m.setName( full_name );
			
			m.addAttribute("Database name", plugin_name, cntrl.getConnection().getProperties().database_name );
			m.addAttribute("Database version", plugin_name, cntrl.getConnection().getProperties().database_version );
			m.addAttribute("Database location", plugin_name, cntrl.getConnection().getProperties().database_location );
			
			m.addAttribute("Property.QuantitationType", plugin_name, permitted_properties[ p ].quant_type );
			m.addAttribute("Property.Scale", plugin_name, permitted_properties[ p ].scale );
			m.addAttribute("Property.Origin", plugin_name, permitted_properties[ p ].origin );
			m.addAttribute("Property.Unit", plugin_name, permitted_properties[ p ].unit );
			m.addAttribute("Property.Name", plugin_name, permitted_properties[ p ].name );
			
			
			// add MeasurmentAttributes for each of the experimental meta data items
			if( expt_meta_data_for_measurement[ s ] != null )
			{
			    for( int emd = 0 ; emd <  expt_meta_data_for_measurement[ s ].length; emd++ )
			    {
				m.addAttribute( expt_meta_data_for_measurement[ s ][ emd ].table, 
						plugin_name,
						expt_meta_data_for_measurement[ s ][ emd ].instance.name );
			    }
			}
			
			
			m.setShow( true );
			
			// this should really be set based on the Property.QuantitiationType
			m.setDataType( ExprData.ExpressionAbsoluteDataType );
			
			// this only works for Double or Integer data types...
			m.setData( convertStringToDouble( data[ p ] ) );
			
			m.setDataTags( dtags );
			
			// and install the data
			if( replace_jrb.isSelected() )
			{
			    if( p == 0 )
			    {
				if( mview.getExprData().addMeasurement( m ) == false )
				{
				    mview.alertMessage( "Loading has been aborted." );
				    displayBrowsePanel();
				    return;
				}
			    }
			    else
			    {
				if( mview.getExprData().addOrderedMeasurement( m ) == false )
				{
				    mview.alertMessage( "Loading has been aborted." );
				    displayBrowsePanel();
				    return;
				}
			    }
			}
			else
			{
			    measurement_v.add( m );
			}
			
			
			if( cluster_props_jcb.isSelected() )
			{
			    java.util.Vector name_list_for_this_property = ( java.util.Vector ) property_cluster_contents.get( required_properties[ p ].name );
			    System.out.println( "adding " + full_name + " to prop-cluster " +  required_properties[ p ].name );
			    name_list_for_this_property.add( full_name );
			}
		    }
		    
		    
		    if( cluster_meas_jcb.isSelected() )
		    {
			ExprData.Cluster meas_cluster = edata.new Cluster( "Properties for " + measurement.name,
									   ExprData.MeasurementName,
									   props_cluster_elements_v );
			meas_cluster_root.addCluster( meas_cluster );
		    }
		}
		
		// if we were merging, do the merge now
		
		if( merge_jrb.isSelected() )
		{
		    edata.mergeMeasurements( (ExprData.Measurement[]) ( measurement_v.toArray( new ExprData.Measurement[0] ) ) );
		}
		
		
		// and optionally add the related Measurements cluster
		
		if( cluster_meas_jcb.isSelected() )
		{
		    edata.addCluster( meas_cluster_root );
		}
		
		
		// and optionally add the related Properties cluster
		
		if( cluster_meas_jcb.isSelected() )
		{
		    for(int p = 0 ; p < requested_properties.length; p++ )
		    {
			java.util.Vector name_list_for_this_property = ( java.util.Vector ) property_cluster_contents.get( requested_properties[ p ].name );
			
			if( name_list_for_this_property.size() > 0 )
			    props_cluster_root.addCluster( edata.new Cluster( "Measurements for " + requested_properties[p].name,
									      ExprData.MeasurementName,
									      name_list_for_this_property ) );
		    }
		    
		    edata.addCluster( props_cluster_root );
		}
		
		
		
		if( array_attrs_jrb.isSelected() )
		{
		    pm.setMessage( 1, "Getting detailed array description ... (Features)");
		    
		    //System.out.println( "loading Array Description meta-data....Features" );
		    
		    loadTagAttrs( edata.getSpotTagAttrs(), array_description.feature_to_attribute_id );
		    
		    pm.setMessage( 1, "Getting detailed array description ... (Probes)");
		    
		    //System.out.println( "loading Array Description meta-data....Probes" );
		    
		    loadTagAttrs( edata.getProbeTagAttrs(), array_description.reporter_to_attribute_id );
		    
		    pm.setMessage( 1, "Getting detailed array description ... (Genes)");
		    
		    //System.out.println( "loading Array Descrption meta-data....Genes" );
		    
		    loadTagAttrs( edata.getGeneTagAttrs(), array_description.gene_to_attribute_id );
		}
		
	    }
	    catch( java.lang.OutOfMemoryError oome )
	    {
		throw new AbortLoadingException("Insufficient free memory to retrieve the selected data.\n" +
						"\n" +
						"Please see the help documentation for information on how\n" + 
						"to allocate more memory to the application." );

	    }
	    catch( Exception ex )
	    {
		ex.printStackTrace();

		throw new AbortLoadingException("Unexpected exception whilst retrieving the selected data.\n" +
						"\n" +
						ex.getMessage() );
		
	    }
	}

    }
    
    
    private String[] extractNamesFromInstances( Instance[] insts )
    {
	if( ( insts == null ) || ( insts.length == 0 ) )
	    return null;

	String[] result = new String[ insts.length ];

	for( int i = 0; i < insts.length; i++ )
	    result[ i ] = ( insts[ i ] == null ) ? "" : insts[ i ].name;

	return result;
    }
    private String[][] extractNamesFromInstances( Instance[][] insts )
    {
	String[][] result = new String[ insts.length ][];

	for( int i = 0; i < insts.length; i++ )
	{
	    if( insts[ i ] != null )
	    {
		result[ i ] = new String[ insts[ i ].length ];
		
		for( int i2 = 0; i2 < insts[ i ].length; i2++ )
		{
		    result[ i ][ i2 ] = ( insts[ i ][ i2 ] == null ) ? "" : insts[ i ][ i2 ].name;
		}
	    }
	}

	return result;
    }

    private double[] convertStringToDouble( String[] vals )
    {
	double[] vals_d = new double[ vals.length ];

	for( int v = 0; v < vals.length; v++ )
	{
	    if( vals [ v ] != null )
	    {
		try
		{
		    vals_d[ v ] = NumberParser.tokenToDouble( vals [ v ] );
		}
		catch( TokenIsNotNumber tinn )
		{
		    vals_d[ v ] = Double.NaN;
		}
	    }
	    else
	    {
		vals_d[ v ] = Double.NaN;
	    }
	}

	return vals_d;

    }

    //
    // work out which of the properties have been selected by the user
    //
    private MeasDataProp[] findRequestedProperties( )
    {
	int[] sels = browse_properties_jl.getSelectedIndices();
	java.util.Vector mdp_v = new java.util.Vector();
	for( int s = 0 ; s < sels.length ; s++ )
	{
	    mdp_v.add( browsing_properties[ sels[ s ] ] );
	}
	return ( MeasDataProp[] ) mdp_v.toArray( new MeasDataProp[ mdp_v.size() ] );
    }

    //
    // work out which of the available properties occur in the set of 
    // properties that have been selected for loading
    //
    private MeasDataProp[] findRequiredProperties( MeasDataProp[] available_mdp )
    {
	java.util.HashSet selected_mdp_names = new java.util.HashSet();

	int[] sels = browse_properties_jl.getSelectedIndices();

	for( int s = 0 ; s < sels.length ; s++ )
	{
	    selected_mdp_names.add( browsing_properties[ sels[ s ] ].name );
	}

	java.util.Vector required_mdp_v = new java.util.Vector();

	for( int a=0; a < available_mdp.length; a++ )
	{
	    if( selected_mdp_names.contains( available_mdp[ a ].name ) )
		required_mdp_v.add( available_mdp[ a ] );
	}
	
	return ( MeasDataProp[] ) required_mdp_v.toArray( new MeasDataProp[ required_mdp_v.size() ] );
    }


    //
    // work out which of the possible properties are permissable for use as Measurement values 
    // ( i.e. those which are INTEGER or DOUBLE )
    //
    private MeasDataProp[] findPermittedProperties( MeasDataProp[] candidate_mdp )
    {
	java.util.Vector mdp_v = new java.util.Vector();

	for( int m = 0 ; m < candidate_mdp.length ; m++ )
	{
	    if( ( candidate_mdp[ m ].data_type_code == MeasDataProp.IntegerDataType ) || 
		( candidate_mdp[ m ].data_type_code == MeasDataProp.DoubleDataType ) )
	    {
		mdp_v.add( candidate_mdp[ m ] );
	    }
	}
	
	if( mdp_v.size() == 0 )
	    return null;
	else
	    return ( MeasDataProp[] ) mdp_v.toArray( new MeasDataProp[ mdp_v.size() ] );
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    /*
        find the names of all possible linked instances....

    */
    class LinkedInfo
    {
	public String table;
	public Instance instance;
	public LinkedInfo( String t, Instance i )
	{
	    table = t;
	    instance = i;
	}
    }

    final boolean debug_expt_meta_data_search = false;

    private LinkedInfo[] getExperimentalMetaDataForMeasurement( Instance meas_inst )
    {
	java.util.Vector result = new java.util.Vector();

	if( debug_expt_meta_data_search )
	    System.out.println("getExperimentalMetaDataForMeasurement(): measurement = " + meas_inst.name  );

	Instance[] meas_insts = new Instance[ 1 ];
	meas_insts[ 0 ] = meas_inst;

	getExperimentalMetaDataForTable( result, meas_insts, "Measurement" );

	for( int r=0 ; r < result.size(); r++ )
	{
	    LinkedInfo li = (LinkedInfo) result.elementAt( r );
	    System.out.println( li.table + " : " + li.instance.name );
	}
	
	return (LinkedInfo[]) result.toArray( new LinkedInfo[ result.size() ] );
    }

    
    private void getExperimentalMetaDataForTable( java.util.Vector accumulator,
						  Instance[] start_insts, 
						  String start_table )
    {
	if( debug_expt_meta_data_search )
	    System.out.println("getExperimentalMetaDataForTable(): starting at " + start_table + "...");

	String[] linked_tables = cntrl.getForeignKeys( start_table );

	if( linked_tables == null )
	    return;

	for( int t=0; t < linked_tables.length; t++ )
	{
	    if( debug_expt_meta_data_search )
		System.out.println("getExperimentalMetaDataForTable(): checking  " + linked_tables[ t ] + "...");

	    getExperimentalMetaDataForLinkedTable( accumulator,
						   start_insts, 
						   start_table, 
						   linked_tables[ t ] );
	}

    }

	
    
    private void getExperimentalMetaDataForLinkedTable( java.util.Vector accumulator,
							Instance[] start_insts, 
							String start_table, 
							String target_table )
    {

	if( debug_expt_meta_data_search )
	    System.out.println("getExperimentalMetaDataForLinkedTable(): examining  " +
			       start_insts.length + " x " + start_table + "...");

	Instance[] linked = cntrl.retrieveLinkedInstances( start_table, start_insts, target_table );

	if (linked == null )
	    return;
	
	if( debug_expt_meta_data_search )
	    System.out.println("getExperimentalMetaDataForLinkedTable(): found " + linked.length + " linked " + target_table );
	
	for(int l=0; l < linked.length; l++ )
	    accumulator.add( new LinkedInfo( target_table, linked[ l ] ) );
	
	getExperimentalMetaDataForTable( accumulator, linked, target_table );

    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --



    public Instance findCommonArrayTypeForMeasurements()
    {
	int[] sels = browse_measurements_jl.getSelectedIndices();

	Instance the_common_array_type = null;

	System.out.println( "findCommonArrayTypeForMeasurements(): " + sels.length + " Measurements to check" );

 	for( int s = 0 ; s < sels.length ; s++ )
	{
	    Instance array_type = getArrayTypeForMeasurement( cntrl.getConnection(), 
							      browsing_measurements[ sels[ s ] ]  );
	    
	    System.out.println( "findCommonArrayTypeForMeasurements(): " + s + " - " + 
				browsing_measurements[ sels[ s ] ].name + " = " + array_type.name );
	    
	    if( s == 0 )
	    {
		the_common_array_type = new Instance( array_type.name, array_type.id );
	    }
	    else
	    {
		if( array_type.id.equals( the_common_array_type.id ) == false )
		{
		    System.out.println( "findCommonArrayTypeForMeasurements(): " + 
					browsing_measurements[ sels[ s ] ].name + 
					" uses a different ArrayType" );

		    return null;
		}
	    }
	}

	System.out.println( "findCommonArrayTypeForMeasurements(): all use the same ArrayType" );

	return the_common_array_type;
    }


    private Instance getArrayTypeForMeasurement( maxdConnection_m2 mconn, Instance measurement )
    {
	Instance[] array_type_id = cntrl.findLinkedInstances( "Measurement", "ArrayType", measurement );

	if( array_type_id == null )
	    return null;

	if( array_type_id.length == 0 )
	    return null;
	
	return array_type_id[ 0 ];
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    /*
       getting the array description meta-data (i.e. the attribute values for Feature,Reporter and Gene)

       need to format the data into something suitable for the TagAttrs data structure
       which is a hashtable mapping ThingName -> [ collection of values ]
       i.e. ( Spot432 -> [ 2, 4, 'no', 3.5 ] )


    */

    private void loadTagAttrs( final ExprData.TagAttrs tag_attrs, 
			       final java.util.Hashtable instance_to_attr_id )
    {
	// this is used to cache the tag_attr_id's for efficiency
	final java.util.Hashtable tag_attr_ids_ht = new java.util.Hashtable();

	for( Enumeration en1 = instance_to_attr_id.keys(); en1.hasMoreElements();  )
	{
	    final Instance instance = (Instance) en1.nextElement();

	    final String db_attr_id = (String) instance_to_attr_id.get( instance );

	    if( ( db_attr_id != null ) && ( db_attr_id.length() > 0 ) )
	    {
		// get the serialised  attribute values for this instance
	    	final String serialised_attr_values = cntrl.getConnection().getDescription( db_attr_id );
		
		final java.util.Hashtable name_to_val_ht = cntrl.unserialiseValues( serialised_attr_values );
		
		// and store each of the values
		for( Enumeration en2 = name_to_val_ht.keys(); en2.hasMoreElements();  )
		{
		    final String attr_name = (String) en2.nextElement();
		    final String attr_val  = (String) name_to_val_ht.get( attr_name );
		    
		    int tag_attr_id = -1;
		
		    // do we already know the id for this attr_name ?
		    Integer tag_attr_id_i = (Integer) tag_attr_ids_ht.get( attr_name );
		    
		    if( tag_attr_id_i == null )
		    {
			tag_attr_id = tag_attrs.addAttr( attr_name );
			// and cache this new id for next time
			tag_attr_ids_ht.put( attr_name, new Integer( tag_attr_id ) );
		    }
		    else
		    {
			// we already knew the id
			tag_attr_id = tag_attr_id_i.intValue();
		    }
		    
		    // set the value in the TagAttr data structure
		    tag_attrs.setTagAttr( instance.name, tag_attr_id, attr_val );
		}
	    }
	}
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --



    //private boolean optimise_for_small_joins = true;

    boolean debug_connect = false;
	
    private Controller cntrl;

    //private Connection connection = null;
	
    //private Vector expt_data = new Vector();

    //private ExprData.DataTags current_dtags = null;
    //private String            current_array_type_id = null;
    //private int               current_n_spots = 0;
    //private Hashtable         spot_id_to_index_ht = null;

    private ProgressOMeter pm = null;

    //String browse_target_name = null;
    //private JLabel browse_table_label;

    private Instance[] browsing_insts;
    private Instance[] browsing_measurements;
    private MeasDataProp[] browsing_properties;

    private JComboBox browse_table_jcb;
    private JList browse_items_jl;
    private JList browse_measurements_jl;
    private JList browse_properties_jl;


    private JPanel atts_wrapper;
    private JPanel opts_wrapper;

    private maxdView mview;
    private ExprData edata;

    private JFrame frame;
    private JPanel top;

    //private JSplitPane c_split_pane, v_split_pane, h_split_pane;

}
