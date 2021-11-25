import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import javax.swing.border.*;

public class SaveToDatabase implements Plugin
{
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	dbcon = mview.getDatabaseConnection();

	if((dbcon.attemptConnection() == false) || (!dbcon.isConnected()))
	{
	    System.out.println("connection failed...");
	    return;
	}
	    
	// System.out.println("connection made ok...");

	makeGUI();

	frame.pack();
	frame.setVisible(true);
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Save To Database", 
					 "exporter", 
					 "Write Measurements to a 'maxdSQL' database (experimental)", "", 
					 0, 1, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[0];
	
	//com[0] = new PluginCommand("save", null);
	
	return com;
    }
 
    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(done != null)
	    done.signal();
     } 

    public String pluginType() { return "exporter"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   SaveToDatabase
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public SaveToDatabase(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {	
	//if(dbcon.isConnected())

	dbcon.disconnect();
	
	frame.setVisible(false);
	frame = null;
    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   makeGUI()
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void makeGUI()
    {
	frame = new JFrame("Save To Database");
	
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

	Color title_colour = new JLabel().getForeground().brighter();	    

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
	    
	    meas_list = new JList();
	    JScrollPane jsp = new JScrollPane(meas_list);
	    meas_list.setModel(new MeasListModel());
	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    line++;

	    /*
	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    gridbag.setConstraints(apply_filter_jchkb, c);
	    panel.add(apply_filter_jchkb);
	    */

	    tabbed.add(" Pick Measurement ", panel);
	    
	}
	    
	// ===== database links ===============================================

	{
	    int oline = 0;
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    panel.setLayout(gridbag);

	    {
		int line = 0;
		JPanel wrapper = new JPanel();
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		TitledBorder title = BorderFactory.createTitledBorder(" Measurement ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);

		// - - - - - - - 

		JLabel label = new JLabel("Name ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		meas_name = new JTextField(24);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 4.0;
		//c.weighty = 1.0;
		c.gridwidth = 2;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(meas_name, c);
		wrapper.add(meas_name);

		JButton check_meas_jb = new JButton("Check");
		check_meas_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    checkMeasurementName();
			}
		    });
		check_meas_jb.setFont(mview.getSmallFont());
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(check_meas_jb, c);
		wrapper.add(check_meas_jb);
		
		meas_name_state = new JLabel(" ");
		meas_name_state.setFont(mview.getSmallFont());
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = line++;
		c.weightx = 3.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(meas_name_state, c);
		wrapper.add(meas_name_state);
		
		// - - - - - - - 

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }

	    // =========================================================================

	    {
		int line = 0;
		JPanel wrapper = new JPanel();
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		TitledBorder title = BorderFactory.createTitledBorder(" Image ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);

		// - - - - - - - 

		JLabel label = new JLabel("Name ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		image_name = new JTextField(24);
		image_name.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.gridwidth = 2;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(image_name, c);
		wrapper.add(image_name);
		
		label = new JLabel("ID ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		image_id = new JTextField(24);
		image_id.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.gridwidth = 2;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(image_id, c);
		wrapper.add(image_id);

		JButton pick_img_jb = new JButton("Pick");
		pick_img_jb.setFont(mview.getSmallFont());
		pick_img_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    pickImage();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(pick_img_jb, c);
		wrapper.add(pick_img_jb);
		
		image_state = new JLabel(" ");
		/*
		image_state.setFont(mview.getSmallFont());
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = line++;
		//c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(image_state, c);
		wrapper.add(image_state);
		*/

		// - - - - - - - 
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }

	    // =========================================================================

	    {
		int line = 0;
		JPanel wrapper = new JPanel();
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		TitledBorder title = BorderFactory.createTitledBorder(" Array Type ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);

		// - - - - - - - 

		JLabel label = new JLabel("Name ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		array_type_name = new JTextField(24);
		array_type_name.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(array_type_name, c);
		wrapper.add(array_type_name);
		
		label = new JLabel("ID ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		array_type_id = new JTextField(24);
		array_type_id.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(array_type_id, c);
		wrapper.add(array_type_id);

		label = new JLabel("Spot count ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		array_type_spot_count = new JTextField(24);
		array_type_spot_count.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(array_type_spot_count, c);
		wrapper.add(array_type_spot_count);

		label = new JLabel("Spots matched ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		array_type_spot_matches = new JTextField(24);
		array_type_spot_matches.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(array_type_spot_matches, c);
		wrapper.add(array_type_spot_matches);

		// - - - - - - - 
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }

	    // =========================================================================

	    {
		int line = 0;
		JPanel wrapper = new JPanel();
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		TitledBorder title = BorderFactory.createTitledBorder(" Image Analysis Protocol ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);

		// - - - - - - - 

		JLabel label = new JLabel("Name ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		image_analprot_name = new JTextField(24);
		image_analprot_name.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.gridwidth = 2;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(image_analprot_name, c);
		wrapper.add(image_analprot_name);
		
		label = new JLabel("ID ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(label, c);
		wrapper.add(label);
		
		image_analprot_id = new JTextField(24);
		image_analprot_id.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.gridwidth = 2;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(image_analprot_id, c);
		wrapper.add(image_analprot_id);

		JButton pick_iap_jb = new JButton("Pick");
		pick_iap_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    pickImageAnalProt();
			}
		    });
		pick_iap_jb.setFont(mview.getSmallFont());
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(pick_iap_jb, c);
		wrapper.add(pick_iap_jb);
		
		iap_state = new JLabel(" ");
		iap_state.setFont(mview.getSmallFont());
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = line++;
		//c.weightx = 1.0;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(iap_state, c);
		wrapper.add(iap_state);
		

		// - - - - - - - 
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }

	    tabbed.add(" Choose Links ", panel);
	}

	// ===== send options ===============================================

	{
	    int oline = 0;
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    panel.setLayout(gridbag);

	    {
		int line = 0;
		JPanel wrapper = new JPanel();
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		TitledBorder title = BorderFactory.createTitledBorder(" Spot Attributes ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);
	    

		spot_attr_wrapper = new JPanel();
		JScrollPane jsp = new JScrollPane(spot_attr_wrapper);
		jsp.setPreferredSize(new Dimension(300, 140));
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		wrapbag.setConstraints(jsp, c);
		wrapper.add(jsp);
		
		// - - - - - - - 
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }

	    {
		int line = 0;
		JPanel wrapper = new JPanel();
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		TitledBorder title = BorderFactory.createTitledBorder(" Image Attributes ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);
	    
		image_attr_wrapper = new JPanel();
		JScrollPane jsp = new JScrollPane(image_attr_wrapper);
		jsp.setPreferredSize(new Dimension(300, 140));
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		wrapbag.setConstraints(jsp, c);
		wrapper.add(jsp);
		
		// - - - - - - - 
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }


	    {
		
		Dimension fillsize = new Dimension(10,10);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = oline++;
		c.gridwidth = 2;
		gridbag.setConstraints(filler, c);
		panel.add(filler);
		
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
		GridBagLayout wrapbag = new GridBagLayout();
		wrapper.setLayout(wrapbag);
		
		main_button = new JButton("Send...");
		main_button.setEnabled(false);
		c = new GridBagConstraints();
		c.weightx = 1.0;
		//c.weighty = 1.0;
		//c.anchor = GridBagConstraints.SOUTH;
		//c.fill = GridBagConstraints.HORIZONTAL;
		main_button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    sendData();
			}
		    });
		wrapbag.setConstraints(main_button, c);
		wrapper.add(main_button);
	    
		// - - - - - - - 

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = oline++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }
	    
	    // ==============

	    tabbed.add(" Send data ", panel);
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
			    if(--mode <= 0)
			    {
				meas_list.setModel(new MeasListModel());

				mode = 0;
				
				bbutton.setEnabled(false);
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

			    if(mode == 0)
			    {
				final int[] si = meas_list.getSelectedIndices();
				
				final int nsi = (si == null) ? 0 : si.length;

				if(nsi != 1)
				{
				    mview.alertMessage("You must select one Measurement");
				    return;
				}

				String mname = (String) meas_list.getSelectedValue();
				selectMeasurement( edata.getMeasurementFromName( mname ) );
			    }

			    if(mode == 1)
			    {
				String emsg = safeToSendData();
				if(emsg != null)
				{
				    mview.errorMessage(emsg);
				    return;
				}
			    }
			    if(++mode >= 2)
			    {
				mode = 2;
				nbutton.setEnabled(false);

				setupAttributes(); 
			    }
			    
			    tabbed.setSelectedIndex(mode);


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
			    mview.getPluginHelpTopic("SaveToDatabase", "SaveToDatabase");
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

	// setupNormaliser();

	frame.getContentPane().add(o_panel);
	frame.pack();
	frame.setVisible(true);
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

    // okay to move the the third tab?
    private String safeToSendData()
    {
	String res = null;

	if(image_analprot_id.getText().length() == 0)
	    res = "Image Analysis Protocol not valid";

	if(cur_img_det.unmatched > 0)
	    res = "One or more spots are unmatched";

	if(!checkImage())
	    res = "Image (Name,ID) is not valid";
	
	if(!checkMeasurementName())
	    res = "Measurement Name is not valid";

	return res;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void selectMeasurement(int m_id)
    {
	// checkMeasurementName();

	cur_meas_id = m_id;

	ExprData.Measurement ms = edata.getMeasurement(m_id);

	String mname = ms.getName();
	meas_name.setText( mname );

	String iname = ms.getAttribute( "Image name" );
	if((iname != null) && (iname.length() == 0))
	    iname = null;
	
	String iid   = ms.getAttribute( "Image ID" );
	if((iid != null) && (iid.length() == 0))
	    iid = null;

	image_name.setText(iname);
	image_id.setText(iid);

	checkMeasurementName();
	
	if(iid != null)
	    selectImage( iid );

	String iap_id = image_analprot_id.getText();
	if(iap_id.length() > 0)
	    selectImageAnalProt(iap_id);
    }

    private boolean checkMeasurementName()
    {
	// is the measurement name already in use?
	{
	    String mname = meas_name.getText();
	    
	    if((mname != null) && (mname.length() > 0))
	    {
		if(mname.length() > dbcon.maxd_props.identifier_len)
		{
		    meas_name_state.setText("Name is TOO LONG");
		    return false;
		}
		else
		{
		    String sql = 
		    "SELECT " + 
		    dbcon.qTableDotField("Measurement","ID") + ", " +
		    dbcon.qTableDotField("Measurement","Name") +
		    " FROM " +  
		    dbcon.qTable("Measurement") +
		    " WHERE " + 
		    dbcon.qTableDotField("Measurement","Name") + " = " +  dbcon.qText( mname );
		    
		    int count = 0;
		    try
		    {
			ResultSet rs = dbcon.executeQuery(sql);
			while (rs.next()) 
			{
			    count++;
			}
		    }
		    catch(SQLException sqle)
		    {
			mview.alertMessage("checkMeasurementName(): Unable to execute SQL:\n" + sqle);
		    }
		    
		    if(count > 0)
		    {
			meas_name_state.setText("Name is already used in the database");
			return false;
		    }
		    else
		    {
			meas_name_state.setText("Name is not used in the database");
			return true;
		    }
		}
	    }
	    else
	    {
		meas_name_state.setText("No name specified");
		return false;
	    }
	}
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ImageDetails
    // --- --- ---  
    // --- --- ---   stores all relevant info about the Image 
    // --- --- ---   and all linked tables down to Spot
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    class ImageDetails
    {
	String image_id;

	String hyb_name;
	String hyb_id;

	String array_name;
	String array_type_name;
	String array_type_id;
	int    array_type_n_spots;

	int unmatched;

	Hashtable spot_name_to_id;
    }

    private ImageDetails cur_img_det = null;

    public ImageDetails getImageDetails(String image_id) 
    {
	ImageDetails img_det = new ImageDetails();
	
	String sql = null;

	try
	{
	    // get the names of the hybridisation, hybridisation protocol and experiment names
	    
	    sql = 
	    "SELECT " + 
	    dbcon.qTableDotField("Hybridisation","ID") + ", " + 
	    dbcon.qTableDotField("Hybridisation","Name") +  /*", " + 
	    dbcon.qTableDotField("HybridisationProtocol","Name") + ", " + 
	    dbcon.qTableDotField("Experiment","Name") + */
	    " FROM " +  
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Image") + /*", " +
	    dbcon.qTable("HybridisationProtocol") + ", " +
	    dbcon.qTable("Experiment") + */
	    " WHERE " + 
	    dbcon.qTableDotField("Image","ID") + " = " +  dbcon.qID(image_id) + 
	     " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") /*+ 
	    " AND " + 
	    dbcon.qTableDotField("Hybridisation","Hybridisation_Protocol_ID") + " = " + dbcon.qTableDotField("HybridisationProtocol","ID") + 
	    " AND " +
	    dbcon.qTableDotField("Hybridisation","Experiment_ID") + " = " + dbcon.qTableDotField("Experiment","ID") */;
	    
	    ResultSet rs = dbcon.executeQuery(sql);
	    if(rs != null)
	    {
		if(rs.next())
		{
		    img_det.hyb_id = rs.getString(1);
		    
		    img_det.hyb_name = rs.getString(2);
		}
	    }

	    // ---------------------------

	    // now get the names of the array and array type

	    sql = 
	    "SELECT " + 
	    dbcon.qTableDotField("Array","Name") + ", " + 
	    dbcon.qTableDotField("ArrayType","Name") + ", " + 
	    dbcon.qTableDotField("ArrayType","ID") + ", " + 
	    dbcon.qTableDotField("ArrayType","Number_Spots") + 
	    " FROM " +  
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Array") + ", " +
	    dbcon.qTable("ArrayType") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Hybridisation","ID") + " = " +  dbcon.qID(img_det.hyb_id) + 
	     " AND " +
	    dbcon.qTableDotField("Hybridisation","Array_ID") + " = " + dbcon.qTableDotField("Array","ID") + 
	    " AND " + 
	    dbcon.qTableDotField("Array","Array_Type_ID") + " = " + dbcon.qTableDotField("ArrayType","ID");
	    
	    rs = dbcon.executeQuery(sql);

	    if(rs != null)
	    {
		while(rs.next())
		{
		    img_det.array_name = rs.getString(1);
		    img_det.array_type_name = rs.getString(2);
		    img_det.array_type_id = rs.getString(3);
		    img_det.array_type_n_spots = rs.getInt(4);
		}
	    }
	}
	catch (SQLException sqle)
	{
	    mview.alertMessage("Unable to execute SQL: " + sqle);
	    return null;
	}
	
	return img_det;
    }

    private void selectImage( String image_id )
    {
	checkImage();
	
	if((cur_img_det == null) || (!image_id.equals(cur_img_det.image_id)))
	{
	    cur_img_det = getImageDetails( image_id );
	    
	    cur_img_det.image_id = image_id;
	}

	if(cur_img_det != null)
	{
	    array_type_name.setText( cur_img_det.array_type_name );
	    array_type_id.setText( cur_img_det.array_type_id );
	    array_type_spot_count.setText( String.valueOf(cur_img_det.array_type_n_spots) );

	    checkSpotMatches(cur_img_det);
	}
    }

    private boolean checkImage()
    {
	String iname = image_name.getText();
	String iid   = image_id.getText();

	boolean image_name_ok = false;
	boolean image_id_ok = false;

	// does the image name correspond with the given id?
	if((iid != null) && (iid.length() > 0))
	{
	    String sql = 
	    "SELECT " + 
	    dbcon.qTableDotField("Image","ID") + ", " +
	    dbcon.qTableDotField("Image","Name") +
	    " FROM " +  
	    dbcon.qTable("Image") +
	    " WHERE " + 
	    dbcon.qTableDotField("Image","ID") + " = " +  dbcon.qID( iid );
	    
	    try
	    {
		ResultSet rs = dbcon.executeQuery(sql);
		while (rs.next()) 
		{
		    if(iid.equals(rs.getString(1)))
		    {
			if((iname != null) && (iname.equals(rs.getString(2))))
			{
			    image_name_ok = true;
			    image_id_ok = true;
			}
		    }
		}
	    }
	    catch(SQLException sqle)
	    {
		mview.alertMessage("checkImage(): Unable to execute SQL:\n" + sqle);
	    }
	}
	else
	{
	    array_type_name.setText("");
	    array_type_id.setText("");
	    array_type_spot_count.setText("");
	    array_type_spot_matches.setText("");

	    image_state.setText("No Image specified");
	}

	if(image_name_ok && image_id_ok) 
	{
	    image_state.setText("Image Name and ID matched in database");
	    return true;
	}
	else
	{
	    image_state.setText("Specified Image not found in database");
	    return false;
	}
    }

    private void pickImage()
    {
	Vector data = new Vector();

	String sql = 
	"SELECT " + 
	dbcon.qTableDotField("Image","Name") + ", " +
	dbcon.qTableDotField("Image","ID") + ", " +
	dbcon.qTableDotField("Image","Digitised_Image_URL") + ", " +
	dbcon.qTableDotField("Hybridisation","Name") + ", " +
	dbcon.qTableDotField("ScanningProtocol","Name") +
	" FROM " +  
	dbcon.qTable("Image") + ", " +
	dbcon.qTable("Hybridisation") + ", " +
	dbcon.qTable("ScanningProtocol") +
	" WHERE " + 
	dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + 
	dbcon.qTableDotField("Hybridisation","ID") + 
	" AND " + 
	dbcon.qTableDotField("Image","Scanning_Protocol_ID") + " = " + 
	dbcon.qTableDotField("ScanningProtocol","ID");

	try
	{
	    ResultSet rs = dbcon.executeQuery(sql);

	    if(rs != null)
	    {
		while (rs.next()) 
		{
		    String[] row = new String[5];
		    row[0] = rs.getString(1);
		    row[1] = rs.getString(2);
		    row[2] = rs.getString(3);
		    row[3] = rs.getString(4);
		    row[4] = rs.getString(5);
		
		    data.addElement(row);
		}
	    }
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("pickImage(): Unable to execute SQL:\n" + sqle);
	}

	System.out.println(data.size() + " images found in db");

	if(data.size() > 0)
	{
	    String[][] tdata = (String[][]) data.toArray(new String[0][]);
	    String[] cdata = { "Image.Name", "Image.ID", "Image.Digitised_Image_URL", 
			       "Hybridisation.Name", "Scanning_Protocol.Name" };

	    int choice = new GenericPicker().pickFromTable("Pick an Image", cdata, tdata);

	    System.out.println("row " + choice + " picked");

	    if(choice >= 0)
	    {
		image_name.setText( tdata[choice][0] );
		image_id.setText( tdata[choice][1] );
		
		selectImage( tdata[choice][1] );
	    }

	}

	
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   IAPDetails
    // --- --- ---  
    // --- --- ---   stores info about the selected ImageAnalysisProtocol
    // --- --- ---   and how the SpotAttrs in the Measurement map to SpotAttrs in the ImageAnalysisProtocol
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private IAPDetails cur_iap_det = null;

    private class IAPDetails
    {
	public String id;
	public String name;
	public Vector spot_attrs;
	public Vector image_attrs;
	
	//	public int[] mv_sa_id_to_db_sa_id;
	public int[] db_sa_id_to_mv_sa_id;
    }

    private void selectImageAnalProt(String iap_id)
    {
	if(cur_iap_det == null)
	    cur_iap_det = new IAPDetails();
	
	cur_iap_det.id = iap_id;

	String sql = 
	"SELECT " + 
	dbcon.qTableDotField("Type","ID") + ", " +
	dbcon.qTableDotField("Type","Name") + ", " +
	dbcon.qTableDotField("Type","Unit") + ", " +
	dbcon.qTableDotField("Type","Data_Type") +
	" FROM " +  
	dbcon.qTable("ImageAnalysisProtocolSpotAttr") + ", " +
	dbcon.qTable("Type") + 
	" WHERE " +
	dbcon.qTableDotField("ImageAnalysisProtocolSpotAttr","Type_ID") + " = " + 
	dbcon.qTableDotField("Type","ID") + 
	" AND " + 
	dbcon.qTableDotField("ImageAnalysisProtocolSpotAttr","Image_Analysis_Protocol_ID") + " = " + 
	dbcon.qID(iap_id);

	cur_iap_det.spot_attrs = new Vector();
	
	try
	{
	    ResultSet rs = dbcon.executeQuery(sql);
	    
	    if(rs != null)
	    {
		while (rs.next()) 
		{
		    String[] row = new String[4];
		    row[0] = rs.getString(1);
		    row[1] = rs.getString(2);
		    row[2] = rs.getString(3);
		    row[3] = rs.getString(4);

		    cur_iap_det.spot_attrs.addElement(row);
		}
	    }
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("setupAttributes(): Unable to execute SQL:\n" + sqle);
	}

    }

    private void pickImageAnalProt()
    {
	Vector data = new Vector();

	String sql = 
	"SELECT " + 
	dbcon.qTableDotField("ImageAnalysisProtocol","Name") + ", " +
	dbcon.qTableDotField("ImageAnalysisProtocol","ID") + ", " +
	dbcon.qTableDotField("ImageAnalysisProtocol","Software_Description_ID") +
	" FROM " +  
	dbcon.qTable("ImageAnalysisProtocol");

	try
	{
	    ResultSet rs = dbcon.executeQuery(sql);

	    if(rs != null)
	    {
		while (rs.next()) 
		{
		    String[] row = new String[5];
		    row[0] = rs.getString(1);
		    row[1] = rs.getString(2);
		    row[2] = rs.getString(3);
		
		    data.addElement(row);
		}
	    }
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("pickImageAnalProt(): Unable to execute SQL:\n" + sqle);
	}

	System.out.println(data.size() + " IAPs found in db");

	if(data.size() > 0)
	{
	    // get the number of Spot attrs for each of the IAPs
	    for(int iap=0; iap < data.size(); iap++)
	    {
		String[] row = (String[])data.elementAt(iap);
		String iap_id = row[1];
		
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Type","ID") +
		" FROM " +  
		dbcon.qTable("ImageAnalysisProtocolSpotAttr") + ", " +
		dbcon.qTable("Type") + 
		" WHERE " +
		dbcon.qTableDotField("ImageAnalysisProtocolSpotAttr","Type_ID") + " = " + 
		dbcon.qTableDotField("Type","ID") + 
		" AND " + 
		dbcon.qTableDotField("ImageAnalysisProtocolSpotAttr","Image_Analysis_Protocol_ID") + " = " + 
		dbcon.qID(iap_id);

		int count = 0;
		
		try
		{
		    ResultSet rs = dbcon.executeQuery(sql);
		    
		    if(rs != null)
		    {
			while (rs.next()) 
			{
			    count++;
			}
		    }
		}
		catch(SQLException sqle)
		{
		    mview.alertMessage("pickImageAnalProt(): Unable to execute SQL:\n" + sqle);
		}

		row[3] = String.valueOf(count);
	    }
	    
	    // get the number of Image attrs for each of the IAPs
	    for(int iap=0; iap < data.size(); iap++)
	    {
		String[] row = (String[])data.elementAt(iap);
		String iap_id = row[1];
		
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Type","ID") +
		" FROM " +  
		dbcon.qTable("ImageAnalysisProtocolImageAttr") + ", " +
		dbcon.qTable("Type") + 
		" WHERE " +
		dbcon.qTableDotField("ImageAnalysisProtocolImageAttr","Type_ID") + " = " + 
		dbcon.qTableDotField("Type","ID") + 
		" AND " + 
		dbcon.qTableDotField("ImageAnalysisProtocolImageAttr","Image_Analysis_Protocol_ID") + " = " + 
		dbcon.qID(iap_id);

		int count = 0;
		
		try
		{
		    ResultSet rs = dbcon.executeQuery(sql);
		    
		    if(rs != null)
		    {
			while (rs.next()) 
			{
			    count++;
			}
		    }
		}
		catch(SQLException sqle)
		{
		    mview.alertMessage("pickImageAnalProt(): Unable to execute SQL:\n" + sqle);
		}

		row[4] = String.valueOf(count);
	    }
	    

	    String[][] tdata = (String[][]) data.toArray(new String[0][]);
	    String[] cdata = { "Name", "ID", "Software_Description_ID", 
			       "Spot Attrs.", "Image Attrs." };
	    
	    int choice = new GenericPicker().pickFromTable("Pick an Image Analysis Protocol", cdata, tdata);
	    
	    System.out.println("row " + choice + " picked");

	    if(choice >= 0)
	    {
		image_analprot_name.setText( tdata[choice][0] );
		image_analprot_id.setText( tdata[choice][1] );

		selectImageAnalProt( tdata[choice][1] );

		if(cur_iap_det != null)
		    cur_iap_det.name = tdata[choice][0];
		
		// selectImage( tdata[choice][1] );
	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   checkSpotMatches()...
    // --- --- ---  
    // --- --- ---   ...compare the Spot names in the data with those in the database
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void checkSpotMatches(ImageDetails img_det)
    {
	if((img_det == null) 
	   || (img_det.array_type_id == null)
	   || (img_det.array_type_id.length() == 0))
	{
	    System.out.println("checkSpotMatches(): no ArrayType.ID specified");
	}
	else
	{
	    Vector data = new Vector();
	    
	    String sql = 
	    "SELECT " + 
	    dbcon.qTableDotField("Spot","Name") + ", " +
	    dbcon.qTableDotField("Spot","ID") +
	    " FROM " +  
	    dbcon.qTable("Spot") +
	    " WHERE " + 
	    dbcon.qTableDotField("Spot", "Array_Type_ID") + " = " +
	    dbcon.qID(cur_img_det.array_type_id);

	    img_det.spot_name_to_id = new Hashtable();

	    try
	    {
		ResultSet rs = dbcon.executeQuery(sql);
		
		if(rs != null)
		{
		    while (rs.next()) 
		    {
			img_det.spot_name_to_id.put( rs.getString(1), rs.getString(2) );
		    }
		}
	    }
	    catch(SQLException sqle)
	    {
		mview.alertMessage("checkSpotMatches(): Unable to execute SQL:\n" + sqle);
	    }

	    // update the "spot count" label with the actual count
	    final int n_spots_on_array = img_det.spot_name_to_id.size();
	    
	    if(n_spots_on_array != cur_img_det.array_type_n_spots)
	    {
		array_type_spot_count.setText( n_spots_on_array + " (claimed to be " + 
					       cur_img_det.array_type_n_spots + ")" );
	    }

	    System.out.println(n_spots_on_array + " spots found on array");

	    // match the spots_name from the DB against those from the data

	    final int ns = edata.getNumSpots();
	    int matched = 0;
	    
	    for(int s=0; s < ns; s++)
	    {
		String id = (String) img_det.spot_name_to_id.get( edata.getSpotName(s) );
		if(id == null)
		{
		    img_det.unmatched++;
		}
		else
		{
		    matched++;
		}
	    }

	    array_type_spot_matches.setText(matched + " of " + ns);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   attributes
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JComboBox[] db_attr_jcb = null;
    private JLabel[]    db_attr_jl = null;

    private void setupAttributes()
    {
	String iap_id = cur_iap_det.id;
	if(iap_id == null)
	{
	    mview.alertMessage("No ImageAnalysisProtocol selected");
	    return;
	}
	
	
	
	spot_attr_wrapper.removeAll();
	GridBagLayout wrapbag = new GridBagLayout();
	spot_attr_wrapper.setLayout(wrapbag);

	// generate a String[] of SpotAttr names for the selected measurement

	final ExprData.Measurement ms = edata.getMeasurement( cur_meas_id );

	Vector data_sa_v = new Vector();

	final int n_sa_in_meas =  ms.getNumSpotAttributes();
	final int n_sa_in_iap   = cur_iap_det.spot_attrs.size();

	cur_iap_det.db_sa_id_to_mv_sa_id = new int[ n_sa_in_iap ];

	for(int sa=0; sa < n_sa_in_iap; sa++)
	{
	    cur_iap_det.db_sa_id_to_mv_sa_id[sa] = -1;
	}
	
	for(int sa=0; sa < n_sa_in_meas; sa++)
	{
	    data_sa_v.addElement(ms.getSpotAttributeName(sa));
	}

	final String[] data_sa_a = (String[]) data_sa_v.toArray(new String[0]);
	
	db_attr_jcb = new JComboBox[n_sa_in_iap];
	db_attr_jl  = new JLabel[n_sa_in_iap];

	for(int a=0; a < n_sa_in_iap; a++)
	{
	    String[] row = (String[]) cur_iap_det.spot_attrs.elementAt(a);
	    
	    JLabel label = new JLabel( row[1] );  // name
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = a;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(label, c);
	    spot_attr_wrapper.add(label);

	    label = new JLabel( row[2] ); // unit
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = a;
	    c.weightx = 1.0;
	    wrapbag.setConstraints(label, c);
	    spot_attr_wrapper.add(label);
	    
	    label = new JLabel( row[3] ); // type
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = a;
	    c.weightx = 1.0;
	    wrapbag.setConstraints(label, c);
	    spot_attr_wrapper.add(label);

	    db_attr_jcb[a] = new JComboBox( data_sa_a );
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = a;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(db_attr_jcb[a], c);
	    spot_attr_wrapper.add(db_attr_jcb[a]);

	    db_attr_jl[a] = new JLabel( "  " );
	    db_attr_jl[a].setForeground(Color.red);
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = a;
	    c.weightx = 1.0;
	    wrapbag.setConstraints(db_attr_jl[a], c);
	    spot_attr_wrapper.add(db_attr_jl[a]);

	    // try to match the attr to one in the current measurement...

	    db_attr_jcb[a].setSelectedIndex(-1);
	    for(int msa=0; msa < n_sa_in_meas; msa++)
	    {
		String msan = edata.getMeasurement(cur_meas_id).getSpotAttributeName(msa);
		if(msan.equals( row[1] ))
		{
		    cur_iap_det.db_sa_id_to_mv_sa_id[a] = msa;

		    db_attr_jcb[a].setSelectedItem( msan );
		}
	    }
	    
	    db_attr_jcb[a].addActionListener(new CustomActionListener(a));

	}

	checkAttrsAreGood();
	spot_attr_wrapper.revalidate();
    }

    private boolean checkAttrsAreGood()
    {
	// check whether all db SpotAttrs are now assigned to
	
	final int db_nsa = cur_iap_det.spot_attrs.size();
	final int mv_nsa = edata.getMeasurement(cur_meas_id).getNumSpotAttributes();
	
	System.out.println("there are " + db_nsa + " spot attrs in this IAP");
	System.out.println("there are " + mv_nsa + " spot attrs in this Measurement");
	
	boolean type_safe = true;

	// make sure all the db_sa's are used
	boolean[] used = new boolean[ db_nsa ];
	int[]     count = new int[ mv_nsa ];

	for(int a=0; a < db_nsa; a++)
	{
	    int mv_db_id = db_attr_jcb[a].getSelectedIndex();
	    if(mv_db_id >= 0)
	    {
		used[a] = true;
		count[mv_db_id]++;
		cur_iap_det.db_sa_id_to_mv_sa_id[a] = mv_db_id;

		String mv_type = edata.getMeasurement(cur_meas_id).getSpotAttributeDataType(mv_db_id);
		String db_type = ((String[])cur_iap_det.spot_attrs.elementAt(a))[3];

		if(!mv_type.equals(db_type))
		{
		    System.out.println("type mismatch: " + mv_type + " != " + db_type);
		    db_attr_jl[a].setText(" T ");
		    type_safe = false;
		}
		else
		    db_attr_jl[a].setText("  ");

	    }
	    else
	    {
		used[a] = false;
		cur_iap_det.db_sa_id_to_mv_sa_id[a] = -1;
	    }
	}

	int used_once = 0;
	for(int s=0; s < mv_nsa; s++)
	    if(count[s] == 1)
	    {
		used_once++;
	    }
	
	int unused = 0;
	for(int s=0; s < db_nsa; s++)
	{
	    if(!used[s])
	    {
		db_attr_jl[s].setText("?");
		unused++;		
	    }
	    else
	    {
		int mv_db_id = db_attr_jcb[s].getSelectedIndex();
		if(count[mv_db_id] > 1)
		    db_attr_jl[s].setText("!!");
	    }
	}

	main_button.setEnabled( type_safe && (unused == 0) );

	return( type_safe && (unused == 0) );
    }

    private class CustomActionListener implements ActionListener
    {
	public CustomActionListener(int attr_)
	{
	    attr = attr_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    checkAttrsAreGood();
	}
	
	private int attr;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   sendData()
    // --- --- ---  
    // --- --- ---   does the actual work....
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    int tables_to_insert_to = 1;
    int fields_to_insert_to = 3;
    int ticker = 0;
    ProgressOMeter pm;

    String insert_header, spot_insert_expr_and_sig_header;
    String spot_insert_expr_only_header, spot_insert_empty_header;
    String locate_spot_header;

    public String sendData()  // returns the new Measurement.ID
    {
	pm = new ProgressOMeter("Sending...", 5);
	pm.startIt();

	new DataSendThread().start();
	return null;
    }

    class DataSendThread extends Thread
    {
	public void run()  // returns the new Measurement.ID
	{
	java.util.Date start_time = new java.util.Date();

	final int n_spots = edata.getNumSpots();

	boolean needs_desc = false;

	if(cur_iap_det.spot_attrs.size() > 0)
	{
	    needs_desc = true;
	    
	    tables_to_insert_to += (1 + cur_iap_det.spot_attrs.size());
	    fields_to_insert_to += (1 + 1 + (cur_iap_det.spot_attrs.size() * 3));
	}

	float insert_delta_per_field = (1.0f / fields_to_insert_to);
	float insert_frac = .0f;
	float total_inserts = (float) n_spots;

	String meas_id = dbcon.generateUniqueID("Measurement", "ID");	
		
	String sql = "INSERT INTO " + dbcon.qTable("Measurement") + " (" + 
	dbcon.qField("Name") + ", " + 
	dbcon.qField("ID") + ", " + 
	dbcon.qField("Image_ID") + ", ";
	

	//if(image_attr_desc_id == null)
	{
	    // no image (global) attributes specified
	    sql += 
	    dbcon.qField("Image_Analysis_Protocol_ID") + ") " +
	    "VALUES (" + dbcon.qText(meas_name.getText()) + ", " +
	    dbcon.qID(meas_id) + ", " +
	    dbcon.qID(cur_img_det.image_id) + ", " +
	    dbcon.qID(cur_iap_det.id) + " )\n";
	}
	/*
	else
	{
	    // record the Description_ID shared by all of the image attributes
	    sql += 
	    dbcon.qField("Image_Attribute_Description_ID") + ", " + 
	    dbcon.qField("Image_Analysis_Protocol_ID") + ") " +
	    "VALUES (" + dbcon.qText(mi.name) + ", " +
	    dbcon.qID(meas_id) + ", " +
	    dbcon.qID(mi.image_id) + ", " +
	    dbcon.qDescID(image_attr_desc_id) + ", " +
	    dbcon.qID(mi.image_analysis_protocol_id) + " )\n";
	}
	*/

	if(dbcon.executeStatement(sql) == false)
	{
	    mview.alertMessage("Unable to create instance\nNew measurement not created");
	    return;
	}
	
	preloadSpotIDs(cur_img_det.array_type_id);

	initInsertHeaders(needs_desc);
	//
	// build the complete set of Description_IDs used to link the SpotMeasurements
	// with the various [Numeric|Integer|Char|String]Property's
	//
	String[] spot_desc_id_table = null;

	if(needs_desc)
	{
	    if(pm != null)
		pm.setMessage("Generating SpotAttribute links");
	    
	    spot_desc_id_table = new String[n_spots];
	    
	    spot_desc_id_table[0] = dbcon.generateDescriptionID(null);
	    
	    
	    // we dont want to use the SELECT(MAX(ID)) query for all
	    // N thousand of the IDs because that would take forever
	    // instead, allocate a block of sequentially numbered IDs 
	    // 
	    // but we'd better hope that another client doesnt allocate 
	    // any IDs in the next while.....
	    
	    int first_id = new Integer(spot_desc_id_table[0]).intValue();
	    
	    float insert_delta  = insert_delta_per_field;
	    
	    for(int spot=1; spot < n_spots; spot++)
	    {
		spot_desc_id_table[spot] = String.valueOf(first_id + spot);
		
		StringBuffer sbuf =  new StringBuffer("INSERT INTO ");
		sbuf.append(dbcon.qTable("Description"));
		sbuf.append(" (" + dbcon.qField("ID") + ") VALUES (");
		sbuf.append(dbcon.qID(spot_desc_id_table[spot]) + ")");
		
		if(dbcon.executeStatement(sbuf.toString()) == false)
		{
		    mview.alertMessage("SpotAttrDescriptionID rows not created");
		    if(pm != null)
			pm.stopIt();
		    return;
		}
		
		insert_frac += insert_delta;
		
		//if((pm != null) && ((spot % spot_update_count) == 0))
		if(++ticker == 10)
		{
		    ticker = 0;
		    updateProgress(pm, start_time, insert_frac, insert_delta, total_inserts);
		}

	    }	
	} // /if(needs_desc)

	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
	
	try 
	{ 
	    if(pm != null)
		pm.setMessage("Inserting SpotMeasurements...");
	    
	    String spot_desc_id = null;
	    
	    float insert_delta  = (needs_desc ? 4.0f : 3.0f) * insert_delta_per_field;
	    
	    boolean has_sval = false;// (mi.sig_val != null);

	    for(int spot = 0; spot < n_spots; spot++)
	    {
		if(needs_desc)
		    spot_desc_id = String.valueOf(spot_desc_id_table[spot]);
		else
		    spot_desc_id = null;

		insertSpotMeasurement(cur_img_det.array_type_id, 
				      spot_desc_id, 
				      meas_id,
				      edata.getSpotName(spot),
				      edata.eValue(cur_meas_id, spot));
		
		insert_frac += insert_delta;
		
		updateProgress(pm, start_time, insert_frac, insert_delta, total_inserts);
	    }
	}
	catch (AbortLoadException ale)
	{
	    if(pm != null)
		pm.stopIt();
	    dbcon.cancelBatch();
	    mview.alertMessage("Load aborted");
	    return;
	}
	
	
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-

	// load each of the spot attributes in turn
	//
	
	try
	{
	    if(needs_desc)
	    {
		// write Property inserts for any Attribute's (i.e. Property's) which are associated
		// with this spot
		//
		float insert_delta  = 3.0f * insert_delta_per_field;
		
		for(int sa=0; sa< cur_iap_det.spot_attrs.size(); sa++)
		{
		    String iap_type_name =  ((String[]) cur_iap_det.spot_attrs.elementAt(sa))[1];
		    String iap_type_type =  ((String[]) cur_iap_det.spot_attrs.elementAt(sa))[3];
		    String iap_type_id = ((String[]) cur_iap_det.spot_attrs.elementAt(sa))[0];
		    int mv_sa_id = cur_iap_det.db_sa_id_to_mv_sa_id[sa];

		    //sbuf = new StringBuffer();
		    
		    if(pm != null)
			pm.setMessage("Inserting " + iap_type_name + "...");
		    
		    //if(debug)
		    System.out.println("inserting SpotAttribute " + iap_type_name + " (type=" +  iap_type_type + ")");
		    
		    String spot_attr_type_id = iap_type_id;

		    int type_code = IntegerPropType;
		    if(iap_type_type.equals("DOUBLE"))
			type_code = DoublePropType;
		    if(iap_type_type.equals("TEXT"))
			type_code = TextPropType;
		    if(iap_type_type.equals("CHAR"))
			type_code = CharPropType;

		    ExprData.Measurement ms = edata.getMeasurement( cur_meas_id );

		    for(int spot = 0; spot < n_spots; spot++)
		    {
			String spot_desc_id = spot_desc_id_table[spot];
			
			String value = ms.getSpotAttributeDataValueAsString(mv_sa_id, spot);

			insertProperty(spot_desc_id,
				       spot_attr_type_id, 
				       type_code, 
				       value);
			
			insert_frac += insert_delta;
			
			updateProgress(pm, start_time, insert_frac, insert_delta, total_inserts);
		    
		    }
		}
	    }
	}
	catch (AbortLoadException ale)
	{
	    if(pm != null)
		pm.stopIt();
	    dbcon.cancelBatch();
	    mview.alertMessage("Load aborted");
	    return;
	}
	
    
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
	
	main_button.setEnabled(false);
	pm.stopIt();
	dbcon.endBatch();  // includes a commit
	}
    }

    class AbortLoadException extends Exception { };

    final static int IntegerPropType = 0;
    final static int DoublePropType  = 1;
    final static int CharPropType    = 2;
    final static int TextPropType    = 3;

    private void insertProperty(String desc_id, String type_id, int type_type, String value) throws AbortLoadException
    {
	StringBuffer sbuf = new StringBuffer("INSERT INTO ");

	switch(type_type)
	{
	case IntegerPropType:
	    //System.out.println("integer property");
	    sbuf.append(dbcon.qTable("IntegerProperty"));
	    break;
	case DoublePropType:
	    //System.out.println("double property");
	    sbuf.append(dbcon.qTable("NumericProperty"));
	    break;
	case CharPropType:
	    //System.out.println("char property");
	    sbuf.append(dbcon.qTable("CharProperty"));
	    break;   
	case TextPropType:
	    //System.out.println("string property");
	    sbuf.append(dbcon.qTable("Property"));
	    break;   
	default:
	    mview.alertMessage("Property type " + type_type + " not supported yet");
	    return;
	}

	sbuf.append(" (" + dbcon.qField("Description_ID") + ", ");
	sbuf.append(dbcon.qField("Type_ID") );
	if((value != null) && (value.length() > 0))
	    sbuf.append(dbcon.qField(", " + "Value"));

	sbuf.append(")\nVALUES (");
	sbuf.append(dbcon.qDescID(desc_id) + ", ");
	sbuf.append(dbcon.qDescID(type_id));
    
	if((value != null) && (value.length() > 0))
	{
	    switch(type_type)
	    {
	    case IntegerPropType:
	    case DoublePropType:
		sbuf.append(", " + value);
		break;
	    case CharPropType:
	    case TextPropType:
		sbuf.append(", " + dbcon.qText(dbcon.tidyText(value)));
	    break;
	    }
	}

	sbuf.append(")\n");
	
	// System.out.println( sbuf.toString() );
	
	if(dbcon.executeStatement(sbuf.toString()) == false)
	{
	    mview.alertMessage("Property row not created");
	    throw new AbortLoadException();
	}
	
    }

    private void insertSpotMeasurement(String array_type_id, String spot_desc_id, 
				       String measure_id, String spot_name,
				       double eval) throws AbortLoadException
    {
	final int max_writes_per_buffer = 1;

	try
	{
	    String spot_id = locateSpotID(array_type_id, spot_name);

	    if(spot_id == null)
	    {
		// the spot file contains a line for a spot which is not on this array
		mview.alertMessage("Spot '" + spot_name + "' is not known.\nLoad aborted.");
		
		throw new AbortLoadException();
	    }
	    
	    StringBuffer sbuf = new StringBuffer();

	    if(Double.isNaN(eval))
	    {
		sbuf.append(spot_insert_empty_header);
	    }
	    else
	    {
		sbuf.append(spot_insert_expr_only_header);
	    }

	    sbuf.append("(" + dbcon.qID(spot_id) + ", ");
	    sbuf.append(dbcon.qID(measure_id));
	    
	    if(!Double.isNaN(eval))
		sbuf.append(", " + String.valueOf(eval));
	    
	    if(spot_desc_id != null)
	    {
		sbuf.append(", " + dbcon.qDescID(spot_desc_id));
	    }
	    sbuf.append(")\n");
	    
	    if(dbcon.executeStatement(sbuf.toString()) == false)
	    {
		mview.alertMessage("SpotMeasurement rows not created");
		throw new AbortLoadException();
	    }

	    // System.out.println( sbuf.toString() );
	}

	catch(ArrayIndexOutOfBoundsException aoobe)
	{
	    mview.alertMessage("Short line in input file.\nLoad aborted.");
	    throw new AbortLoadException();
	}
    }


    Hashtable spot_name_to_id_ht = null;

    public boolean preloadSpotIDs(String array_type_id)
    {
	spot_name_to_id_ht = new Hashtable();
	
	String sql = 
	" SELECT " + 
	dbcon.qField("Name") + ", " +
	dbcon.qField("ID") +
	" FROM " +
	dbcon.qTable("Spot") + 
	" WHERE " +
	dbcon.qField("Array_Type_ID") + " = " +
	dbcon.qID(array_type_id);
	
       ResultSet rs = dbcon.executeQuery(sql);

       //int cnt = 0;
       //System.out.println("ArrayType Spot names:");

       if(rs != null)
       {
	   try
	   {
	       while (rs.next()) 
	       {
		   spot_name_to_id_ht.put(rs.getString(1), rs.getString(2));
		   
		   //if(++cnt < 10)
		   //    System.out.println( "  " + rs.getString(1) );
	       }
	   }
	   catch(SQLException sqle)
	   {
	       mview.alertMessage("preloadSpotIDs(): SQL error in result set\n" + sqle);
	       return false;
	    }
       }
       else
       {
	   mview.alertMessage("No Spots linked to this ArrayType (?)");
	   return false;
       }
       
       // System.out.println(spot_name_to_id_ht.size() + " Spot_IDs preloaded");

       return true;
    }

    String locateSpotID(String array_type_id, String name)
    {
	if(spot_name_to_id_ht != null)
	{
	    return (String) spot_name_to_id_ht.get(name);
	}

	// remember, multiple ArrayTypes can share the same spot names,
	//  so we need the array_type_id to uniquely
	//  identify a given spot by name
	//
	String get_spot_ids_sql = 
	locate_spot_header + dbcon.qID(array_type_id) + 
	" AND " + dbcon.qField("Name") + " = " + dbcon.qText(dbcon.tidyText(name));
	
	String spot_id = null;

	ResultSet rs = dbcon.executeQuery(get_spot_ids_sql);
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    spot_id = rs.getString(1);
		}
	    }
	    catch(SQLException sqle)
	    {
		mview.alertMessage("locateSpotID(): SQL error in result set\n" + sqle);
	    }
	}

	return spot_id;
    }

    private void initInsertHeaders(boolean needs_desc)
    {
	// initalise and save any parts of the SQL INSERT statements which are constant
	// 
	StringBuffer sbuf = new StringBuffer();
	
	sbuf.append("INSERT INTO " + dbcon.qTable("SpotMeasurement") + " (" + 
		    dbcon.qField("Spot_ID") + ", " + dbcon.qField("Measurement_ID") + ", " + 
		    dbcon.qField("Expression_Level") + ", " +
		    dbcon.qField("Significance") + " ");
	if(needs_desc == true)
	    sbuf.append(", " + dbcon.qField("Output_Description_ID") + " ");
	sbuf.append(")\n VALUES ");
	
	spot_insert_expr_and_sig_header = sbuf.toString();

	// -----------

	sbuf = new StringBuffer();
	
	sbuf.append("INSERT INTO " + dbcon.qTable("SpotMeasurement") + " (" + 
		    dbcon.qField("Spot_ID") + ", " + dbcon.qField("Measurement_ID") + ", " + 
		    dbcon.qField("Expression_Level") + " ");
	if(needs_desc == true)
	    sbuf.append(", " + dbcon.qField("Output_Description_ID") + " ");
	sbuf.append(")\n VALUES ");

	spot_insert_expr_only_header = sbuf.toString();

	// -----------

	sbuf = new StringBuffer();

	sbuf.append("INSERT INTO " + dbcon.qTable("SpotMeasurement") + " (" + 
		    dbcon.qField("Spot_ID") + ", " + dbcon.qField("Measurement_ID") + " "); 
	if(needs_desc == true)
	    sbuf.append(", " + dbcon.qField("Output_Description_ID") + " ");
	sbuf.append(")\n VALUES ");

	spot_insert_empty_header = sbuf.toString();
	
	// -----------
	
	
	locate_spot_header = 
	"SELECT " + dbcon.qField("ID") + " FROM " + dbcon.qTable("Spot") + 
	" WHERE " + dbcon.qField("Array_Type_ID") + " = ";
		
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
	// -|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-
    }

    int prog_update_cnt  = 0;
    int prog_update_freq = 25;

    private void updateProgress(ProgressOMeter pm, 
				java.util.Date start_time, 
				float insert_frac, 
				float insert_delta,
				float total_inserts)
    {
	if(pm == null)
	    return;

	if(++prog_update_cnt < prog_update_freq)
	    return;

	prog_update_cnt = 0;

	long elapsed = mview.secondsSince(start_time);
	
	float sps = (elapsed > 0) ? (insert_frac / ((float)elapsed)) : 0;
	
	prog_update_freq = (int)(sps * insert_delta);

	pm.setMessage(1, (int)(Math.floor((insert_frac * 100.0f) / total_inserts)) + " % complete");
	
	pm.setMessage(2, "Elapsed time: " + mview.niceTime(elapsed));
	
	String sps_str = mview.niceDouble(sps, 7, 2);
	
	pm.setMessage(3, sps_str + " spots/second");
	
	long remain = (sps > 0) ? (long)(Math.ceil(((total_inserts - insert_frac) / sps))) : 0;
	
	pm.setMessage(4, "Remaining (?): " + mview.niceTime(remain));

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   generic picker
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class GenericPicker
    {
	private boolean picked;

	// returns -1 if cancelled, or >=0 for the selected row
	public int pickFromTable(String title, String[] colnames, String[][] data)
	{
	    // bung a JTable in a JDialog...

	    final JDialog dia = new JDialog(frame, true);
	    dia.setModal(true);
	    dia.setTitle(title);

	    GridBagConstraints c = null;
	    GridBagLayout gbag = new GridBagLayout();
	    JPanel panel = new JPanel();
	    panel.setLayout(gbag);
	    
	    final JTable table = new JTable(data, colnames); 
	    JScrollPane jsp = new JScrollPane(table);
	    jsp.setMinimumSize(new Dimension(350, 300));
	    
	    picked = false;
	    
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.NORTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 2;
	    gbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    JButton jb = new JButton("Cancel");
	    jb.addActionListener(new ActionListener() 
		{
		public void actionPerformed(ActionEvent e) 
		    {
			dia.setVisible(false);
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gbag.setConstraints(jb, c);
	    panel.add(jb);
	    
	    jb = new JButton("OK");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			int ind = table.getSelectedRow();
			if(ind >= 0)
			{
			    picked = true;
			    dia.setVisible(false);
			}
			else
			{
			    mview.alertMessage("You must pick one row or press 'Cancel'");
			}
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gbag.setConstraints(jb, c);
	    panel.add(jb);
	    
	    dia.getContentPane().add(panel);
	    dia.pack();
	    mview.locateWindowAtCenter(dia);
	    dia.setVisible(true);
	    
	    if(picked == true)
	    {
		return table.getSelectedRow();
	    }
	    else
	    {
		return -1;
	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   intestines
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private int cur_meas_id;

    private JTextField meas_name;
    private JLabel meas_name_state;

    private JTextField image_name;
    private JTextField image_id;
    private JLabel image_state;

    private JTextField image_analprot_name;
    private JTextField image_analprot_id;
    private JLabel iap_state;

    private JTextField array_type_name;
    private JTextField array_type_id;
    private JTextField array_type_spot_count;
    private JTextField array_type_spot_matches;

    private JPanel spot_attr_wrapper;
    private JPanel image_attr_wrapper;

    private JButton main_button;

    private JTextArea info_text_area;
    private int mode;
    private JList meas_list;

    private DatabaseConnection dbcon;

    private JFrame frame;
    private maxdView mview;
    private ExprData edata;
    private JPanel export_panel;
    private JComboBox meas_jcb;
}
