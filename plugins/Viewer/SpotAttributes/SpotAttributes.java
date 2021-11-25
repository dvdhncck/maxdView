import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import java.util.Date;
import javax.swing.table.*;
import javax.swing.event.*;

public class SpotAttributes implements Plugin, ExprData.ExprDataObserver, ExprData.ExternalSelectionListener
{
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public SpotAttributes(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }


    public void startPlugin()
    {
	frame = createFrame();
	
	mview.decorateFrame( frame );

	frame.setVisible(true);

	mview.getExprData().addObserver(this);

	sel_listener_handler = mview.getExprData().addExternalSelectionListener( this );

	// displayMeasurement();
    }

    public void cleanUp()
    {
	mview.getExprData().removeObserver(this);

	mview.getExprData().removeExternalSelectionListener( sel_listener_handler );

	frame.setVisible(false);

	if( options_frame != null )
	{
	    options_frame.setVisible(false);
	}

	// save properties
	{
	    mview.putBooleanProperty("SpotAttributes.sync_spot_sel",  synchronise_spot_selection );
	    mview.putBooleanProperty("SpotAttributes.sync_meas_sel",  synchronise_meas_selection );
	    mview.putBooleanProperty("SpotAttributes.remember_column_layout",     remember_column_layout );
	    mview.putBooleanProperty("SpotAttributes.remember_column_widths",     remember_column_widths );
	    mview.putBooleanProperty("SpotAttributes.group_spot_attrs_by_name",   group_spot_attrs_by_name );
	    mview.putBooleanProperty("SpotAttributes.always_include_expr_values", always_include_expr_values );
	    mview.putBooleanProperty("SpotAttributes.expand_spot_attr_names",     expand_spot_attr_names );
	    mview.putBooleanProperty("SpotAttributes.expand_name_attr_names",     expand_name_attr_names );

	    mview.putBooleanProperty("SpotAttributes.apply_filter", apply_filter_jchkb.isSelected());
	}
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = 
	   new PluginInfo("Spot Attributes", "viewer", "Displays the Spot Attributes of one or more a Measurements", "", 1, 1, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	return null;
    }

    public void   runCommand(String name, String[] args, CommandSignal done)
    { 
	if(done != null)
	    done.signal();
    } 

    public String pluginType() { return "viewer"; }



 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void clusterSelectionChanged(ExprData.Cluster[] clusters)
    {
    }

    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids)
    {
	// int n_meas = meas_ids == null ? 0 : meas_ids.length;
	// int n_spot = spot_ids == null ? 0 : spot_ids.length;
	// System.out.println("spotMeasurementSelectionChanged() #m=" + n_meas + " #s=" + n_spot);

	importSelection( spot_ids, meas_ids );
    }

