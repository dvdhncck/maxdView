import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class DataPlotLayoutOptions extends JFrame implements ExprData.ExprDataObserver
{
 
    public DataPlotLayoutOptions(final maxdView m_viewer, final DataPlot d_plot)
    {
	
	super("Layout");

	mview = m_viewer;
	dplot = d_plot;
	edata = mview.getExprData();
	
	mview.decorateFrame(this);

	layout_options = new JTabbedPane();
	layout_options.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagConstraints c = null;
	Color title_colour = new JLabel().getForeground().brighter();	    
	JLabel label = null;

	JPanel outer_panel = new JPanel();
	GridBagLayout outer_gridbag = new GridBagLayout();
	outer_panel.setLayout(outer_gridbag);
	
	
	//	JTabbedPane tabbed = null;
	
	// ====== box size and gaps =====================================================================

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout gridbag = new GridBagLayout();
	    wrapper.setLayout(gridbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Spot Geometry ");
	    title.setTitleColor(title_colour);
	    wrapper.setBorder(title);
	    
	    label = new JLabel("Border Gap  ");
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    border_gap_slider = new JSlider(JSlider.HORIZONTAL, 1, 65, 1);
	    border_gap_slider.setPaintTicks(true);
	    border_gap_slider.setMajorTickSpacing(8);
	    border_gap_slider.setValue(dplot.getBorderGap());
	    border_gap_slider.addChangeListener(new ChangeListener()
						{
						    public void stateChanged(ChangeEvent e) 
						    {
							JSlider source = (JSlider)e.getSource();
							//if (!source.getValueIsAdjusting()) 
							{
							    dplot.setBorderGap(source.getValue());
							}
						    }
						});
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    gridbag.setConstraints(border_gap_slider, c);
	    wrapper.add(border_gap_slider);

	    label = new JLabel(" Column Width  ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    box_width_slider = new JSlider(JSlider.HORIZONTAL, 1, 129, 1);
	    box_width_slider.setPaintTicks(true);
	    box_width_slider.setMajorTickSpacing(16);
	    box_width_slider.setValue(dplot.getBoxWidth());
	    box_width_slider.addChangeListener(new DisplaySliderListener());
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    gridbag.setConstraints(box_width_slider, c);
	    wrapper.add(box_width_slider);
	    
	    label = new JLabel("Row Height  ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    box_height_slider = new JSlider(JSlider.HORIZONTAL, 1, 65, 1);
	    box_height_slider.setPaintTicks(true);
	    box_height_slider.setMajorTickSpacing(8);
	    box_height_slider.setValue(dplot.getBoxHeight());
	    box_height_slider.addChangeListener(new DisplaySliderListener());
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 1;
	    c.gridy = 2;
	    c.weightx = 1.0;
	    gridbag.setConstraints(box_height_slider, c);
	    wrapper.add(box_height_slider);

	    label = new JLabel("Column Gap  ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    col_gap_slider = new JSlider(JSlider.HORIZONTAL, 0, 32, 1);
	    col_gap_slider.setPaintTicks(true);
	    col_gap_slider.setMajorTickSpacing(8);
	    col_gap_slider.setValue(dplot.getColGap());
	    col_gap_slider.addChangeListener(new DisplaySliderListener());
	    col_gap_slider.addChangeListener(new DisplaySliderListener());
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 1;
	    c.gridy = 3;
	    c.weightx = 1.0;
	    gridbag.setConstraints(col_gap_slider, c);
	    wrapper.add(col_gap_slider);

	    label = new JLabel("Row Gap  ");
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = 4;
	    gridbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    row_gap_slider = new JSlider(JSlider.HORIZONTAL, 0, 32, 1);
	    row_gap_slider.setPaintTicks(true);
	    row_gap_slider.setMajorTickSpacing(8);
	    row_gap_slider.setValue(dplot.getRowGap());
	    row_gap_slider.addChangeListener(new DisplaySliderListener());
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 1;
	    c.gridy = 4;
	    c.weightx = 1.0;
	    gridbag.setConstraints(row_gap_slider, c);
	    wrapper.add(row_gap_slider);

	    /*
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    outer_gridbag.setConstraints(wrapper, c);
	    */

	    layout_options.add(" Geometry ", wrapper);
	}

	// ===== clusters ===========================================================================
	
	{
	    JPanel o_wrapper = new JPanel();
	    GridBagLayout o_gbag = new GridBagLayout();
	    o_wrapper.setLayout(o_gbag);

	    show_glyphs_jcb           = new JCheckBox[2];
	    show_branches_jcb           = new JCheckBox[2];
	    overlay_root_children_jcb = new JCheckBox[2];
	    align_glyphs_jcb          = new JCheckBox[2];
	    branch_scale_slider       = new JSlider[2];

	    for(int cli=0; cli < 2; cli++)
	    {
		final int cl = cli;

		JPanel wrapper = new JPanel();
		GridBagLayout gbag = new GridBagLayout();
		wrapper.setLayout(gbag);

	    
		TitledBorder title = BorderFactory.createTitledBorder( ((cl==0) ? " Spot" : " Measurement" ) + " Clusters ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);
		
		show_branches_jcb[cl] = new JCheckBox("Show tree");
		show_branches_jcb[cl].setSelected(dplot.getShowBranches(cl));
		show_branches_jcb[cl].addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    dplot.setShowBranches(cl, show_branches_jcb[cl].isSelected());
			}
		    });
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		c.gridy = 0;
		gbag.setConstraints(show_branches_jcb[cl], c);
		
		show_glyphs_jcb[cl] = new JCheckBox("Show glyphs  ");
		show_glyphs_jcb[cl].setSelected(dplot.getShowGlyphs(cl));
		show_glyphs_jcb[cl].addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    dplot.setShowGlyphs(cl, show_glyphs_jcb[cl].isSelected());
			}
		    });
	    
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 2;
		c.gridy = 0;
		gbag.setConstraints(show_glyphs_jcb[cl], c);
		
		overlay_root_children_jcb[cl] = new JCheckBox("Overlay Root children");
		overlay_root_children_jcb[cl].setSelected(dplot.getOverlayRootChildren(cl));
		overlay_root_children_jcb[cl].addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    dplot.setOverlayRootChildren(cl, overlay_root_children_jcb[cl].isSelected());
			}
		    });
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		c.gridy = 1;
		gbag.setConstraints(overlay_root_children_jcb[cl], c);
		
		align_glyphs_jcb[cl] = new JCheckBox("Align Glyphs");
		align_glyphs_jcb[cl].setSelected(dplot.getAlignGlyphs(cl));
		align_glyphs_jcb[cl].addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    dplot.setAlignGlyphs(cl, align_glyphs_jcb[cl].isSelected());
			}
		    });
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 2;
		c.gridy = 1;
		gbag.setConstraints(align_glyphs_jcb[cl], c);
		
		label = new JLabel(" Depth ");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = 2;
		//c.weightx = 1.0;
		//c.weighty = 1.0;
		gbag.setConstraints(label, c);
		
		branch_scale_slider[cl] = new JSlider(JSlider.HORIZONTAL, 1, 32, 8);
		branch_scale_slider[cl].setPaintTicks(true);
		branch_scale_slider[cl].setMajorTickSpacing(2);
		branch_scale_slider[cl].setValue(dplot.getBranchScale(cl));
		
		branch_scale_slider[cl].addChangeListener(new ChangeListener()
		    {
			public void stateChanged(ChangeEvent e) 
			{
			    dplot.setBranchScale(cl, branch_scale_slider[cl].getValue()); 
			}
		    });
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		gbag.setConstraints(branch_scale_slider[cl], c);
		
		wrapper.add(show_glyphs_jcb[cl]);
		wrapper.add(show_branches_jcb[cl]);
		wrapper.add(overlay_root_children_jcb[cl]);
		wrapper.add(label);
		wrapper.add(branch_scale_slider[cl]);
		wrapper.add(align_glyphs_jcb[cl]);


		//---------
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = cl;
		c.weightx = 10.0;
		c.weighty = 5.0;
		o_gbag.setConstraints(wrapper, c);
		
		o_wrapper.add(wrapper);
	    }

	    // --------------
	    /*
	      c = new GridBagConstraints();
	      c.gridx = 0;
	      c.gridy = 2;
	      c.weightx = 1.0;
	      c.fill = GridBagConstraints.BOTH;
	      outer_gridbag.setConstraints(wrapper, c);
	      layout_options.add(wrapper);
	    */
	    layout_options.add(" Clusters ", o_wrapper);
	}
	
	// ====== name columns ====================================================================
	
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout gbag = new GridBagLayout();
	    wrapper.setLayout(gbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Name Columns ");
	    title.setTitleColor(title_colour);
	    wrapper.setBorder(title);
	    
	    int wline = 0;
	    
	    name_col_panel = new JPanel();
	    JScrollPane jsp = new JScrollPane(name_col_panel);
	    addNameColControls(name_col_panel);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline++;
	    c.weightx = 1.0;
	    c.weighty = 7.0;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.BOTH;
	    gbag.setConstraints(jsp, c);
	    wrapper.add(jsp);
	    
	    label = new JLabel(" Column gap ");
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    //c.weighty = 1.0;
	    c.gridy = wline;
	    gbag.setConstraints(label, c);
	    wrapper.add(label);

	    JSlider name_col_gap_slider = new JSlider(JSlider.HORIZONTAL, 4, 32, 8);
	    name_col_gap_slider.setPaintTicks(true);
	    name_col_gap_slider.setMajorTickSpacing(2);
	    name_col_gap_slider.setValue(dplot.getNameColGap());

	    name_col_gap_slider.addChangeListener(new ChangeListener()
					       {
						   public void stateChanged(ChangeEvent e) 
						   {
						       JSlider source = (JSlider)e.getSource();
						       dplot.setNameColGap(source.getValue()); 
						   }
					       });
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = wline++;
	    //c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gbag.setConstraints(name_col_gap_slider, c);
	    wrapper.add(name_col_gap_slider);
	    
	    JButton add_jb = new JButton("Add another column");
	    add_jb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			dplot.addNameCol();
			addNameColControls(name_col_panel);
			name_col_panel.updateUI();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.gridwidth = 2;
	    //c.weighty = 1.0;
	    gbag.setConstraints(add_jb, c);
	    wrapper.add(add_jb);
	    
	    // --------------
	    
	    /*
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.weightx = 1.0;
	    c.weighty = 5.0;
	    c.fill = GridBagConstraints.BOTH;
	    outer_gridbag.setConstraints(wrapper, c);
	    layout_options.add(wrapper);
	    */

	    layout_options.add(" Name Columns ", wrapper);
	}

	// ====== font ==========================================================================

	{
	    
	    JPanel o_wrapper = new JPanel();
	    GridBagLayout o_gbag = new GridBagLayout();
	    o_wrapper.setLayout(o_gbag);

	    for(int t=0; t < 2; t++)
	    {
		final int tf = t;

		JPanel wrapper = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		wrapper.setLayout(gridbag);
		TitledBorder title = BorderFactory.createTitledBorder( tf == 0 ? " Spot Labels " : " Measurement Labels ");
		title.setTitleColor(title_colour);
		wrapper.setBorder(title);
		
		int row = 0;

		if(tf == 1)
		{
		    label = new JLabel(" Align  ");
		    c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.EAST;
		    c.gridx = 0;
		    c.gridy = row;
		    gridbag.setConstraints(label, c);
		    wrapper.add(label);
		    

		    String[] al_str = { "Left", "Center", "Right" };
		    JComboBox meas_labels_align_jcb = new JComboBox(al_str);
		    
		    meas_labels_align_jcb.setToolTipText("How the Measurement names are aligned");
		    meas_labels_align_jcb.setSelectedIndex( dplot.getMeasurementLabelAlign() );
		    meas_labels_align_jcb.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				JComboBox source = (JComboBox)e.getSource();
				dplot.setMeasurementLabelAlign(source.getSelectedIndex());
			    }
			});
		    
		    c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.WEST;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    c.gridx = 1;
		    c.gridy = row;
		    gridbag.setConstraints(meas_labels_align_jcb, c);
		    wrapper.add(meas_labels_align_jcb);
		   
		    row++;
		}

		label = new JLabel("Size  ");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = row;
		gridbag.setConstraints(label, c);
		wrapper.add(label);
		
		JSlider font_size_slider = new JSlider(JSlider.HORIZONTAL, 4, 32, 8);
		font_size_slider.setPaintTicks(true);
		font_size_slider.setMajorTickSpacing(2);
		font_size_slider.setValue( tf == 0 ? dplot.getSpotFontSize() : dplot.getMeasurementFontSize() );
		
		font_size_slider.addChangeListener(new ChangeListener()
		    {
			public void stateChanged(ChangeEvent e) 
			{
			    JSlider source = (JSlider)e.getSource();
				//if(!source.getValueIsAdjusting())
			    {
				//System.out.println("slider done\n");
				if(tf == 0)
				    dplot.setSpotFontSize(source.getValue()); 
				else
				    dplot.setMeasurementFontSize(source.getValue()); 
			    }
			}
		    });
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = row;
		c.gridwidth = 2;
		c.weightx = 1.0;
		gridbag.setConstraints(font_size_slider, c);
		wrapper.add(font_size_slider);
		
		row++;

		label = new JLabel(" Style  ");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = row;
		gridbag.setConstraints(label, c);
		wrapper.add(label);
		
		JComboBox font_family_jcb = new JComboBox(dplot.font_family_names);
		font_family_jcb.setSelectedIndex( tf == 0 ? dplot.getSpotFontFamily() : dplot.getMeasurementFontFamily());
		font_family_jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JComboBox source = (JComboBox)e.getSource();
			    if(tf == 0)
				dplot.setSpotFontFamily(source.getSelectedIndex());
			    else
				dplot.setMeasurementFontFamily(source.getSelectedIndex());
			}
			});
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = row;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(font_family_jcb, c);
		wrapper.add(font_family_jcb);
		
		JComboBox font_style_jcb = new JComboBox(dplot.font_style_names);
		font_style_jcb.setSelectedIndex( tf == 0 ? dplot.getSpotFontStyle() : dplot.getMeasurementFontStyle() );
		font_style_jcb.addActionListener(new ActionListener()
		    {
			    public void actionPerformed(ActionEvent e) 
			{
			    JComboBox source = (JComboBox)e.getSource();
			    if(tf == 0)
				dplot.setSpotFontStyle(source.getSelectedIndex());
			    else
				dplot.setMeasurementFontStyle(source.getSelectedIndex());
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = row++;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(font_style_jcb, c);
		wrapper.add(font_style_jcb);


		JCheckBox antialias_jchkb = new JCheckBox("Use antialiasing to smooth the font");
		antialias_jchkb.setSelected( tf == 0 ? dplot.getSpotFontAntialiasing() : dplot.getMeasurementFontAntialiasing() );
		antialias_jchkb.addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    if(tf == 0)
				dplot.setSpotFontAntialiasing( source.isSelected() );
			    else
				dplot.setMeasurementFontAntialiasing( source.isSelected() );
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = row;
		c.gridwidth = 2;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(antialias_jchkb, c);
		wrapper.add(antialias_jchkb);


		// ===================


		c = new GridBagConstraints();
		c.gridy = t;
		c.weightx = 10.0;
		c.weighty = 4.0;
		c.fill = GridBagConstraints.BOTH;
		o_gbag.setConstraints(wrapper, c);
		o_wrapper.add(wrapper,c);

	    }

	    // ----------
	    
	    layout_options.add(" Fonts ", o_wrapper);
	}
	

	// ====== scrollmode ====================================================================

	/*
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout gbag = new GridBagLayout();
	    wrapper.setLayout(gbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Scroll mode ");
	    title.setTitleColor(title_colour);
	    wrapper.setBorder(title);

	    // --------------
	    
	    final JCheckBox keep_names_jcb = new JCheckBox("Lock names");
	    final JCheckBox keep_clusters_jcb = new JCheckBox("Lock clusters");

	    keep_names_jcb.setToolTipText("Keep the names on screen when scrolling");
	    keep_names_jcb.setSelected((dplot.getScrollMode() & 1) > 0);
	    keep_names_jcb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			int sm = keep_names_jcb.isSelected() ? 1 : 0;
			if(keep_clusters_jcb.isSelected())
			    sm += 2;
			dplot.setScrollMode( sm );
					     
		    }
		});

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    gbag.setConstraints(keep_names_jcb, c);
	    wrapper.add(keep_names_jcb);
	
	    keep_clusters_jcb.setToolTipText("Keep the clusters on screen when scrolling");
	    keep_clusters_jcb.setSelected((dplot.getScrollMode() & 2) > 0);
	    keep_clusters_jcb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			int sm = keep_names_jcb.isSelected() ? 1 : 0;
			if(keep_clusters_jcb.isSelected())
			    sm += 2;
			dplot.setScrollMode( sm );
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.anchor = GridBagConstraints.WEST;
	    gbag.setConstraints(keep_clusters_jcb, c);
	    wrapper.add(keep_clusters_jcb);
	   
	    // --------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 4;
	    c.weightx = 1.0;
	    c.weighty = 5.0;
	    c.fill = GridBagConstraints.BOTH;
	    outer_gridbag.setConstraints(wrapper, c);
	    layout_options.add(wrapper);
	}
	*/

	/*
	{
	    JLabel label = new JLabel("Measurement Labels  ");
	    layout_options.add(label);
		
	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = 7;
	    gridbag.setConstraints(label, c);
	}
	{
	    JPanel wrapper = new JPanel();
	    
	    {
		String[] str = {"None", "Top", "Bottom" };
		
		meas_labels_pos_jcb = new JComboBox(str);
		
		meas_labels_pos_jcb.setToolTipText("Where to place the gene labels");
		meas_labels_pos_jcb.setSelectedIndex(dplot.getSetLabelPos());
		meas_labels_pos_jcb.addActionListener(new ActionListener()
						      { 
							  public void actionPerformed(ActionEvent e) 
							  {
							      dplot.setMeasurementLabelPos(meas_labels_pos_jcb.getSelectedIndex());
							      updateSetLabelControls();
							  }
						      });
		wrapper.add(meas_labels_pos_jcb);
	    }
	
	    {
		JLabel label = new JLabel("  Align  ");
		wrapper.add(label);
	    }
	    {
		String[] str = { "Left", "Right", "Center" };
		meas_labels_align_jcb = new JComboBox(str);
		
		meas_labels_align_jcb.setToolTipText("Where to place the set labels");
		
		meas_labels_align_jcb.addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			   dplot.setRowLabelAlign(meas_labels_align_jcb.getSelectedIndex());
			   //updateSetLabelControls(); 
			}
		    });
		wrapper.add(meas_labels_align_jcb);
		
	    }

	    layout_options.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 7;
	    c.weighty = c.weightx = 1.0;
	    c.gridwidth = 3;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	}
	*/

	// ====== close/help buttons ===============================================================

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 1.0;
	c.weighty = 8.0;
	c.fill = GridBagConstraints.BOTH;
	outer_gridbag.setConstraints(layout_options, c);
	outer_panel.add(layout_options);


	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
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
		
		c = new GridBagConstraints();
		c.weighty = c.weightx = 1.0;
		inner_gridbag.setConstraints(jb, c);
	    }
	    {   
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    switch( layout_options.getSelectedIndex() )
			    {
			    case 0:
				mview.getHelpTopic("ViewerLayout", "#spot");
				break;
			    case 1:
				mview.getHelpTopic("ViewerLayout", "#text");
				break;
			    case 2:
				mview.getHelpTopic("ViewerLayout", "#clust");
				break;
			    case 3:
				mview.getHelpTopic("ViewerLayout", "#name");
				break;
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.weighty = c.weightx = 1.0;
		inner_gridbag.setConstraints(jb, c);
	    }

	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    outer_gridbag.setConstraints(buttons_panel, c);
	    outer_panel.add(buttons_panel);
	    
	}
	
	int tab = mview.getIntProperty("DataPlotLayoutOptions.tab_panel",0);
	int w =  mview.getIntProperty("DataPlotLayoutOptions.panel_w",350);
	int h =  mview.getIntProperty("DataPlotLayoutOptions.panel_h",380);
	
	layout_options.setSelectedIndex(tab);
	layout_options.setPreferredSize(new Dimension(w, h));

	getContentPane().add(outer_panel, BorderLayout.CENTER);
	
	edata.addObserver(this);

	addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	pack();
	setVisible(true);
    }

    private void cleanUp()
    {
	int tab = layout_options.getSelectedIndex();
	mview.putIntProperty("DataPlotLayoutOptions.tab_panel", tab);

	mview.putIntProperty("DataPlotLayoutOptions.panel_w",layout_options.getWidth());
	mview.putIntProperty("DataPlotLayoutOptions.panel_h",layout_options.getHeight());

	edata.removeObserver(this);
	setVisible(false);
    }

    private void addNameColControls(JPanel panel)
    {
	panel.removeAll();
	GridBagLayout gbag = new GridBagLayout();
	panel.setLayout(gbag);
	
	JLabel label = new JLabel(" Source ");
	GridBagConstraints c = new GridBagConstraints();
	c.anchor = GridBagConstraints.SOUTH;
	c.gridx = 1;
	c.gridy = 0;
	//c.weightx = 1.0;
	//c.weighty = 1.0;
	gbag.setConstraints(label, c);
	panel.add(label);

	label = new JLabel(" Align ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.SOUTH;
	c.gridx = 2;
	c.gridy = 0;
	//c.weightx = 1.0;
	//c.weighty = 1.0;
	gbag.setConstraints(label, c);
	panel.add(label);
	
	label = new JLabel(" Trim ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.SOUTH;
	c.gridx = 3;
	c.gridwidth = 2;
	c.gridy = 0;
	//c.weightx = 1.0;
	//c.weighty = 1.0;
	gbag.setConstraints(label, c);
	panel.add(label);
	
	// -----------------
	
	final int nnc = dplot.getNumNameCols();
	nt_sel               = new NameTagSelector[ nnc ];
	row_labels_align_jcb = new JComboBox[ nnc ];
	trim_len_jtf         = new JTextField[ nnc ];
	trim_jcb             = new JCheckBox[ nnc ];
	
	ImageIcon del_icon = new ImageIcon(mview.getImageDirectory() + "delete.gif");

	for(int nc=0; nc < nnc; nc++)
	{
	    if(nnc > 1)
	    {
		JButton del_jb = new JButton(del_icon);
		del_jb.setPreferredSize(new Dimension(20,20));
		del_jb.addActionListener(new NameColActionListener(nc, 5));
		del_jb.setToolTipText("Remove this name column");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = nc+1;
		c.fill = GridBagConstraints.VERTICAL;
		gbag.setConstraints(del_jb, c);
		panel.add(del_jb);
	    }

	    // -----------------
	    
	    nt_sel[nc] = new NameTagSelector(mview);
	    nt_sel[nc].setToolTipText("Choose what this name column displays");
	    nt_sel[nc].setNameTagSelection(dplot.getNameColSelection(nc));
	    nt_sel[nc].addActionListener(new NameColActionListener(nc, 0));
	    
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 1;
	    c.gridy = nc+1;
	    c.weightx = 2.0;
	    //c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.fill = GridBagConstraints.BOTH;
	    gbag.setConstraints(nt_sel[nc], c);
	    panel.add(nt_sel[nc]);
	    
	    // -----------------
	    
	    String[] al_str = { "Left", "Center", "Right" };
	    row_labels_align_jcb[nc] = new JComboBox(al_str);
	    
	    row_labels_align_jcb[nc].setToolTipText("How this name column is aligned");
	    row_labels_align_jcb[nc].setSelectedIndex(dplot.getNameColAlign(nc));
	    row_labels_align_jcb[nc].addActionListener(new NameColActionListener(nc, 1));
	    
	    c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    c.gridx = 2;
	    c.gridy = nc+1;
	    c.weightx = 1.0;
	    //c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.fill = GridBagConstraints.BOTH;
	    gbag.setConstraints(row_labels_align_jcb[nc], c);
	    panel.add(row_labels_align_jcb[nc]);
	    
	    // -----------------
	    
	    trim_len_jtf[nc] = new JTextField(3);
	    trim_len_jtf[nc].setToolTipText("Maximum length (in chars) to display in this name column");
	    trim_len_jtf[nc].setText(String.valueOf(dplot.getNameColTrimLength(nc)));
	    trim_len_jtf[nc].addActionListener(new NameColActionListener(nc, 3));

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 3;
	    c.gridy = nc+1;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.weighty = 1.0;
	    gbag.setConstraints(trim_len_jtf[nc], c);
	    panel.add(trim_len_jtf[nc]);

	    // -----------------
	    
	    trim_jcb[nc] = new JCheckBox();
	    trim_jcb[nc].setToolTipText("Whether to trim the width of this name column");
	    trim_jcb[nc].setSelected(dplot.getNameColTrimEnabled(nc));
	    trim_jcb[nc].addActionListener(new NameColActionListener(nc, 2));

	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.anchor = GridBagConstraints.WEST;
	    c.gridy = nc+1;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gbag.setConstraints(trim_jcb[nc], c);
	    panel.add(trim_jcb[nc]);
	    
	    // -----------------
	    
	    
	}
	panel.updateUI();
	
    }

    private class NameColActionListener implements ActionListener
    {
	private int nc, part;

	public NameColActionListener(int nc_, int part_)
	{
	    nc = nc_; part = part_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    for(int nc =0 ; nc < trim_len_jtf.length; nc ++)
		trim_len_jtf[nc].setEnabled(trim_jcb[nc].isSelected());
		
	    switch(part)
	    {
	    case 0:  // selection
		dplot.setNameColSelection(nc, nt_sel[nc].getNameTagSelection()); 
		break;
	    case 1:  // align
		dplot.setNameColAlign(nc, row_labels_align_jcb[nc].getSelectedIndex());
		break;
	    case 2:  // trim-enabled
		dplot.setNameColTrimEnabled(nc, trim_jcb[nc].isSelected());
		break;
	    case 3:  // trim-length
		try
		{
		    dplot.setNameColTrimLength(nc, new Integer(trim_len_jtf[nc].getText()).intValue());
		}
		catch(NumberFormatException nfe)
		{
		}
		break;
	    case 5: // delete
		dplot.removeNameCol(nc);
		addNameColControls(name_col_panel);
		name_col_panel.updateUI();
		break;
	    }
	}
    }

    // handles row_labels_align and row_labels_pos
    private void updateNameColControls()
    {
	//row_labels_align_jcb.setEnabled(side_labels_src_jcb.getSelectedIndex() != 0);
	//row_labels_pos_jcb.setEnabled(side_labels_src_jcb.getSelectedIndex() != 0);
    }

   // handles meas_labels_align and meas_labels_pos
    private void updateMeasurementLabelControls()
    {
	//meas_labels_align_jcb.setEnabled(meas_labels_pos_jcb.getSelectedIndex() != 0);
    }

    // handles all the box geometry sliders
    //
    class DisplaySliderListener implements ChangeListener 
    {
	public void stateChanged(ChangeEvent e) 
	{
	    JSlider source = (JSlider)e.getSource();
	    //if (!source.getValueIsAdjusting()) 
	    {
		//System.out.println("slider done, value is " + source.getValue());
		
		dplot.setBoxGeometry(box_width_slider.getValue(), box_height_slider.getValue(), 
				     col_gap_slider.getValue(), row_gap_slider.getValue());
            }
	}
    }

    // ---------------- --------------- --------------- ------------- ------------

    //
    // observer implementation
    //

    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.NameAttrsChanged:
	    System.out.println("attrs changed");
	    addNameColControls(name_col_panel);
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
    }


    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }
 
    // ---------------- --------------- --------------- ------------- ------------

    // 
    private JSlider box_width_slider, box_height_slider, col_gap_slider, row_gap_slider, border_gap_slider;
    private JComboBox meas_labels_align_jcb, meas_labels_pos_jcb;

    private JCheckBox[] show_branches_jcb;
    private JCheckBox[] show_glyphs_jcb;
    private JCheckBox[] overlay_root_children_jcb;
    private JCheckBox[] align_glyphs_jcb;
    private JSlider[]   branch_scale_slider;

    private NameTagSelector[] nt_sel;
    private JComboBox[] row_labels_align_jcb;
    private JTextField[] trim_len_jtf;
    private JCheckBox[] trim_jcb;

    private JPanel name_col_panel;

    private JTabbedPane layout_options;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

}