    // the main selection has changed, update the table selection
    public void spotSelectionChanged(int[] spot_ids)
    {
	importSelection( spot_ids, null );
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // the table selection has changed, update the main  selection
    public void exportSpotSelection()
    {
	// System.out.println("exportSpotSelection.... update=" + (selection_is_external_update ? "true":"false") );

	if( selection_is_external_update )
	    return;
	    
	if( synchronise_spot_selection )
	{
	    if(reverse_spot_order != null)
	    {
		//System.out.println("exportSpotSelection....");
		
		ListSelectionModel lsm = atts_table.getSelectionModel();
		CustomTableModel ctm = (CustomTableModel) atts_table.getModel();
		
		final int n_rows = ctm.getRowCount();
		
		int[] poss_sels = new int[ n_rows ];   // max possible number of selected things
		int sel_p = 0;
		
		for( int r=0; r < n_rows; r++ )
		    if( lsm.isSelectedIndex( r ))
			poss_sels[ sel_p++ ] = spot_order[ r ];

		
		// prevent a cycle of propagation from ending the known universe
		selection_is_internal_update = true;
		    

		if(sel_p == 0)
		{
		    // nothing selected
		    edata.clearSpotSelection(  );
		}
		else
		{
		    int[] real_sels = new int[ sel_p ];
		    for( int r=0; r < sel_p; r++)
			real_sels[r] = poss_sels[r];
		    
		    edata.setSpotSelection( real_sels );
		}


		    
		selection_is_internal_update = false;
	    }
	}
    }


    // the table selection has changed, update the main  selection
    public void exportMeasurementSelection()
    {
	// System.out.println("exportMeasurementSelection.... update=" + (selection_is_external_update ? "true":"false") );

	if( selection_is_external_update )
	    return;
	    
	if( synchronise_meas_selection )
	{
	    Object[] sel_meas_names = meas_list.getSelectedValues();

	    selection_is_internal_update = true;
		
	    if( sel_meas_names == null)
	    {
		edata.clearMeasurementSelection();
	    }
	    else
	    {
		int[] meas_ids = new int[sel_meas_names.length];

		for(int m=0; m < sel_meas_names.length; m++)
		{
		    meas_ids[m] = edata.getMeasurementFromName( (String) sel_meas_names[m] );
		}

		edata.setMeasurementSelection( meas_ids );
	    }

	    selection_is_internal_update = false;
		
	}
    }

    public void importSelection()
    {
	int[] sel_spot_ids = null;
	int[] sel_meas_ids = null;

	if( synchronise_spot_selection )
	    sel_spot_ids = edata.getSpotSelection();
	
	if( synchronise_meas_selection )
	    sel_meas_ids = edata.getMeasurementSelection();
	
	if( synchronise_spot_selection || synchronise_meas_selection )
	    importSelection( sel_spot_ids, sel_meas_ids );
    }

    public void importSelection( int[] spot_ids, int[] meas_ids )
    {

	if( selection_is_internal_update )
	    return;

	if( synchronise_meas_selection )
	{
	    // update the meas list selection

	    if( meas_ids != null )
	    {
		int[] list_index = new int[ meas_ids.length ];
		
		for(int m=0; m < meas_ids.length; m++)
		    list_index[m] = edata.getIndexOfMeasurement( meas_ids[m] );
		
		selection_is_external_update = true;
	    
		meas_list.setSelectedIndices( list_index );

		selection_is_external_update = false;
	    }
	}

	if( synchronise_spot_selection )
	{
	    // update the table selection
	    
	    ListSelectionModel lsm = new DefaultListSelectionModel();
	    
	    for(int s=0; s < spot_ids.length; s++)
	    {
		int table_index = reverse_spot_order[ spot_ids[s]];
		lsm.addSelectionInterval( table_index, table_index );
	    }
	    
	    lsm.addListSelectionListener( selection_listener );
	    
	    selection_is_external_update = true;
	    
	    atts_table.setSelectionModel( lsm ); 
	    
	    selection_is_external_update = false;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //

    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	updateTable( true, false );
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	//populateNameAttrList();

	//populateMeasurementList(null);

	//updatePickLists();
	
	populateListWithMeasurements();

	updateTable( true, true );
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   SpotAttributes
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JFrame createFrame()
    {
	JFrame frame = new JFrame("Spot Attributes Viewer");

	// mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
	    });


	// load properties
	{
	    synchronise_spot_selection = mview.getBooleanProperty("SpotAttributes.sync_spot_sel", true);
	    synchronise_meas_selection = mview.getBooleanProperty("SpotAttributes.sync_meas_sel", true);
	    remember_column_layout =     mview.getBooleanProperty("SpotAttributes.remember_column_layout", true);
	    remember_column_widths =     mview.getBooleanProperty("SpotAttributes.remember_column_widths",  true);
	    group_spot_attrs_by_name =   mview.getBooleanProperty("SpotAttributes.group_spot_attrs_by_name", false);
	    always_include_expr_values = mview.getBooleanProperty("SpotAttributes.always_include_expr_values", true);
	    expand_spot_attr_names =     mview.getBooleanProperty("SpotAttributes.expand_spot_attr_names", true);
	    expand_name_attr_names =     mview.getBooleanProperty("SpotAttributes.expand_name_attr_names", true);
	}

	panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	GridBagLayout gridbag = new GridBagLayout();
	//panel.setPreferredSize(new Dimension(400, 300));
	panel.setLayout(gridbag);

	JSplitPane split_pane = new JSplitPane();

	{
	    JPanel pick_wrapper = new JPanel();
	    //pick_wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
	    GridBagLayout pick_gridbag = new GridBagLayout();
	    pick_wrapper.setLayout(pick_gridbag);
	    GridBagConstraints c;

	    // left hand side: the pick list

	    ButtonGroup bg = new ButtonGroup();
	    JRadioButton jrb;

	    jrb = new JRadioButton("Names & Name Attrs");
	    jrb.setSelected(list_mode == 0);
	    jrb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    list_mode = 0;
		    displayCorrectList();
		}
	    });
	    c = new GridBagConstraints();
	    c.gridy = 0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    pick_gridbag.setConstraints(jrb, c);
	    pick_wrapper.add(jrb);
	    bg.add(jrb);


	    jrb = new JRadioButton("Measurements");
	    jrb.setSelected(list_mode == 1);
	    jrb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    list_mode = 1;
		    displayCorrectList();
		}
	    });
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.gridwidth = 2;
	     c.anchor = GridBagConstraints.NORTHWEST;
	    pick_gridbag.setConstraints(jrb, c);
	    pick_wrapper.add(jrb);
	    bg.add(jrb);


	    jrb = new JRadioButton("Spot Attrs");
	    jrb.setSelected(list_mode == 2);
	    jrb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    list_mode = 2;
		    displayCorrectList();
		}
	    });
	    c = new GridBagConstraints();
	    c.gridy = 2;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    pick_gridbag.setConstraints(jrb, c);
	    pick_wrapper.add(jrb);
	    bg.add(jrb);


	    name_name_attr_list = new JList();
	    name_name_attr_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if(! e.getValueIsAdjusting() )
			    listSelectionHasChanged();
		    }
		});
	    
	    meas_list = new JList();
	    meas_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if(! e.getValueIsAdjusting() )
			{
			    listSelectionHasChanged();

			    exportMeasurementSelection();
			}

		    }
		});
	    
	    spot_attr_list = new JList();
	    spot_attr_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if(! e.getValueIsAdjusting() )
			    listSelectionHasChanged();
		    }
		});

	    list_wrapper = new JPanel();
	    list_jsp = new JScrollPane( list_wrapper );
	    c = new GridBagConstraints();
	    c.gridy = 3;
	    c.weighty = 9.0;
	    c.weightx = 10.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.fill = GridBagConstraints.BOTH;
	    pick_gridbag.setConstraints(list_jsp, c);
	    pick_wrapper.add(list_jsp);

	    displayCorrectList( );


	    JButton jb = new JButton("All");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			selectAllInCurrentList( );
		    }
		});
	    jb.setMargin(new Insets(0,1,0,1));
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 4;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    pick_gridbag.setConstraints(jb, c);
	    pick_wrapper.add(jb);

	    jb = new JButton("None");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			selectNoneInCurrentList( );
		    }
		});jb.setMargin(new Insets(0,1,0,1));
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 4;
	    c.gridx = 1;
	    c.anchor = GridBagConstraints.EAST;
	    pick_gridbag.setConstraints(jb, c);
	    pick_wrapper.add(jb);


	    /*
	    spot_name_attr_labels_list = new JList();
	    jsp = new JScrollPane( spot_name_attr_labels_list );
	    left_split_pane.setTopComponent( jsp );
	    spot_name_attr_labels_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if(! e.getValueIsAdjusting() )
			  updateTable( false, true );
		    }
		});

	    meas_spot_attr_list = new JList();
	    jsp = new JScrollPane( meas_spot_attr_list );
	    left_split_pane.setBottomComponent( jsp );
	    meas_spot_attr_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if(! e.getValueIsAdjusting() )
			    updateTable( false, true );
		    }
		});
	    
	    // JSplitPane left_split_pane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
	    */

	    
	    split_pane.setLeftComponent( pick_wrapper );
	}

	{
	    // right hand side: the viewing table

	    table_model = new CustomTableModel();
	    atts_table = new JTable( table_model );
	    atts_table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
	    JScrollPane jsp = new JScrollPane(atts_table);

	    selection_listener = new ListSelectionListener() 
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			// propagate the table selection back to the main spot selection
			if( ! e.getValueIsAdjusting() )
			    exportSpotSelection();
		    }
		};
	    atts_table.getSelectionModel().addListSelectionListener( selection_listener );

	    split_pane.setRightComponent( jsp );
	}

	{
	    // add the split pane to the panel

	    GridBagConstraints c = new GridBagConstraints();
	    c.weightx = 10.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(split_pane, c);
	    panel.add(split_pane);

	}

	{ 
	    // the control buttons

	    JPanel button_wrapper = new JPanel();
	    button_wrapper.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
	    GridBagLayout button_gridbag = new GridBagLayout();
	    button_wrapper.setLayout(button_gridbag);
	    GridBagConstraints c;
	    int col =0;
	    Dimension fillsize = new Dimension(10,10);
	    Box.Filler filler;

	    // ---------------------

	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter_jchkb.setSelected( mview.getBooleanProperty( "SpotAttributes.apply_filter", false ));
	    apply_filter_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateTable( true, false );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.WEST;
	    button_gridbag.setConstraints(apply_filter_jchkb, c);
	    button_wrapper.add(apply_filter_jchkb);
	    
	    
	    filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    button_gridbag.setConstraints(filler, c);
	    button_wrapper.add(filler);
	   	

	    final JButton djb = new JButton("Delete");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.anchor = GridBagConstraints.EAST;
	    button_gridbag.setConstraints(djb, c);
	    button_wrapper.add(djb);
	    djb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			deleteAttributes();
		    }
		});
	    
	    
	    final JButton rjb = new JButton("Rename");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.anchor = GridBagConstraints.EAST;
	    button_gridbag.setConstraints(rjb, c);
	    button_wrapper.add(rjb);
	    rjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			renameAttributes();
		    }
		});
	  

	    filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.EAST;
	    button_gridbag.setConstraints(filler, c);
	    button_wrapper.add(filler);
	    

	    final JButton ojb = new JButton("Options");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    button_gridbag.setConstraints(ojb, c);
	    button_wrapper.add(ojb);
	    ojb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayOptions();
		    }
		});
	   
	    final JButton hjb = new JButton("Help");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.EAST;
	    button_gridbag.setConstraints(hjb, c);
	    button_wrapper.add(hjb);
	    hjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("SpotAttributes", "SpotAttributes");
		    }
		});
	   

	    final JButton cjb = new JButton("Close");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.EAST;
	    button_gridbag.setConstraints(cjb, c);
	    button_wrapper.add(cjb);
	    cjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    

	    // ---------------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(button_wrapper, c);
	    panel.add(button_wrapper);
	}

	updatePickLists();
	displayCorrectList();
	updateTable(true, true);

	frame.getContentPane().add(panel, BorderLayout.CENTER);
	frame.pack();

	return frame;
    }
    
    private class CustomActionListener implements ActionListener
    {
	public void actionPerformed(ActionEvent e) 
	{
	    // setMeasurement((String)meas_jcb.getSelectedItem());
	}
    }

    //
    // =====================================================================
    // list populating
    // =====================================================================
    //


    // ******************************************
    //
    // cannot use 'NameTagSelection.getNameTagArray' because the
    // current version (in ExprData) doesn't quite work as desired:
    //   it only returns SpotAttr values if the corresponding Name is enabled,
    //   for example, Spot.COMMENT cannot be retrieved if SpotName is not also retrieved
    //
    // ******************************************
    
    private class NameNameAttrID
    {
	int name_id;
	int name_attr_id;

	public NameNameAttrID(int n_id, int na_id)
	{
	    name_id = n_id;
	    name_attr_id = na_id;
	}

	public String getName()
	{
	    switch( name_id )
	    {
	    case 0:
		if( name_attr_id == -1 )
		    return "Spot name";
		else
		    return edata.getSpotTagAttrs().getAttrName( name_attr_id );

	    case 1:
		if( name_attr_id == -1 )
		    return "Probe name";
		else
		    return edata.getProbeTagAttrs().getAttrName( name_attr_id );

	    case 2:
		if( name_attr_id == -1 )
		    return "Gene name(s)";
		else
		    return edata.getGeneTagAttrs().getAttrName( name_attr_id );
	    }
	    return "!ERROR!";
	}
    }

    private void updatePickLists( )
    {
	populateListWithNamesAndNameAttrs();
	populateListWithMeasurements();
	populateListWithSpotAttrs();
    }

    private void displayCorrectList( )
    {
	// System.out.println("list_mode=" + list_mode);

	list_wrapper.removeAll();
	
	GridBagLayout gridbag = new GridBagLayout();
	list_wrapper.setLayout(gridbag);

	JComponent component;

	switch( list_mode )
	{
	case 0: 
	    component = name_name_attr_list;
	    break;
	case 1: 
	    component = meas_list;
	    break;
	default: 
	    component = spot_attr_list;
	    break;
	}
	
	
	GridBagConstraints c = new GridBagConstraints();
	c.weightx = c.weighty = 10.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(component, c);
	list_wrapper.add( component );
	list_jsp.revalidate();	
	list_jsp.repaint();	
    }

    private void selectAllInCurrentList()
    {
	JList list;
	switch( list_mode )
	{
	case 0: 
	    list = name_name_attr_list;
	    break;
	case 1: 
	    list = meas_list;
	    break;
	default: 
	    list = spot_attr_list;
	    break;
	}
	list.setSelectionInterval(0, list.getModel().getSize()-1);
    }
    private void selectNoneInCurrentList()
    {
	JList list;
	switch( list_mode )
	{
	case 0: 
	    list = name_name_attr_list;
	    break;
	case 1: 
	    list = meas_list;
	    break;
	default: 
	    list = spot_attr_list;
	    break;
	}
	list.clearSelection();
    }


    private void populateListWithNamesAndNameAttrs( )
    {
	String[] cur_sel = getListSelection( name_name_attr_list );

	Vector names = new Vector();
	ExprData.TagAttrs ta;
	int pos = 0;

	name_attr_list_index_to_data_ht = new Hashtable();

	names.addElement("Gene name(s)");
	name_attr_list_index_to_data_ht.put( new Integer(pos++), new NameNameAttrID( 2, -1 ));

	ta = edata.getGeneTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    names.addElement( "  " + ta.getAttrName(a) );
	    name_attr_list_index_to_data_ht.put( new Integer(pos++), new NameNameAttrID( 2, a ));
	}

	names.addElement("Probe name");
	name_attr_list_index_to_data_ht.put( new Integer(pos++), new NameNameAttrID( 1, -1 ));

	ta = edata.getProbeTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    names.addElement( "  " + ta.getAttrName(a) );
	    name_attr_list_index_to_data_ht.put( new Integer(pos++), new NameNameAttrID( 1, a ));
	}
 
	names.addElement("Spot name");
	name_attr_list_index_to_data_ht.put( new Integer(pos++), new NameNameAttrID( 0, -1 ));

	ta = edata.getSpotTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    names.addElement( "  " + ta.getAttrName(a) );
	    name_attr_list_index_to_data_ht.put( new Integer(pos++), new NameNameAttrID( 0, a ));
	}

	name_name_attr_list.setListData( names );

	setListSelection( name_name_attr_list, cur_sel );
    }

    private void populateListWithMeasurements( )
    {
	String[] cur_sel = getListSelection( meas_list );

	Vector names = new Vector();
	
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int m_id = edata.getMeasurementAtIndex(m);
	    
	    names.addElement( edata.getMeasurementName( m_id ) );
	}

	meas_list.setListData( names );

	setListSelection( meas_list, cur_sel );

    }

    private void populateListWithSpotAttrs( )
    {
	String[] cur_sel = getListSelection( spot_attr_list );

	Vector names = new Vector();

	HashSet all_spot_attr_names_hs = new HashSet();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    ExprData.Measurement meas = edata.getMeasurement( m );
		
	    for(int a=0; a < meas.getNumSpotAttributes(); a++)
	    {
		all_spot_attr_names_hs.add( meas.getSpotAttributeName( a ) );
	    }
	}

	String[] names_a = (String[]) all_spot_attr_names_hs.toArray( new String[0] );

	Arrays.sort( names_a );

	spot_attr_list.setListData( names_a );

	setListSelection( spot_attr_list, cur_sel );
    }

    private void listSelectionHasChanged( )
    {
	updateTable( false, true );
    }

    private boolean isSelected( MeasSpotAttrID msa_id, MeasSpotAttrID[] selection )
    {
	if(selection == null)
	    return false;
	
	for(int i=0; i < selection.length; i++)
	    if((selection[i].meas_id == msa_id.meas_id) && (selection[i].spot_attr_id == msa_id.spot_attr_id))
		return true;

	return false;
    }

    //
    // =====================================================================
    // renameAttribute()
    // =====================================================================
    //

    private int[] getSelectedMeasurements()
    {
	Object[] sel_meas_names = meas_list.getSelectedValues();

	if((sel_meas_names == null) || (sel_meas_names.length == 0))
	    return null;

	int[] m_ids = new int[sel_meas_names.length];

	for(int m=0; m < sel_meas_names.length; m++)
	{
	    String m_name = (String) sel_meas_names[m];
	    m_ids[m]      = edata.getMeasurementFromName( m_name );
	}

	return m_ids;
    }


    private String[] getSelectedAttributes()
    {
	Object[] sel_attr_names_o = spot_attr_list.getSelectedValues();

	if(( sel_attr_names_o == null) || ( sel_attr_names_o.length == 0))
	    return null;

	String[] sel_attr_names = new String[ sel_attr_names_o.length ];
	for(int s=0; s < sel_attr_names_o.length; s++)
	    sel_attr_names[s] = (String) sel_attr_names_o[s];

	return sel_attr_names;
    }

    private String getDescriptionOfMeasurements( int[] meas_ids )
    {
	if(( meas_ids == null ) || (meas_ids.length == 0 ))
	    return "no Measurements";

	if( meas_ids.length == 1 )
	    return "Measurement '" + edata.getMeasurementName( meas_ids[0] ) + "'";

	if( meas_ids.length == 2 )
	    return 
		"Measurements '" + 
		edata.getMeasurementName( meas_ids[0] ) + "' and '" +
		edata.getMeasurementName( meas_ids[1] ) + "'";

	return "the " + meas_ids.length + " selected Measurements";
    }

    private String getDescriptionOfSpotAttributes( String[] sa_names )
    {
	if((  sa_names == null ) || ( sa_names.length == 0 ))
	    return "no Spot Attributes";

	if(  sa_names.length == 1 )
	    return "Spot Attribute '" + sa_names[0] + "'";

	if(  sa_names.length == 2 )
	    return "Spot Attributes '" + sa_names[0] + "' and '" + sa_names[1] + "'";

	return "the " +  sa_names.length + " selected Spot Attributes";	
    }


    public void renameAttributes()
    {
	int[] sel_meas_ids      = getSelectedMeasurements();
	String[] sel_attr_names = getSelectedAttributes();
	
	if( sel_meas_ids == null )
	{
	    mview.alertMessage("Select one or more Measurements first");
	    return;
	}
	if(( sel_attr_names == null ) || (sel_attr_names.length != 1))
	{
	    mview.alertMessage("Select exactly one Spot Attribute to rename");
	    return;
	}
	
	try
	{
	    String old_name = sel_attr_names[0];
		
	    String msg = 
		"Really rename Spot Attribute '" + old_name + "' in " + 
		getDescriptionOfMeasurements( sel_meas_ids ) + " ?";
	    
	    if( mview.infoQuestion( msg, "Yes", "No") == 0 )
	    {
		
		String new_name = mview.getString("New name for '" + old_name + "':", old_name);
		
		for(int m=0; m < sel_meas_ids.length; m++)
		{
		    ExprData.Measurement meas = edata.getMeasurement( sel_meas_ids[m] );
		    
		    for(int sa=0; sa < meas.getNumSpotAttributes(); sa++)
		    {
			//System.out.println("checking " + meas.getName() + " sa_id " + sa);
			
			if( meas.getSpotAttributeName( sa ).equals( old_name ) )
			{
			    //System.out.println("....hit!");

			    meas.setSpotAttributeName( sa, new_name );
			}
		    }
		}

		//System.out.println("done.");
		populateListWithSpotAttrs();
		edata.generateMeasurementUpdate( ExprData.NameChanged );
		
	    }
	}
	catch(UserInputCancelled uic)
	{
	}


    }

    
    //
    // =====================================================================
    // deleteAttribute()
    // =====================================================================
    //

    public void deleteAttributes()
    {
	int[] sel_meas_ids      = getSelectedMeasurements();
	String[] sel_attr_names = getSelectedAttributes();
	
	if( sel_meas_ids == null )
	{
	    mview.alertMessage("Select one or more Measurements first");
	    return;
	}
	if( sel_attr_names == null )
	{
	    mview.alertMessage("Select one or more Spot Attribute first");
	    return;
	}
	
	String msg = 
	    "Really delete " + 
	    getDescriptionOfSpotAttributes(sel_attr_names)  + " in " + 
	    getDescriptionOfMeasurements( sel_meas_ids ) + " ?";
	
	if( mview.infoQuestion( msg, "Yes", "No") == 0 )
	{ 
	    // a little bit tricky because, as we delete each SpotAttr, 
	    // the indices of the other SpotAttrs will change - so it should 
	    // be done as a multi-pass operation, one SpotAttr at a time.

	    
	    for(int sa=0; sa < sel_attr_names.length; sa++)
	    {
		final String target_name = sel_attr_names[sa];
		
		for(int m=0; m < sel_meas_ids.length; m++)
		{
		    ExprData.Measurement meas = edata.getMeasurement( sel_meas_ids[m] );
		    
		    int sa_id = meas.getSpotAttributeFromName( target_name );

		    if( sa_id >= 0 )
			meas.removeSpotAttribute( sa_id );

		    
		}
	    }

	    //System.out.println("done.");
	    populateListWithSpotAttrs();
	    edata.generateMeasurementUpdate( ExprData.NameChanged );
	}
	
    }


    //
    // =====================================================================
    // buildSpotList()
    // =====================================================================
    //

    private int[] spot_order = new int[0];
    private int[] reverse_spot_order = new int[0];

    private void buildSpotList()
    {
	boolean apply_filter = apply_filter_jchkb.isSelected();

	int ns = 0;

	final int nspt = edata.getNumSpots();
	    
	if(apply_filter)
	{
	    for(int s=0; s < nspt; s++)
		if(!edata.filter(s))
		    ns++;
	}
	else
	{
	    ns = edata.getNumSpots();
	}

	// System.out.println("expecting " + ns + " spots");

	spot_order = new int[ns];

	reverse_spot_order = new int[ edata.getNumSpots() ];

	int si = 0;
	
	if(apply_filter)
	{
	    for(int s=0; s < nspt; s++)
	    {
		int sid = edata.getSpotAtIndex(s);
		if(!edata.filter(sid))
		{
		    spot_order[si] = sid;
		    reverse_spot_order[sid] = si;
		    si++;
		}
	    }
	}
	else
	{
	    for(int s=0; s < nspt; s++)
	    {
		spot_order[s] = edata.getSpotAtIndex(s);
		reverse_spot_order[ spot_order[s] ] = s;
	    }
	}

    }


    //
    // =====================================================================
    // CustomTableModel
    // =====================================================================
    //


    public class CustomTableModel extends javax.swing.table.AbstractTableModel
    {
	public int getRowCount()    { return spot_order.length; }

	public int getColumnCount() { return n_name_cols + n_data_cols; }

	public Object getValueAt(int row, int col)
	{
	    String result = null;

	    try
	    {
		if( col < n_name_cols )
		{
		    // it's a Name or NameAttribute column 

		    NameNameAttrID nna = data_cols_nna[ col ];

		    if( nna.name_attr_id == -1 )
		    {
			// it's a Name (rather than a NameAttribute)

			switch( nna.name_id )
			{
			case 0:
			    result = edata.getSpotName( spot_order[row] );
			    break;
			case 1:
			    result = edata.getProbeName( spot_order[row] );
			    break;
			case 2:
			    result = edata.getGeneName( spot_order[row] );
			    break;
			}
		    }
		    else
		    {
			// it's a NameAttribute
			ExprData.TagAttrs ta;
			String name;

			switch( nna.name_id )
			{
			case 0:
			    name = edata.getSpotName( spot_order[row] );
			    ta = edata.getSpotTagAttrs();
			    result = ta.getTagAttr( name, nna.name_attr_id );
			    break;
			
			case 1:
			    name = edata.getProbeName( spot_order[row] );
			    ta = edata.getProbeTagAttrs();
			    result = ta.getTagAttr( name, nna.name_attr_id );
			    break;
			
			case 2:
			    // special handling for gene names
			    //
			    String[] gnames = edata.getGeneNames( spot_order[row] );
			    if((gnames == null) || (gnames.length == 0))
			    {
				result = null;
			    }
			    else
			    {
				result = "";
				ta = edata.getGeneTagAttrs();
				for(int g=0; g < gnames.length; g++)
				{
				    String ta_val = ta.getTagAttr( gnames[g], nna.name_attr_id );
				    if(ta_val != null)
				    {
					if(g > 0)
					    result += " ";
					result += ta_val;
				    }
				}
			    }
			    break;
			}
		    }

		}
		else
		{
		    // it's a Measurement or SpotAttribute column 

		    MeasSpotAttrID msa = data_cols_msa[ col - n_name_cols ];
		    if( msa.isSpotAttr() )
		    {
			ExprData.Measurement meas = edata.getMeasurement( msa.meas_id );
			result = meas.getSpotAttributeDataValueAsString( msa.spot_attr_id, spot_order[row] );
		    }
		    else
		    {
			result = String.valueOf( edata.eValue( msa.meas_id, spot_order[row] ));
		    }
		}
	    }
	    catch(Throwable t)
	    {
		System.err.println("getValueAt():" + t); 
		// t.printStackTrace();
		result =  "!ERROR!";
	    }

	    return (result == null) ? "" : result;
	}

	public String getColumnName(int col) 
	{
	    if( col < n_name_cols )
	    {
		return name_cols[col]; 
	    }
	    else
	    {
		return data_cols[ col - n_name_cols ];
	    }
	}

	public int n_name_cols;
	public int n_data_cols;

	public MeasSpotAttrID[] data_cols_msa;
	public NameNameAttrID[] data_cols_nna;

	public String[] data_cols;
	public String[] name_cols;
    }

    //
    // =====================================================================
    // updateTable()
    // =====================================================================
    //

    private int ut_count = 0;

    private void updateTable( boolean rows_changed, boolean cols_changed )
    {
	// System.out.println( "updateTable() " + (ut_count++) );
	    
	rememberCurrentColumnLayout();

	CustomTableModel table_model = new CustomTableModel();

	if( cols_changed )
	{
	    // how many names/name_attrs are selected?
	    
	    Vector nna_v      = new Vector();
	    Vector nna_name_v = new Vector();
	    
	    for(int n=name_name_attr_list.getMinSelectionIndex(); 
		n <= name_name_attr_list.getMaxSelectionIndex(); 
		n++)
	    {
		if( name_name_attr_list.isSelectedIndex(n) )
		{
		    NameNameAttrID nna = (NameNameAttrID) name_attr_list_index_to_data_ht.get( new Integer( n ) );
		    if(nna != null)
		    {
			nna_v.addElement( nna );
			nna_name_v.addElement( nna.getName() );
		    }
		}
	    }
	    
	    NameNameAttrID[] nna_a = (NameNameAttrID[]) nna_v.toArray( new NameNameAttrID[0] );
	    String[] nna_names     = (String[])         nna_name_v.toArray( new String[0] );
	    
	    table_model.n_name_cols   = nna_a.length;
	    table_model.name_cols     = nna_names;
	    table_model.data_cols_nna = nna_a;
	    
	    //System.out.println( table_model.n_name_cols + " name cols");
	    
	    // how many measurements/spot_attrs are selected?


	    // first: build a lookup table of the selected  spot_attrs
	    //  (as they will be displayed in all selected measurements)

	    HashSet selected_spot_attr_names_hs = new HashSet();
	    Object[] sel_spot_attr_names = spot_attr_list.getSelectedValues();
	    for(int sa=0; sa < sel_spot_attr_names.length; sa++)
		selected_spot_attr_names_hs.add( sel_spot_attr_names[sa] );

	    Vector msa_v      = new Vector();
	    Vector msa_name_v = new Vector();
	    MeasSpotAttrID msa;

	    // now scan each measurement
	    Object[] sel_meas_names = meas_list.getSelectedValues();
	    
	    for(int m=0; m < sel_meas_names.length; m++)
	    {
		final String m_name             = (String) sel_meas_names[m];
		final int m_id                  = edata.getMeasurementFromName( m_name );
		final ExprData.Measurement meas = edata.getMeasurement( m_id );
		
		if(meas != null)
		{
		    if( always_include_expr_values )
		    {
			msa = new MeasSpotAttrID( m_id, -1 );
			msa_v.addElement( msa );
			msa_name_v.addElement( m_name );
		    }
		    
		    for(int a_id=0; a_id < meas.getNumSpotAttributes(); a_id++)
		    {
			final String sa_name =  meas.getSpotAttributeName( a_id );
			
			if( selected_spot_attr_names_hs.contains( sa_name ) )
			{
			    msa = new MeasSpotAttrID( m_id, a_id );
			    msa_v.addElement( msa );
			    
			    if(expand_spot_attr_names)
				msa_name_v.addElement( m_name + "." + sa_name );
			    else
				msa_name_v.addElement( sa_name );
			}
		    }
		}
	    }

	    MeasSpotAttrID[] msa_a = (MeasSpotAttrID[]) msa_v.toArray( new MeasSpotAttrID[0] );
	    String[] msa_names     = (String[])         msa_name_v.toArray( new String[0] );
	    
	    table_model.n_data_cols   = msa_a.length;
	    table_model.data_cols     = msa_names;
	    table_model.data_cols_msa = msa_a;
	    
	    //System.out.println( table_model.n_data_cols + " data cols");
	}
	else
	{
	   CustomTableModel existing_table_model = (CustomTableModel) atts_table.getModel();
	   
	   table_model.n_name_cols   = existing_table_model.n_name_cols;
	   table_model.name_cols     = existing_table_model.name_cols;
	   table_model.data_cols_nna = existing_table_model.data_cols_nna;
	   			                                 
	   table_model.n_data_cols   = existing_table_model.n_data_cols;
	   table_model.data_cols     = existing_table_model.data_cols;
	   table_model.data_cols_msa = existing_table_model.data_cols_msa;
	   
	   //System.out.println( "recycling previous column info");
	}


	if( rows_changed )
	{
	    buildSpotList();

	    //System.out.println( spot_order.length + " data rows");
	}
	

	// prevent the table update triggering an update of the spot selection
	selection_is_external_update = true;
	atts_table.setModel( table_model );
	selection_is_external_update = false;

	
	if( synchronise_spot_selection )
	{
	    importSelection( edata.getSpotSelection(), null );
	}

	
	restorePreviousColumnLayout();

    }

    //
    // =====================================================================
    // column layout
    // =====================================================================
    //

    Hashtable column_size_ht;
    Hashtable column_order_ht;

    private void rememberCurrentColumnLayout()
    {
	TableColumnModel tcm = atts_table.getColumnModel();
	
	column_order_ht = new Hashtable();
	column_size_ht  = new Hashtable();

	String prev_name = null;

	// in order to be able to restore the column ordering,
	// store a set of (col_name, prev_col_name) pairs in a hashtable
	
	// also store the current width of the column in 
	// a hashtable, indexed by the name of the column

	for(int c=0; c < tcm.getColumnCount(); c++)
	{
	    TableColumn tc = tcm.getColumn(c);
	    String name = (String) tc.getHeaderValue();

	    if(prev_name != null)
		column_order_ht.put( name, prev_name );

	    column_size_ht.put( name, new Integer( tc.getWidth() ));

	    //System.out.println(c + "=" + name + "(" + tc.getWidth() + ")" );

	    prev_name = name;
	}
    }

    private void restorePreviousColumnLayout()
    {
	// restore the previous widths

	TableColumnModel tcm = atts_table.getColumnModel();
	
	if( remember_column_widths )
	{
	    for(int c=0; c < tcm.getColumnCount(); c++)
	    {
		TableColumn tc = tcm.getColumn(c);
		
		String name = (String) tc.getHeaderValue();
		
		tc.setIdentifier( name );
		
		Integer prev_width_i = (Integer) column_size_ht.get( name );
		if( prev_width_i != null )
		{
		    int prev_width = prev_width_i.intValue();
		    
		    if( tc.getWidth() != prev_width )
		    {
			tc.setWidth( prev_width );
			tc.setPreferredWidth( prev_width );
			//tc.setMinWidth( prev_width );
			
			//System.out.println("restore width of " + name + " to " + prev_width );
		    }
		}
	    }
	}

	// restore the previous order (where possible)

	// for each pair of (col_name, prev_col_name), try to
	// move the 'col_name' to the right of the 'prev_col_name'
	
	if( remember_column_widths )
	{
	    while( column_order_ht.size() > 0 )
	    {
		String col_name      = getFirstKey( column_order_ht );
		String prev_col_name = (String) column_order_ht.get( col_name );
		
		try
		{
		    int col_index      = tcm.getColumnIndex( col_name );
		    int left_col_index = tcm.getColumnIndex( prev_col_name );
		    
		    //System.out.println( col_name + " (in pos " + col_index + 
		    //		")  should be after " + prev_col_name + " (in pos " +
		    //		left_col_index + ")" );
		    
		    while( col_index < left_col_index )
		    {
			tcm.moveColumn( col_index, col_index+1 );
			
			col_index      = tcm.getColumnIndex( col_name );
			left_col_index = tcm.getColumnIndex( prev_col_name );
		    }
		}
		catch(IllegalArgumentException iae)
		{
		    //System.out.println( "pants!");
		}
		
		column_order_ht.remove( col_name );
	    }
	}
    }

    private String getFirstKey( Hashtable ht )
    {
	Enumeration en = ht.keys();
	return (String) en.nextElement();
    }
    //
    // =====================================================================
    // options panel
    // =====================================================================
    //

    JFrame options_frame = null;

    private void displayOptions()
    {
	if( options_frame != null )
	{
	    options_frame.setVisible(true);
	    return;
	}

	options_frame = new JFrame("Spot Attributes Viewer Options");
	
	options_frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    options_frame = null;
		}
	    });
	// mview.decorateFrame(options_frame);


	JPanel options_panel = new JPanel();
	options_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout gridbag = new GridBagLayout();
	options_panel.setLayout(gridbag);
	GridBagConstraints c;
	JCheckBox jchkb;
	Dimension fillsize = new Dimension(10,10);
	Box.Filler filler;

	int line = 0;
	//  - - - - - - - - - - - - - - - - 

	JLabel label = new JLabel("Spot Attributes Viewer Options");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	gridbag.setConstraints(label, c);
	options_panel.add(label);
	

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);
	   
	
	jchkb = new JCheckBox( "Synchronise the Spot selection " );
	jchkb.setSelected( synchronise_spot_selection );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    synchronise_spot_selection = ((JCheckBox)e.getSource()).isSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);
	
	label = new JLabel("        ( with the selection in the main display )");
	label.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 0.5;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints(label, c);
	options_panel.add(label);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	jchkb = new JCheckBox( "Synchronise the Measurement selection " );
	jchkb.setSelected( synchronise_meas_selection );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    synchronise_meas_selection = ((JCheckBox)e.getSource()).isSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);
	
	label = new JLabel("        ( with the selection in the main display )");
	label.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 0.5;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints(label, c);
	options_panel.add(label);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	jchkb = new JCheckBox( "Always display the Measurement values" );
	jchkb.setSelected( always_include_expr_values );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		   always_include_expr_values = ((JCheckBox)e.getSource()).isSelected();
		   updateTable( false, true );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);
	
	label = new JLabel("        ( otherwise only display selected SpotAttr values )");
	label.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 0.5;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints(label, c);
	options_panel.add(label);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	jchkb = new JCheckBox( "Expand SpotAttribute Names" );
	jchkb.setSelected( expand_spot_attr_names );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    expand_spot_attr_names = ((JCheckBox)e.getSource()).isSelected();
		    updateTable( false, true );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);

	label = new JLabel("        ( i.e. Measurement_7 . Attribute_X )");
	label.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 0.5;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints(label, c);
	options_panel.add(label);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	jchkb = new JCheckBox( "Group SpotAttribute columns together" );
	jchkb.setSelected( group_spot_attrs_by_name );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    group_spot_attrs_by_name = ((JCheckBox)e.getSource()).isSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);

	label = new JLabel("        ( based on their names )");
	label.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 0.5;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints(label, c);
	options_panel.add(label);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	jchkb = new JCheckBox( "Attempt to remember column layout" );
	jchkb.setSelected( remember_column_layout );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    remember_column_layout = ((JCheckBox)e.getSource()).isSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);
	
	label = new JLabel("        ( doesn't always work properly )");
	label.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 0.5;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints(label, c);
	options_panel.add(label);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	jchkb = new JCheckBox( "Attempt to remember column widths" );
	jchkb.setSelected( remember_column_widths );
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    remember_column_widths = ((JCheckBox)e.getSource()).isSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	options_panel.add(jchkb);
	

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	options_panel.add(filler);


	JButton jb = new JButton("Close");
	jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			options_frame.setVisible(false);
			options_frame = null;

			importSelection();
		    }
		});
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weighty = 1.0;
	c.anchor = GridBagConstraints.SOUTH;
	gridbag.setConstraints(jb, c);
	options_panel.add(jb);
	
	//  - - - - - - - - - - - - - - - - 

	options_frame.getContentPane().add(options_panel, BorderLayout.CENTER);
	options_frame.pack();
	options_frame.setVisible(true);
    }


    //
    // =====================================================================
    // handy list selcetion manipulation methods
    // =====================================================================
    //

    public String[] getListSelection( JList list )
    {
	Object[] sel = list.getSelectedValues();
	if(sel == null)
	    return null;

	String[] s_sel = new String[ sel.length ];
	
	for(int si=0; si < sel.length; si++)
	    s_sel[si] = (String) sel[si];
	
	return s_sel;
    }

    public void setListSelection( JList list, String[] items )
    {
	if(( items == null )  || (items.length == 0))
	{
	    list.clearSelection();
	    return;
	}

	try
	{
	    javax.swing.ListModel dlm = (javax.swing.ListModel) list.getModel();
	    
	    java.util.Hashtable ht = new java.util.Hashtable();
	    for(int i=0; i < dlm.getSize(); i++)
		ht.put(dlm.getElementAt(i), new Integer(i));


	    
	    int[] list_indices = new int[ items.length ];

	    int hits = 0;
	    for(int i=0; i < items.length; i++)
	    {
		try
		{
		    int index = ((Integer) ht.get( items[i] )).intValue();
		    list_indices[hits++] = index;
		}
		catch(Exception ex)
		{
		    // null pointer means item not found in list
		}
	    }
	    
	    
	    if(hits < items.length)
	    {
		int[] shortened = new int[hits];
		for(int i=0; i < hits; i++)
		    shortened[i] = list_indices[i];
		list_indices = shortened;
	    }
	    
	    list.setSelectedIndices( list_indices );

	    if(list_indices.length > 0)
		list.ensureIndexIsVisible( list_indices[0] );
	}
	catch(ClassCastException cce)
	{
	    // it wasn't a ListModel then....
	    
	}
    }

 

    //
    // =====================================================================
    // state
    // =====================================================================
    //

    private JFrame frame;

    private JCheckBox apply_filter_jchkb;
    
    private boolean synchronise_spot_selection = true;
    private boolean synchronise_meas_selection = true;

    private boolean remember_column_layout = true;
    private boolean remember_column_widths = true;
    private boolean group_spot_attrs_by_name = false;

    private boolean always_include_expr_values = true;

    private boolean expand_spot_attr_names = true;
    private boolean expand_name_attr_names = true;

    private JTable atts_table;
    private CustomTableModel table_model;

    private CustomActionListener meas_al = null;
    
    private Hashtable meas_list_index_to_data_ht;
    private Hashtable name_attr_list_index_to_data_ht;

    private JScrollPane list_jsp;
    private JPanel list_wrapper;
    private JList name_name_attr_list;
    private JList meas_list;
    private JList spot_attr_list;

    private int list_mode = 0;

    private maxdView mview;
    private ExprData edata;

    private JPanel panel;
    private JTextArea text_area;

    private int sel_listener_handler;

    private boolean selection_is_internal_update = false;
    private boolean selection_is_external_update = false;

    private ListSelectionListener selection_listener;
}
