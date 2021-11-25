import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.HashSet;

import java.awt.font.*;
import java.awt.geom.*;
import java.awt.dnd.*;

import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

//
//  displays the profile of all spots or measurements within a cluster
//


//
// to fix in 1.2.0
// 
//   - spot selection is lost whenever apply_filter or label nam_eattr is changed
//
//   - better scaling ( eg best-fit autoscale for current selection)
//
//   - initial drawing doesn't always trigger in 'spot' mode
//
//
//
//   - allow picking based on measurement clusters as well as on spot clusters
//
//     
public class ProfileViewer implements ExprData.ExprDataObserver, Plugin, ExprData.ExternalSelectionListener
{
    public ProfileViewer(maxdView mview_)
    {
	mview = mview_;

	
	//System.out.println("++ a new ProfileViewer is alive, mview is "  + mview);
    }

    private void buildGUI()
    {
	sel_cls = new Vector();
	sel_cl_ids = new Vector();
	meas_ids = new int[0];

	frame = new JFrame ("Profile Viewer");

	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });


	
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);

	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	frame.getContentPane().add(panel);
	
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();

	
	GridBagConstraints c = null;

	/*
	{
	    JToolBar tool_bar = new JToolBar();
	    tool_bar.setFloatable(false);
	    addTopToolbarStuff(tool_bar);
	    getContentPane().add(tool_bar);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = c.weighty = 0.0;
	    //c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(toolbar, c);
	}
	*/

	// ========================================================================================

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,3,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    int col = 0;
	    JButton jb = null;

	    JButton button = new JButton("Back");
	    button.setToolTipText( "Go back to the previous view" );
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			goBack();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    addFiller( wrapper, w_gridbag, 0, col++, 16 );
	    addFiller( wrapper, w_gridbag, 0, col++, 16 );

/*
	    button = new JButton("->");
	    button.setEnabled(false);
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			goForward();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);
*/

	    JLabel label = new JLabel(" Labels ");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 2.0;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    nts = new NameTagSelector(mview);
	    nts.loadSelection("ProfileViewer.name_tags");
	    nt_sel = nts.getNameTagSelection();
	    nts.setToolTipText( "Select which Name or Name Attribute to use in labels" );
	    nts.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			nts.saveSelection("ProfileViewer.name_tags");
			nt_sel = nts.getNameTagSelection();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.weightx = 2.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(nts, c);
	    wrapper.add(nts);

	    jb = new JButton("Clear All");
	    jb.setToolTipText( "Remove all labels" );
	    
	    //jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			clearAllSpotLabels();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.fill = GridBagConstraints.VERTICAL;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    jb = new JButton("Label All");
	    jb.setToolTipText( "Add labels to all Spots" );
	    
	    //jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setAllSpotLabels();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.fill = GridBagConstraints.VERTICAL;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);


	    addFiller( wrapper, w_gridbag, 0, col++, 16 );


	    ImageIcon icon = new ImageIcon(mview.getImageDirectory() + "f-up.gif");
	    Insets ins = new Insets(0,0,0,0);
	    jb = new JButton(icon);
	    jb.setToolTipText( "Increase label font size" );
	    jb.setMargin(ins);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			spot_font_scale += 0.1;
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.weighty = 1.0;
	    //c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    icon = new ImageIcon(mview.getImageDirectory() + "f-down.gif");
	    jb = new JButton(icon);
	    jb.setMargin(ins);
	    jb.setToolTipText( "Reduce label font size" );
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(spot_font_scale > .0)
			{
			    spot_font_scale -= 0.1;
			    pro_panel.repaint( );
			}
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.weighty = 1.0;
	    c.weightx = 0.1;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    
	    addFiller( wrapper, w_gridbag, 0, col++, 16 );


	    final JCheckBox auto_space_jchkb = new JCheckBox("Auto space");
	    auto_space = mview.getBooleanProperty("ProfileViewer.auto_space", true);
	    auto_space_jchkb.setSelected(auto_space);
	    auto_space_jchkb.setToolTipText( "Attempt to layout labels neatly (can be time consuming)" );
	    
	    //auto_space_jchkb.setFont(mview.getSmallFont());
	    auto_space_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			auto_space = auto_space_jchkb.isSelected();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(auto_space_jchkb, c);
	    wrapper.add(auto_space_jchkb);


	    col_sel = true;

	    /*
	    final JCheckBox col_sel_jchkb = new JCheckBox("Colour");
	    col_sel = mview.getBooleanProperty("ProfileViewer.col_sel", true);
	    col_sel.setToolTipText( "Attempt to layout labels neatly (can be time consuming)" );
	    
	    col_sel_jchkb.setSelected(col_sel);
	    //col_sel_jchkb.setFont(mview.getSmallFont());
	    col_sel_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			col_sel = col_sel_jchkb.isSelected();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(col_sel_jchkb, c);
	    wrapper.add(col_sel_jchkb);
	    */

	    // 
	    // top button panel
	    //
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);


	}

	// ========================================================================================
	
	{
	    // // // // // // // // 

	    pickwrap = new JPanel();
	    pickbag = new GridBagLayout();
	    pickwrap.setLayout(pickbag);

	    ButtonGroup bg = new ButtonGroup();
	    

	    JRadioButton jchkb = new JRadioButton("Spots");
	    jchkb.setSelected(false);
	    bg.add(jchkb);
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    pickbag.setConstraints(jchkb, c);
	    pickwrap.add(jchkb);
	    jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			JRadioButton es = (JRadioButton) e.getSource();
			if(es.isSelected())
			{
			    setPickerPanel(false);
			}
		    }
		});


	    jchkb = new JRadioButton("Clusters");
	    jchkb.setSelected(true);
	    bg.add(jchkb);
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    pickbag.setConstraints(jchkb, c);
	    pickwrap.add(jchkb);
	    jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			JRadioButton es = (JRadioButton) e.getSource();
			if(es.isSelected())
			{
			    setPickerPanel(true);
			}
		    }
		});

	    // // // // // // // // 

	    spot_picker_panel = new JPanel();
	    GridBagLayout spotpickbag = new GridBagLayout();
	    spot_picker_panel.setLayout(spotpickbag);

	    spot_picker_nts = new NameTagSelector(mview);
	    spot_picker_nts.setFont(mview.getSmallFont());
	    spot_picker_nts.loadSelection("ProfileViewer.spot_label");
	    spot_picker_nts.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			spot_picker_nts.saveSelection("ProfileViewer.spot_label");
			
			populateListWithSpots( spot_list );
		    }
		});
	    c = new GridBagConstraints();
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    spotpickbag.setConstraints(spot_picker_nts, c);
	    spot_picker_panel.add(spot_picker_nts);


	    spot_list = new DragAndDropList();
	    spot_list.setSelectionInterval( 0, meas_ids.length - 1);
	    spot_list.addListSelectionListener(new ListSelectionListener() 
		{
		    public void valueChanged(ListSelectionEvent e)
		    {
			spotListSelectionHasChanged();
		    }
		});
	    

	    JScrollPane jsp1a = new JScrollPane( spot_list );
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weightx = 9.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    spotpickbag.setConstraints(jsp1a, c);
	    spot_picker_panel.add(jsp1a);
	    
	    spot_list.setDropAction( new DragAndDropList.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    int[] sids = dnde.getSpotIds();
			    if(sids != null)
			    {
				setSpotListSelection( sids );
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			    try
			    {
				ExprData.Cluster cl = dnde.getCluster();
				if(cl != null)
				{
				    int[] sids = cl.getAllClusterElements();
				    if(sids != null)
					setSpotListSelection( sids );
				}
				
			    }
			    catch(DragAndDropEntity.WrongEntityException wee2)
			    {
			    }
			}
		    }
		});
	
	    populateListWithSpots( spot_list );
	    
	    // // // // // // // // 
	    
	    cluster_picker_panel = new JPanel();
	    
	    CustomKeyListener ckl = new CustomKeyListener();
	    
	    cluster_picker_panel.addKeyListener( ckl );
	    
	    GridBagLayout cluster_picker_bag = new GridBagLayout();
	    cluster_picker_panel.setLayout(cluster_picker_bag);
	    
	    cl_tree = new DragAndDropTree();

	    cl_tree.addKeyListener( ckl );

	    populateTreeWithClusters(cl_tree, edata.getRootCluster() );

	    addTreeDragAndDropActions(cl_tree);
	    
	    cl_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

            cl_tree.addTreeSelectionListener(new TreeSelectionListener() 
		{
		    public void valueChanged(TreeSelectionEvent e) 
		    {
			treeSelectionHasChanged();
		    }
		});

	    JScrollPane jsp1b = new JScrollPane( cl_tree );
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weightx = 9.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    cluster_picker_bag.setConstraints(jsp1b, c);
	    cluster_picker_panel.add(jsp1b);
	    
	    // =========================================================

	    meas_list = new DragAndDropList();

	    populateListWithMeasurements(meas_list);

	    meas_list.setSelectionInterval( 0, meas_ids.length - 1);

	    meas_list.addListSelectionListener(new ListSelectionListener() 
		{
		    public void valueChanged(ListSelectionEvent e)
		    {
			measurementListSelectionHasChanged();
		    }
		});

	    meas_list.setDropAction( new DragAndDropList.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			String[] meas_n = dnde.getMeasurementNames(edata);
			int[] cur_sel = meas_list.getSelectedIndices();
			int n_cur = (cur_sel == null) ? 0 : cur_sel.length ;
			int[] new_sel = new int[meas_n.length];
			int n_new = 0;

			for(int n=0; n < meas_n.length; n++)
			{
			    ListModel lm = meas_list.getModel();
			    int i = -1;
			    for(int o=0; o < lm.getSize(); o++)
				if(meas_n[n].equals( (String) lm.getElementAt(o)))
				    i = o;
			    if(i >= 0)
			    {
				new_sel[n_new] = i;
				n_new++;
			    }
			}
			if(n_new > 0)
			{
			    int[] mix_sel = new int[n_cur + n_new];

			    for(int s=0; s <  n_cur; s++)
				mix_sel[s] = cur_sel[s];

			    for(int s=0; s < n_new; s++)
				mix_sel[n_cur+s] = new_sel[s];

			    meas_list.setSelectedIndices(mix_sel);
			}
			
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		}
	    });
	
	    JScrollPane jsp2 = new JScrollPane(meas_list);
	    
	    //panel.add(jsp2);

	    JSplitPane jsplt_pane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	    jsplt_pane1.setBottomComponent( pickwrap );
	    jsplt_pane1.setTopComponent(jsp2);
	    jsplt_pane1.setOneTouchExpandable(true);

	    pickwrap_parent = jsplt_pane1;

	    pickwrap_parent = jsplt_pane1;

	    /*
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 2.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsplt_pane1, c);
	    */

	    setPickerPanel(true);

	    
	    // =========================================================

	    pro_panel = new ProfilePanel();

	    pro_panel.setDropAction(new DragAndDropPanel.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
		    // System.out.println("Drop!");
			
			try // is it a cluster?
			{
			    ExprData.Cluster drop_c = dnde.getCluster();
			    //System.out.println("cluster dropped");
			    int[] sids = drop_c.getAllClusterElements();
			    if(sids != null)
			    {
				// show the labels for each spot in the cluster

				for(int s=0;s < sids.length; s++)
					showSpotLabel(sids[s]);

				pro_panel.repaint();
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			    try // or a spot?
			    {
				int[] sids = dnde.getSpotIds();
				if(sids != null)
				{
				    for(int s=0;s < sids.length; s++)
					showSpotLabel(sids[s]);
				    pro_panel.repaint();
				   
				}
				//System.out.println("spot dropped");
				// showSpotLabel(sid);
				
				//pro_panel.repaint();
				
				
			    }
			    catch(DragAndDropEntity.WrongEntityException wee2)
			    {
				// not a cluster or spot, ignore it
			    }
			}
		    }
		});
	    
	    pro_panel.setDragAction(new DragAndDropPanel.DragAction()
		{
		    public DragAndDropEntity getEntity(DragGestureEvent event)
		    {
			if(root_profile_picker != null)
			{
			    Point pt = event.getDragOrigin();
			    int sid = root_profile_picker.findProfile(pt.x, pt.y);
			    if(sid >= 0)
			    {
				return DragAndDropEntity.createSpotNameEntity(sid);
			    }

			    // maybe it is a cluster glyph?
			    // was the click over any of the cluster glyphs?
			    int gx = pt.x / graph_sx;
			    int gy = pt.y / graph_sy;
			    int cli = 0;

			    if((gx <= n_cols) && (gy <= n_rows))
			    {
				//System.out.println("hit in graph " + gx + "," + gy);
				
				int cx = xoff + (gx * graph_sx) + graph_w + ticklen;
				int cy = yoff + (gy * graph_sy) - ticklen;
				
				int mx = pt.x;
				int my = pt.y;
				
				//System.out.println("mouse=" + e.getX() + "," +  e.getY() + " clust=" + cx + "," + cy);
				
				if((mx >= cx) && (mx < (cx+ticklen)) &&
				   (my >= cy) && (my < (cy+ticklen)))
				{
				    //System.out.println("drag hit on glyph for cluster " + gx + "," + gy);

				    // work out which cluster it is...
				    int cr = 0;
				    int cc = 0;
				    for(int s=0; s < sel_cls.size(); s++)
				    {
					if((cc == gx) && (cr == gy))
					{
					    // select this one for dragging
					    return DragAndDropEntity.createClusterEntity( (ExprData.Cluster) sel_cls.elementAt(s) );
					}
					else
					{
					    if(++cc == n_cols)
					    {
						cr++;
						cc = 0;
					    }
					}
				    }
				}
			    }
			}
			return null;
		    }
		});;
	    
	    
	    
	    //pro_panel.setPreferredSize(new Dimension(400, 350));
	    
	    JScrollPane jsp3 = new JScrollPane(pro_panel);
	    /*
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 8.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp3, c);
	    */
	    //panel.add(jsp3);

	    JSplitPane jsplt_pane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    jsplt_pane2.setLeftComponent(jsplt_pane1);
	    jsplt_pane2.setRightComponent(jsp3);
	    jsplt_pane2.setOneTouchExpandable(true);

	    // ------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsplt_pane2, c);
	    panel.add(jsplt_pane2);


	}

	// ========================================================================================

	{
	    // 
	    // bottom button panel
	    //
	    JButton button = null;
	    
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    int col = 0;

	    // ------------------

	    
	    // ------------------

	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter = mview.getBooleanProperty("ProfileViewer.apply_filter", false);
	    apply_filter_jchkb.setSelected(apply_filter);
	    apply_filter_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			populateListWithSpots( spot_list );
			pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(apply_filter_jchkb, c);
	    wrapper.add(apply_filter_jchkb);

	    // ------------------

	    show_mean_jchkb = new JCheckBox("Show mean");
	    show_mean = mview.getBooleanProperty("ProfileViewer.show_mean", false);
	    show_mean_jchkb.setSelected(show_mean);
	    show_mean_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			show_mean = show_mean_jchkb.isSelected();
			// updateProfiles();
			pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    // c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(show_mean_jchkb, c);
	    wrapper.add(show_mean_jchkb);

	    // ------------------

	    include_children_jchkb = new JCheckBox("Include children");
	    include_children_jchkb.setToolTipText( "Include all of the child clusters with their selected parent" );
	    include_children = mview.getBooleanProperty("ProfileViewer.include_children", true);
	    include_children_jchkb.setSelected(include_children);
	    include_children_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			include_children = include_children_jchkb.isSelected();
			updateProfiles();
			pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    // c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(include_children_jchkb, c);
	    wrapper.add(include_children_jchkb);

	    // ------------------
	    
	    uniform_scale_jchkb = new JCheckBox("Uniform scale");
	    uniform_scale_jchkb.setToolTipText( "Force each component to use the same vertical scale" );
	    uniform_scale = mview.getBooleanProperty("ProfileViewer.uniform_scale", true);
	    uniform_scale_jchkb.setSelected(uniform_scale);
	    uniform_scale_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			uniform_scale = uniform_scale_jchkb.isSelected();
			// updateProfiles();
			pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(uniform_scale_jchkb, c);
	    wrapper.add(uniform_scale_jchkb);

	    // ------------------
	
	    button = new JButton("Print");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			new PrintManager( mview, pro_panel, pro_panel ).openPrintDialog();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    addFiller( wrapper, w_gridbag, 0, col++, 10 );
	    
	    button = new JButton("Help");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("ProfileViewer", "ProfileViewer");
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    addFiller( wrapper, w_gridbag, 0, col++, 10 );

	    button = new JButton("Close");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);
	    	
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	// ===== axis =====================================================================================

	axis_man = new AxisManager(mview);

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    pro_panel.repaint();
		}
	    });

	axis_man.addAxis(new PlotAxis(mview, "Value"));
	axis_man.addAxis(new PlotAxis(mview, "Count"));

	// ===== decorations ===============================================================================

	deco_man = new DecorationManager(mview, "ProfileViewer");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    pro_panel.repaint();
		}
	    });

	// ===== printing ==================================================================================

	// =================================================================================================

    }


    private void addFiller( JPanel panel, GridBagLayout bag, int row, int col, int size )
    {
	Dimension fillsize = new Dimension( size, size);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridy = row;
	c.gridx = col;
	bag.setConstraints(filler, c);
	panel.add(filler);
    }


    public void cleanUp()
    {
	mview.putBooleanProperty("ProfileViewer.auto_space", auto_space );
	mview.putBooleanProperty("ProfileViewer.col_sel", col_sel );

	mview.putBooleanProperty("ProfileViewer.include_children", include_children_jchkb.isSelected() );
	mview.putBooleanProperty("ProfileViewer.uniform_scale", uniform_scale_jchkb.isSelected() );
	mview.putBooleanProperty("ProfileViewer.apply_filter", apply_filter_jchkb.isSelected() );
	mview.putBooleanProperty("ProfileViewer.show_mean", show_mean_jchkb.isSelected() );
	
	// edata.removeExternalSelectionListener(sel_handle);

	edata.removeObserver(this);
	frame.setVisible(false);
    }


    // ============= measurement list ===============================

    private void populateListWithMeasurements(JList list)
    {
	// save existing selection if any
	Hashtable sels = new Hashtable();
	ListSelectionModel lsm = list.getSelectionModel();
	if(lsm != null)
	{
	    for(int s=lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
	    {
		if(lsm.isSelectedIndex(s))
		    sels.put( list.getModel().getElementAt(s) , "x");
	    }
	}
	
	// build a vector of names to use as the list data
	Vector data = new Vector();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi))
	    {
		data.addElement(edata.getMeasurementName(mi));
	    }
	}
	list.setListData( data );
	
	// update the meas_id map
	meas_ids = new int[ data.size() ];
	int mp = 0;
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi))
	    {
		meas_ids[mp++] = mi;
	    }
	}

	// and restore the selection if there was one
	if(sels.size() > 0)
	{
	    Vector sels_v = new Vector();

	    // check each of the new elements 
	    for(int o=0; o < data.size(); o++)
	    {
		String name = (String) data.elementAt(o);
		if(sels.get(name) != null)
		{
		    sels_v.addElement(new Integer(o));
		}
	    }

	    int[] sel_cl_ids = new int[ sels_v.size() ];
	    for(int s=0; s <  sels_v.size(); s++)
	    {
		sel_cl_ids[s] = ((Integer) sels_v.elementAt(s)).intValue();
	    }

	    list.setSelectedIndices(sel_cl_ids);

	}		
    }

    // ============= spot list ===============================

    private void spotListSelectionHasChanged()
    {
	//	System.out.println("spotListSelectionHasChanged()");

	storePosition();

	Object[] list_sel_names = (Object[]) spot_list.getSelectedValues();
	Vector master_hits = new Vector();

	if(list_sel_names != null)
	{
	    for(int s=0; s < list_sel_names.length; s++)
	    {
		String selname = (String) list_sel_names[ s ];
		Vector hits = (Vector) sel_spot_back_map.get( selname );
		
		if(hits != null)
		    for(int h=0; h < hits.size(); h++)
			master_hits.addElement( hits.elementAt( h ));
	    }
	}
	
	sel_spot_ids = new int[ master_hits.size() ];
	for(int s=0; s < sel_spot_ids.length; s++)
	    sel_spot_ids[s] = ((Integer) master_hits.elementAt(s)).intValue();

	// System.out.println(list_sel_names.length + " names -> " + sel_spot_ids.length + " spots");

	// updateProfiles();
	pro_panel.repaint();

    }

    private void populateListWithSpots(JList list)
    {

	// save existing selection if any
	HashSet sels = new HashSet();
	ListSelectionModel lsm = list.getSelectionModel();
	if(lsm != null)
	{
	    for(int s=lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
	    {
		if(lsm.isSelectedIndex(s))
		    sels.add( list.getModel().getElementAt(s) );
	    }
	}



	final ExprData.NameTagSelection nts = spot_picker_nts.getNameTagSelection();
	final int ns = edata.getNumSpots();

	// build a hashtable of names to use as the list data
	// (to uniqify the names.....)
	
	java.util.HashSet data = new java.util.HashSet();

	final boolean af = ((apply_filter_jchkb != null) && (apply_filter_jchkb.isSelected()));

	sel_spot_back_map = new Hashtable();
	for(int s=0; s < ns; s++)
	{
	    if(!af || (!edata.filter(s)))
	    {
		final String n = nts.getNameTag( s );

		if(n != null)
		{
		    Vector indices = (Vector) sel_spot_back_map.get( n );
		    if(indices == null)
		    {
			data.add( n );
			indices = new Vector();
			sel_spot_back_map.put(n, indices);
		    }
		    indices.addElement( new Integer(s) );
		}
	    }
	}
	
	String[] names = new String[ data.size() ];
	int np = 0;

	for(java.util.Iterator it = data.iterator() ; it.hasNext();  )
	{
	    names[np] = (String) it.next();
	    
	    np++;
	}
	
	java.util.Arrays.sort(names);

	// build a reverse map from name to index in the list
	// (needed to handle dropping of spots onto the list)
	//
	sel_spot_name_map = new Hashtable();
	for(int nps=0; nps < names.length; nps++)
	    sel_spot_name_map.put( names[nps], new Integer( nps ));
	

	// and install the new list data

	list.setListData( names );
	


	// and restore the previous selection if there was one
	if(sels.size() > 0)
	{
	    final Vector sels_v = new Vector();

	    // check each of the new elements 
	    for(int o=0; o < names.length; o++)
	    {
		if( sels.contains( names[ o ] ) )
		{
		    sels_v.addElement( new Integer( o ) );
		}
	    }

	    final int[] sel_cl_ids = new int[ sels_v.size() ];
	    for( int s=0; s <  sels_v.size(); s++ )
	    {
		sel_cl_ids[s] = ( (Integer) sels_v.elementAt(s)).intValue();
	    }

	    list.setSelectedIndices( sel_cl_ids );
	}		

    }

    private void setSpotListSelection( int[] sids )
    {
	// System.out.println("setSpotListSelection(): got " + sids.length + " spots");
	
	// we get a bunch of spot ids, want to convert to a bunch of list indices

	// check each spot id against indices in the back_map

	final ExprData.NameTagSelection nts = spot_picker_nts.getNameTagSelection();

	final Vector hits = new Vector();

	for(int s=0; s < sids.length; s++)
	{
	    // get the name tag(s) for this spot

	    final String n = nts.getNameTag( sids[s] );
	    
	    if(n != null)
	    {
		final Integer lid_i = (Integer) sel_spot_name_map.get( n );
		
		if(lid_i != null)
		    hits.addElement(lid_i);
	    }
	    
	}
	
	int[] sels = new int[ hits.size() ];

	for(int s=0; s <  hits.size(); s++)
	    sels[s] = ((Integer) hits.elementAt(s)).intValue();

	// System.out.println("setSpotListSelection(): " + hits.size() + " hits");
	
	spot_list.setSelectedIndices( sels );

    }
    
    // ============= cluster tree ===============================

    private void populateTreeWithClusters(JTree tree, ExprData.Cluster cluster)
    {
	// System.out.println("making tree for " + cluster.getName() );
	
	// record the selected items
	TreeSelectionModel tsm = tree.getSelectionModel();
	Hashtable sels = new Hashtable();
	if(tsm != null)
	{
	    TreePath[] tpaths = tsm.getSelectionPaths();
	    if(tpaths != null)
	    {
		for(int tp=0; tp < tpaths.length; tp++)
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tpaths[tp].getLastPathComponent();
		    ExprData.Cluster clust = (ExprData.Cluster) node.getUserObject();
		    sels.put(String.valueOf(clust.getId()), clust);
		}
	    }
	}

	DefaultMutableTreeNode dmtn = generateTreeNodes( null, cluster );
	if(dmtn == null)
	    return;

	DefaultTreeModel model =  new DefaultTreeModel( dmtn );
	tree.setModel(model);

	tree.putClientProperty("JTree.lineStyle", "Angled");
	
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	renderer.setOpenIcon(null);
	renderer.setClosedIcon(null);
	tree.setCellRenderer(renderer);
	
	if(sels.size() > 0)
	{
	    //System.out.println(sels.size() + " saved selections");

	    /*
	    for (Enumeration e =  sels.keys(); e.hasMoreElements() ;) 
	    {
		String id = e.nextElement();
		ExprData.Cluster clust = (ExprData.Cluster) sels.get( id );
		
	    }
	    */
	    Vector tp_vec = new Vector();

	    for (Enumeration e = dmtn.depthFirstEnumeration(); e.hasMoreElements() ;) 
	    {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
		ExprData.Cluster clu = (ExprData.Cluster) node.getUserObject();
		String id = String.valueOf(clu.getId());
		if(sels.get(id) != null)
		{
		    // make a path for this node
		    TreeNode[] tn_path = model.getPathToRoot(node);
		    TreePath tp = new TreePath(tn_path);
		    // and record it for later
		    tp_vec.addElement(tp);
		}
	    }
	    // add all of the Selection paths in one go
	    TreePath[] tp_a = (TreePath[]) tp_vec.toArray(new TreePath[0]);
	    tree.setSelectionPaths( tp_a );

	    pro_panel.repaint();
	}
	
    }

    private DefaultMutableTreeNode generateTreeNodes(DefaultMutableTreeNode parent, ExprData.Cluster clust )
    {
	if(clust.getIsSpot())
	{
	    DefaultMutableTreeNode node = new DefaultMutableTreeNode( clust );
	    
	    Vector ch = clust.getChildren();
	    if(ch != null)
	    {
		for(int c=0; c < ch.size(); c++)
		    generateTreeNodes( node, ( ExprData.Cluster) ch.elementAt(c) );
	    }
	    
	    if(parent != null)
	    {
		parent.add(node);
		return parent;
	    }
	    else
	    {
		return node;
	    }
	}
	return null;
    }

    private void addTreeDragAndDropActions(final DragAndDropTree tree)
    {
	tree.setDropAction( new DragAndDropTree.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			ExprData.Cluster cl = dnde.getCluster();
			
			DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			
			for (Enumeration e =  root.depthFirstEnumeration(); e.hasMoreElements() ;) 
			{
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
				ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
				if(cl == ncl)
				{
				    TreeNode[] tn_path = model.getPathToRoot(dmtn);
				    TreePath tp = new TreePath(tn_path);
				    
				    tree.expandPath(tp);
				    tree.scrollPathToVisible(tp);
				    tree.setSelectionPath(tp);
				    return;
				}
			}
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		}
	    });
	
	tree.setDragAction(new DragAndDropTree.DragAction()
	    {
		public DragAndDropEntity getEntity()
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		    
		    if(node != null)
		    {
			ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
			
			DragAndDropEntity dnde = DragAndDropEntity.createClusterEntity(cluster);
			
			return dnde;
		    }
		    else
			return null;
		}
	    });
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
	dplot = mview.getDataPlot();

	buildGUI();

	frame.pack();
	frame.setVisible(true);

	// register ourselves with the data
	//
	edata.addObserver(this);

	// sel_handle = edata.addExternalSelectionListener(this);

    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Profile Viewer", "viewer", 
					 "Shows the profile of elements within one or more clusters", "",
					 1, 2, 1);
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
	switch(due.event)
	{
	case ExprData.ColourChanged:
	    break;
	case ExprData.OrderChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.RangeChanged:
	    spotListSelectionHasChanged();
	    pro_panel.repaint();
	    break;
	case ExprData.VisibilityChanged:
	    break;
	case ExprData.NameChanged:
	case ExprData.SizeChanged:
	    populateListWithSpots(spot_list);
	    spotListSelectionHasChanged();
	    pro_panel.repaint();
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	case ExprData.VisibilityChanged:
	    break;
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    if(cl_tree != null)
	    {
		populateTreeWithClusters(cl_tree, edata.getRootCluster() );
		treeSelectionHasChanged();
	    }
	    break;
	}
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    populateListWithMeasurements(meas_list);
	    measurementListSelectionHasChanged();
	    break;
	}
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();
	pro_panel.repaint();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void setPickerPanel(boolean display_clusters)
    {
	// to update the graph layout....
	
	
	// ------------

	JComponent rjc = (picker_pick_spots) ? spot_picker_panel : cluster_picker_panel;

	pickwrap.remove( rjc );

	picker_pick_spots = !display_clusters;

	JComponent jc = (picker_pick_spots) ? spot_picker_panel : cluster_picker_panel;

	GridBagConstraints c = new GridBagConstraints();
	c.gridy = 1;
	c.gridwidth = 2;
	c.weightx = 9.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	pickbag.setConstraints(jc, c);
	pickwrap.add(jc);
	
        pickwrap.revalidate();
	pickwrap_parent.revalidate();
	pickwrap_parent.repaint();

	if(display_clusters)
	{
	    treeSelectionHasChanged();
	}
	else
	{
	    n_cols = 1;
	    n_rows = 1;
	
	    layoutGraphs();

	    if(pro_panel != null)
	    {
		updateProfiles();
		
		pro_panel.repaint();
	    }
	}

    }


    private void measurementListSelectionHasChanged()
    {
	int[] sel_inds = meas_list.getSelectedIndices();

	meas_ids = new int[ sel_inds.length ];
	int mp = 0;

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(meas_list.isSelectedIndex( m ))
	    {
		meas_ids[mp++] = mi;
	    }
	}

	// updateProfiles();
	
	pro_panel.repaint();
    }

    private void layoutGraphs( )
    {
	if(picker_pick_spots)
	{
	    n_cols = 1;
	    n_rows = 1;
	}
	else
	{
	    TreeSelectionModel tsm = (TreeSelectionModel) cl_tree.getSelectionModel();
	    
	    int mi = tsm.getMinSelectionRow();
	    int ma = tsm.getMaxSelectionRow();
	    
	    int count = 0;
	    
	    for(int s=mi; s <= ma; s++)
	    {
		if(tsm.isRowSelected(s))
		{
		    count++;
		}
	    }
	    

	    //System.out.println( "layoutGraphs(): " + count + " things selected..." );


	    if(count > 0)
	    {
		// find the nicest factors of 'count'
		
		// pick the two factors that are closest to one another
		
		double best_rc_diff = Double.MAX_VALUE;
		
		n_cols = count;

		for(int m=1; m < count; m++)
		{
		    double c = (double) m;
		    double r = ((double) count) / c;
		    
		    double rc_diff = Math.abs(r - c);
		    if(rc_diff <  best_rc_diff)
		    {
			best_rc_diff= rc_diff;
			n_cols = (int) c;
		    }
		}
		
		n_rows = (n_cols > 0) ? (int)(Math.ceil((double)count / (double)n_cols)) : count;

		if( n_rows < 1 )
		    n_rows = 1;
		if( n_cols < 1 )
		    n_cols = 1;

		//System.out.println( "layoutGraphs(): layout is " + n_rows + "x" + n_cols );

	    }
	}
    }

    private void treeSelectionHasChanged()
    {

	// possibly save the current view

	if( sel_cls.size() > 0 )
	    storePosition();
	sel_cls.clear();

	// how many selected things?
	
	TreeSelectionModel tsm = (TreeSelectionModel) cl_tree.getSelectionModel();

	int mi = tsm.getMinSelectionRow();
	int ma = tsm.getMaxSelectionRow();


	for(int s=mi; s <= ma; s++)
	{
	    if(tsm.isRowSelected(s))
	    {
		TreePath tp = cl_tree.getPathForRow(s);
		DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();
		ExprData.Cluster cl = (ExprData.Cluster) dmtn.getUserObject();
		sel_cls.addElement(cl);
	    }
	}

	layoutGraphs();
	
	if(pro_panel != null)
	{
	    updateProfiles();
	    
	    pro_panel.repaint();
	}
    }

    /**
     * repoulates the sel_cl_ids vector after a selection change
     * ( sel_cl_ids is a vector of int[] for each selected cluster )
     *  
     */
    private void updateProfiles()
    {
	if( picker_pick_spots )
	{

	}
	else
	{
	    sel_cl_ids.clear();
	    
	    for(int s=0; s < sel_cls.size(); s++)
	    {
		ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);
		int[] ids = include_children ? cl.getAllClusterElements() :  cl.getElements();
		sel_cl_ids.addElement(ids);
	    }
	}
    }

    private void selectClusterInTree(final JTree tree, final ExprData.Cluster cl)
    {
	DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
	DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
	for (Enumeration en =  root.depthFirstEnumeration(); en.hasMoreElements() ;) 
	{
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) en.nextElement();
	    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
	    if(cl == ncl)
	    {
		TreeNode[] tn_path = model.getPathToRoot(dmtn);
		TreePath tp = new TreePath(tn_path);
		
		tree.scrollPathToVisible(tp);
		tree.setSelectionPath(tp);
	    }
	}
    }

    private void selectClustersInTree(final JTree tree, final Hashtable clusters)
    {
	Vector tree_paths_v = new Vector();
	DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
	DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

	for (Enumeration en =  root.depthFirstEnumeration(); en.hasMoreElements() ;) 
	{
	    
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) en.nextElement();
	    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();

	    ExprData.Cluster cl = (ExprData.Cluster) clusters.get(ncl);

	    if(cl != null)
	    {
		TreeNode[] tn_path = model.getPathToRoot(dmtn);
		TreePath tp = new TreePath(tn_path);
		
		tree_paths_v.addElement(tp);
	    }
	}
	if(tree_paths_v.size() > 0)
	{
	    tree.setSelectionPaths((TreePath[]) tree_paths_v.toArray( new TreePath[tree_paths_v.size()] ));
	    tree.scrollPathToVisible((TreePath) tree_paths_v.elementAt(0));
	    treeSelectionHasChanged();
	}
    }

    private void expandClusterInTree(final JTree tree, final ExprData.Cluster cl)
    {
	// select cluster 'cl' and "some" of its children....
	int max = 10;
	
	if(cl.getNumChildren() >= 10)
	    max = cl.getNumChildren() + 1;

	Vector start = new Vector();
	start.addElement(cl);

	Vector clusters = new Vector();

	addClusters( 0, max, start, clusters );

	//System.out.println("decided to add " + clusters.size() + " clusters...");

	Hashtable cl_ht = new Hashtable();

	for(int c=0; c < clusters.size(); c++)
	    cl_ht.put( clusters.elementAt(c), clusters.elementAt(c));

	selectClustersInTree( cl_tree, cl_ht );
    }

    private void addClusters( final int depth, final int space_left, Vector cl_v, final Vector vec )
    {
	//System.out.println("depth:" + depth + ": has "+  cl_v.size() + " clusters");

	// is there enough space to add all the clusters in the vector 'cl_v'?
	final int size = cl_v.size();
	if(size <  space_left)
	{
	    //System.out.println("depth:" + depth + ": adding these "+  cl_v.size() + " clusters");

	    for(int c=0; c < size; c++)
		vec.addElement( cl_v.elementAt( c ));
	}
	else
	{
	    //System.out.println("depth:" + depth + ": no room for these "+  cl_v.size() + " clusters");

	    return;
	}

	// might there be any room for the children?
	if((space_left-size) > 0)
	{
	    //System.out.println("depth:" + depth + ": checking for next depth....");

	    // now compose a vector of the children of all of the clusters in the vector 'cl_v'
	    Vector all_ch_v = new Vector();
	    for(int c=0; c < size; c++)
	    {
		Vector ch_v = ((ExprData.Cluster)cl_v.elementAt(c)).getChildren();
		if(ch_v != null)
		    for(int cc=0; cc < ch_v.size(); cc++)
			all_ch_v.addElement( ch_v.elementAt( cc ));
	    }
	    
	    //System.out.println("depth:" + depth + ": next depth will have "+  all_ch_v.size() + " clusters");
	    
	    // and try to add this
	    if(all_ch_v.size() > 0)
		addClusters( depth+1, space_left - size, all_ch_v, vec );
	}
    }

    // ----------------------------------------------------------------------------------------------
    //
    //  tree expander
    //
    // ----------------------------------------------------------------------------------------------

    private final void openTreeToDepth(final int depth)
    {
	final JTree target = cl_tree;
	if(target == null)
	    return;
	final DefaultTreeModel model      = (DefaultTreeModel)       target.getModel();
	if(model == null)
	    return;
	final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();

	// fully collapse the tree...
	int rc = target.getRowCount();
	while(rc > 0)
	{
	    if(target.isExpanded(rc))
		target.collapseRow(rc);
	    rc--;
	}
	
	if(depth > 0)
	    openNodeToDepth(target, model, node, depth);


    }

    private final void openNodeToDepth(final JTree target, final DefaultTreeModel model, 
				       final DefaultMutableTreeNode node, final int depth)
    {
	TreeNode[] tn_path = model.getPathToRoot(node);
	TreePath tp = new TreePath(tn_path);
	
	if(depth > 0)
	{
	    target.expandPath(tp);
	    final int n_c = node.getChildCount();
	    for(int c=0; c < n_c; c++)
	    {
		openNodeToDepth(target, model, (DefaultMutableTreeNode) node.getChildAt(c), depth-1);
	    }
	}
	else
	{
	    target.collapsePath(tp);
	}
    }


    private final void openTreeToDepth(final JTree target, final int depth)
    {
	if(target == null)
	    return;
	final DefaultTreeModel model      = (DefaultTreeModel)       target.getModel();
	if(model == null)
	    return;
	final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();

	// fully collapse the tree...
	int rc = target.getRowCount();
	while(rc > 0)
	{
	    if(target.isExpanded(rc))
		target.collapseRow(rc);
	    rc--;
	}
	
	if(depth > 0)
	    openNodeToDepth(target, model, node, depth);


    }

    public class CustomKeyListener implements KeyListener
    {
	public void keyTyped(KeyEvent e) 
	{
	}
	
	public void keyPressed(KeyEvent e) 
	{
	}
	
	public void keyReleased(KeyEvent e) 
	{
	    handleKeyEvent(e);
	}
	public void keyAction(KeyEvent e) 
	{
	}

	protected void handleKeyEvent(KeyEvent e)
	{
	    int od = -1;

	    switch(e.getKeyCode())
	    {
	    case KeyEvent.VK_0:
		od = 0;
		break;
	    case KeyEvent.VK_1:
		od = 1;
		break;
	    case KeyEvent.VK_2:
		od = 2;
		break;
	    case KeyEvent.VK_3:
		od = 3;
		break;
	    case KeyEvent.VK_4:
		od = 4;
		break;
	    case KeyEvent.VK_5:
		od = 5;
		break;
	    case KeyEvent.VK_6:
		od = 6;
		break;
	    case KeyEvent.VK_7:
		od = 7;
		break;
	    case KeyEvent.VK_8:
		od = 8;
		break;
	    }
	    
	    if(od >= 0)
	    {
		//System.out.println(" key_event: od=" + od );

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) cl_tree.getLastSelectedPathComponent();
		if(node != null)
		{
		    final DefaultTreeModel model = (DefaultTreeModel) cl_tree.getModel();
		    if(model == null)
			return;
		    openNodeToDepth(cl_tree, model, node, od);
		}
		else
		{
		    openTreeToDepth(od);
		}
	    }

	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ExternalSelectionListener
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void spotSelectionChanged(int[] spot_ids)
    {
	pro_panel.repaint();
    }

    public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }
    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ProfileViewerPanel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class ProfilePanel extends DragAndDropPanel implements MouseListener, MouseMotionListener, Printable
    {
	private Point last_pt = null;

	private double scale = 1.0;

	private Polygon[] glyph_poly = null;
	private int glyph_poly_height;

	private String tool_tip_text = null;

	public ProfilePanel()
	{
	    super();
	    addMouseListener(this);
	    addMouseMotionListener(this);

	    sel_colour = text_col.brighter();
	    unsel_colour = text_col.darker();
	}

	public void mouseMoved(MouseEvent e) 
	{
	    Point pt = new Point();
	    double xval, yval, xval_t, yval_t;

	    pt.x  = e.getX();
	    pt.y  = e.getY();

	    if(root_profile_picker != null)
	    {
		int sid = root_profile_picker.findProfile(pt.x, pt.y);
		if(sid >= 0)
		{
		    nt_sel = nts.getNameTagSelection();
		    
		    String str = nt_sel.getNameTag(sid);
		   
		    tool_tip_text = str;
		    
		    setToolTipText(str);
		}
		else
		{
		    tool_tip_text = null;
		}
	    }
	}

	public String getToolTipText(MouseEvent event)
	{
	    return tool_tip_text;
	}

	public void mouseDragged(MouseEvent e) 
	{
	} 

	public void mousePressed(MouseEvent e) 
	{
	    
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	}

	public void mouseClicked(MouseEvent e) 
	{
	    // was the click over any of the cluster glyphs?
	    int gx = e.getX() / graph_sx;
	    int gy = e.getY() / graph_sy;
	    if((gx <= n_cols) && (gy <= n_rows))
	    {
		//System.out.println("hit in graph " + gx + "," + gy);

		int cx = xoff + (gx * graph_sx) + graph_w + ticklen;
		int cy = yoff + (gy * graph_sy) - ticklen;

		int mx = e.getX();
		int my = e.getY();

		//System.out.println("mouse=" + e.getX() + "," +  e.getY() + " clust=" + cx + "," + cy);

		if((mx >= cx) && (mx < (cx+ticklen)) &&
		   (my >= cy) && (my < (cy+ticklen)))
		{
		    //System.out.println("hit on glyph for cluster " + gx + "," + gy);
		    
		    // work out which cluster it is...
		    int cr = 0;
		    int cc = 0;
		    for(int s=0; s < sel_cls.size(); s++)
		    {
			if((cc == gx) && (cr == gy))
			{
			    // select this cluster in the tree

			    ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);

			    //System.out.println("this is cluster " + cl.getName());
			    
			    if(sel_cls.size() == 1)
			    {
				// this is the only cluster graph, expand it...
				expandClusterInTree( cl_tree, cl );
			    }
			    else
			    {
				// this is one of many cluster graphs, select it as the only one...

				// selectClusterInTree( cl_tree, cl );

				// or, expand it
				expandClusterInTree( cl_tree, cl );
				
			    }
			    return;

			}
			else
			{
			    if(++cc == n_cols)
			    {
				cr++;
				cc = 0;
			    }
			}
		    }
		}
	    }
	    
	    if(root_profile_picker != null)
	    {
		int sid = root_profile_picker.findProfile(e.getX(), e.getY());
		if(sid >= 0)
		{
		    toggleSpotLabel(sid);
		    return;
		}
	    }
	    

	    
	}

	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    if(sel_colours == null)
	    {
		sel_colours = new Color[n_sel_colours];
		sel_colours[0] = Color.red;
		sel_colours[1] = Color.green;
		sel_colours[2] = Color.blue;
		sel_colours[3] = Color.yellow;
		sel_colours[4] = Color.magenta;
		sel_colours[5] = Color.cyan;
		sel_colours[6] = Color.pink;
		sel_colours[7] = Color.white;
	    }

	    
	    ( (Graphics2D) graphic ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
	    
	    
	    drawProfiles(graphic, getWidth(), getHeight());

	    
	}

	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    // margins
	    //
	    g.translate((int)pf.getImageableX(), 
			(int)pf.getImageableY());
	    
	    // area of one page
	    //
	    // ??  area seems to be too small, c.f. ScatterPlot...
	    //
	    int pw = (int)(pf.getImageableWidth() - pf.getImageableX());   
	    int ph = (int)(pf.getImageableHeight() - pf.getImageableY());
	    
	    //System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	    drawProfiles(g, pw, ph );

	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	    // panel.paintIntoRegion(g, pw, ph);

	    return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}

	public void drawProfiles(Graphics graphic, int width, int height)
	{
	    try
	    {
		
		graphic.setColor(background_col);
		graphic.fillRect(0, 0, width, height);
		graphic.setColor(text_col);
		graphic.setFont(dplot.getFont());
		
		root_profile_picker = new ProfilePicker();
		root_profile_picker.setupPicker( width, height );

		if((n_cols > 0) && (n_rows > 0))
		{
		    
		    graph_sx = (int)((double)width * scale)  / n_cols;
		    graph_sy = (int)((double)height * scale) / n_rows;
		    
		    graph_w = (int)((double)graph_sx * 0.8 * scale);
		    graph_h = (int)((double)graph_sy * 0.8 * scale);
		    
		    ticklen = graph_h / 20;
		    
		    int col = 0;
		    
		    xoff = (int)((double)graph_sx * 0.1 * scale);
		    yoff = (int)((double)graph_sy * 0.1 * scale);
		    
		    int xp = xoff;
		    int yp = yoff;
		    
		    if(meas_ids.length < 2)
			return;
		    
		    int[] ssel =  edata.getSpotSelection();
		    coloured = (ssel.length > 0);
		    
		    font = new Font("Helvetica", 1, (int)( (double)ticklen * scale * spot_font_scale));
		    frc = new FontRenderContext(null, false, false);
		    
		    
		    apply_filter = apply_filter_jchkb.isSelected();
		    
		    meas_labels   = new TextLayout[meas_ids.length];
		    meas_labels_o = new int[meas_ids.length];
		    

		    // find the min & max for the selected  measurements....
		    double max = -Double.MAX_VALUE;  // edata.getMinEValue();
		    double min = Double.MAX_VALUE;   // edata.getMaxEValue();
		    
		    for(int m=0; m < meas_ids.length; m++)
		    {
			ExprData.Measurement meas = edata.getMeasurement( meas_ids[m] );

			final double local_min = edata.getMeasurementMinEValue( meas_ids[ m ] );
			
			if( Double.isNaN( local_min ) == false )
			    if( ( m == 0 ) || ( local_min < min ) )
				min = local_min;

			final double local_max = edata.getMeasurementMaxEValue( meas_ids[ m ] );

			if( Double.isNaN( local_max ) == false )
			    if( ( m == 0 ) || ( local_max > max ) )
				max = local_max;
			
			/*
			if( meas.getMinEValue() < min )
			    min = meas.getMinEValue();
			if( meas.getMaxEValue() > max )
			    max = meas.getMaxEValue();
			*/

			String meas_str = meas.getName();

			meas_labels[m] = new TextLayout( meas_str, font, frc);
			meas_labels_o[m] = (int)( meas_labels[m].getBounds().getWidth() / 2.0 );
		    }
		    

		    String str = mview.niceDouble(min, 9, 4);
		    min_label = new TextLayout( str, font, frc);
		    min_label_o = (int)(min_label.getBounds().getWidth() / 2.0);
		    
		    zero_label = new TextLayout( "0.0", font, frc);
		    zero_label_o = (int)(zero_label.getBounds().getWidth() / 2.0);
		    
		    str = mview.niceDouble(max, 9, 4);
		    max_label = new TextLayout( str, font, frc);
		    max_label_o = (int)(max_label.getBounds().getWidth() / 2.0);
		    
		    draw_zero = min < .0;
		    

	    
		    graph_x_axis_step = graph_w / ( meas_ids.length-1 );
		    



		    
		    //if( uniform_scale )
		    {
			graph_y_axis_min   = min;
			graph_y_axis_max   = max;
			graph_y_axis_scale = (double) graph_h / ( max - min );
		    }
		    //else
		    //{
			
		    //}


		    if( (glyph_poly == null) || ( glyph_poly_height != ticklen ) )
		    {
			// generate (or re-generate at a new size) the glyphs
			glyph_poly = mview.getDataPlot().getScaledClusterGlyphs(ticklen);
			glyph_poly_height = ticklen;
		    }
		    
		    String name = null;
		    Graphics2D g2 = (Graphics2D) graphic;
		    
		    resetLabelMap(width, height);
		    colour_alloc = 0;
		    
		    if( picker_pick_spots )
		    {
			drawAxes( graphic, xp, yp, graph_w, graph_h, min, max );
			
			if(sel_spot_ids != null)
			    drawElementsInto( graphic, null, sel_spot_ids, xp, yp, graph_w, graph_h );
			
		    }
		    else
		    {
			for(int s=0; s < sel_cls.size(); s++)
			{
			    ExprData.Cluster cl  = (ExprData.Cluster) sel_cls.elementAt(s);
			    int[]            spot_ids = (int[])            sel_cl_ids.elementAt(s);
			    
			    graphic.setColor( text_col );
			    
			    if(!uniform_scale)
			    {
				// work out the scale for this graph
				
				if( (spot_ids != null) && (spot_ids.length > 0) )
				{
				    min = max = edata.eValue( meas_ids[0], spot_ids[0] );
				    
				    for(int m=0; m < meas_ids.length; m++)
				    {
					for(int i=0; i < spot_ids.length; i++)
					{
					    final double e = edata.eValue(meas_ids[m], spot_ids[i]);
					    
					    if(e < min)
						min = e;
					    if(e > max)
						max = e;
					}
				    }
				    graph_y_axis_min = min;
				    graph_y_axis_scale = (double) graph_h / (max - min);
				}
				
			    }
			    
			    drawAxes( graphic, xp, yp, graph_w, graph_h, min, max );
			    
			    if(spot_ids != null)
			    {
				//graphic.drawString( cl.getName() + " (" + String.valueOf(ids.length) + ")", xp, yp );
				name =  cl.getName() + " (" + String.valueOf(spot_ids.length) + ")";
				
				drawElementsInto( graphic, cl, spot_ids, xp, yp, graph_w, graph_h );
			    }
			    else
			    {
				name =  cl.getName() + " (0)";
				//graphic.drawString( cl.getName() + " (0)", xp, yp );
			    }
			    
			    graphic.setColor( text_col );
			    
			    // the title
			    TextLayout name_tl = new TextLayout( name, font, frc);
			    int name_o = (int)((name_tl.getBounds().getWidth()) / 2.0) ;
			    AffineTransform new_at = new AffineTransform();
			    new_at.translate(xp+(graph_w/2)-name_o, yp-ticklen);
			    Shape shape = name_tl.getOutline(new_at);
			    g2.fill(shape);
			    
			    // and a glyph for good measure
			    int gly = cl.getGlyph();
			    Polygon poly = new Polygon(glyph_poly[gly].xpoints, 
						       glyph_poly[gly].ypoints,
						       glyph_poly[gly].npoints);
			    
			    poly.translate(xp+graph_w+ticklen, yp-ticklen);
			    graphic.setColor(cl.getColour());
			    graphic.fillPolygon(poly);
			
			    
			    xp += graph_sx;
			    
			    if(++col == n_cols)
			    {
				col = 0;
				yp += graph_sy;
				xp = xoff;
			    }
			}
		    }
		}

		deco_man.drawDecorations(graphic, width, height);
	    }
	    catch(Throwable th)
	    {
		th.printStackTrace();
	    }
	}

	private void drawElementsInto( Graphics graphic, ExprData.Cluster cl, int[] ids, int xp, int yp, int w, int h )
	{
	    if(show_mean)
	    {
		// calculate the mean....
		drawMeanElementsInto( graphic, cl, ids, false, false, xp, yp, w, h );
	    }
	    else
	    {
		if(show_spot_label.size() > 0)
		{
		    // draw the selected spots on top of (i.e. after) the unselected ones
		    
		    drawElementsInto( graphic, cl, ids, true, false, xp, yp, w, h );
		    
		    drawElementsInto( graphic, cl, ids, true, true, xp, yp, w, h );
		}
		else
		{
		    drawElementsInto( graphic, cl, ids, false, false, xp, yp, w, h );
		}
	    }
	}

	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	private int colour_alloc = 0;
	private Color[] sel_colours = null;

	private void drawElementsInto( Graphics graphic,  ExprData.Cluster cl, int[] ids, 
				       boolean do_sel, boolean is_sel, 
				       int xp, int yp, int w, int h )
	{
	    if((cl == null) || (cl.getIsSpot()))
	    {
		Graphics2D g2 = null;

		int spot_label_w = 0;
		int spot_label_h = 0;
		int spot_label_o = 0;
		TextLayout spot_label = null;

		for(int e=0; e < ids.length; e++)
		{
		    final int sid = ids[e];
		    final int sel_colour_id = getSpotLabel(sid);
		    final boolean spot_is_sel = (sel_colour_id >= 0);

		    if((do_sel == false) || (spot_is_sel == is_sel))
		    {
			boolean show_label = spot_is_sel;
			String label = spot_is_sel ? nt_sel.getNameTag(sid) : null;
			if((label == null) || (label.length() == 0))
			    show_label = false;
			
			if(show_label)
			{
			    spot_label = new TextLayout( label, font, frc);
			    spot_label_w = (int)spot_label.getBounds().getWidth();
			    spot_label_h = (int)spot_label.getBounds().getHeight();
			    spot_label_o = spot_label_w / 2;
			    
			}

			if((apply_filter == false) || (!edata.filter(sid)))
			{
			    int exp = xp;
			    int last_eyp = 0;
			    int last_exp = 0;
			    
			    Color draw_col = text_col;

			    if(do_sel)
			    {
				if(col_sel)
				{
				    //graphic.setColor( is_sel ? sel_colour : unsel_colour ); 
				    if(is_sel)
				    {
					draw_col = sel_colours[ sel_colour_id ];
				    }
				    else
					draw_col = unsel_colour; 
				}
				else
				{
				    draw_col = is_sel ? sel_colour : unsel_colour ; 
				}
			    }

			    graphic.setColor( draw_col );
			    if(g2 == null)
				g2 = (Graphics2D) graphic;

			    for(int m=0; m < meas_ids.length; m++)
			    {
				final double eval = edata.eValue(meas_ids[m], sid);
				int eyp = yp + h - (int)((eval - graph_y_axis_min) * graph_y_axis_scale);
			    
				
				// System.out.println("m=" + m + " s=" + ids[e] + " y=" + eyp);
				
				
				if(m > 0)
				{
				   if(root_profile_picker != null)
				       root_profile_picker.addSegment(last_exp, last_eyp, exp, eyp, sid);
				   
				   graphic.drawLine( last_exp, last_eyp, exp, eyp);
				}
				
				if(show_label)
				{
				    // put the label underneath when the value is rising
				    int label_y = eyp;
				    int label_x = exp-spot_label_o;

				    if((m+1) < meas_ids.length)
				    {
					if( eval < edata.eValue(meas_ids[m+1], sid) )
					    label_y += spot_label_h;
				    }
				    else
				    {
					// put the last label underneath when the value is falling
					if( eval < edata.eValue(meas_ids[m-1], sid) )
					    label_y += spot_label_h;
				    }

				    if(spaceForLabel( 18, spot_label_w, spot_label_h, label_x, label_y ))
				    {
					graphic.setColor( background_col );
					graphic.fillRect( label_x, label_y-spot_label_h, spot_label_w, spot_label_h);
					graphic.setColor( draw_col );

					AffineTransform new_at = new AffineTransform();
					new_at.translate(label_x, label_y);
					Shape shape = spot_label.getOutline(new_at);
					g2.fill(shape);
					
					storeLabelExtent( spot_label_w, spot_label_h, label_x, label_y );
				    }
				}

				last_exp = exp;
				last_eyp = eyp;
			    
				
				exp += graph_x_axis_step;
			    }
			}
		    }
		}
	    }
	}


	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	private void drawMeanElementsInto( Graphics graphic,  ExprData.Cluster cl, int[] ids, 
					   boolean do_sel, boolean is_sel, 
					   int xp, int yp, int w, int h )
	{
	    if((cl == null) || (cl.getIsSpot()))
	    {
		Graphics2D g2 = null;

		int spot_label_w = 0;
		int spot_label_h = 0;
		int spot_label_o = 0;
		TextLayout spot_label = null;

		final int n_spots = ids.length;
		final int n_meas  = meas_ids.length;
		
		if(n_spots == 0)
		    return;

		final double[] emean = new double[n_meas];
		final double[] emin = new double[n_meas];
		final double[] emax = new double[n_meas];

		for(int m=0; m < n_meas; m++)
		{
		    emin[m] = Double.MAX_VALUE; 
		    emax[m] = -Double.MAX_VALUE; 
		}
		
                // get mean, min and max for each Measurement
							  
		for(int s=0; s < n_spots; s++)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			final double eval = edata.eValue(meas_ids[m], ids[s]);
			if( eval > emax[m] )
			    emax[m] = eval;
			if( eval < emin[m] )
			    emin[m] = eval;
			emean[m] += eval;
		    }
		}
		for(int m=0; m < n_meas; m++)
		{
		    emean[m] /= (double) n_spots;
		}
		
		
		
		{
		    int exp = xp;
		    int last_eyp = 0;
		    int last_exp = 0;
		    
		    Color draw_col = text_col;
		    
		    graphic.setColor( draw_col );
		    
		    for(int m=0; m < n_meas; m++)
		    {
			int eyp = yp + h - (int)((emean[m] - graph_y_axis_min) * graph_y_axis_scale);

			// System.out.println("m=" + m + " s=" + ids[e] + " y=" + eyp);
			
			if(m > 0)
			{
			    graphic.drawLine( last_exp, last_eyp, exp, eyp);
			}
			
			int min_yp = yp + h - (int)((emin[m] - graph_y_axis_min) * graph_y_axis_scale);
			int max_yp = yp + h - (int)((emax[m] - graph_y_axis_min) * graph_y_axis_scale);
			
			// draw the error bars
			graphic.drawLine( exp, min_yp, exp, max_yp );
			graphic.drawLine( exp-1, min_yp, exp+1, min_yp );
			graphic.drawLine( exp-1, max_yp, exp+1, max_yp );
			
			// and update for the next meas
			last_exp = exp;
			last_eyp = eyp;
			
			exp += graph_x_axis_step;
		    }
		}
	    }
	}
	
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	
	private void drawAxes( Graphics graphic,  int xp, int yp, int w, int h, double min, double max )
	{
	    graphic.drawRect( xp, yp, w, h );
	    
	    Shape shape = null;
	    TextLayout tl = null;
	    
	    //int eyp = yp + h - (int)((edata.eValue(meas_ids[m], ids[e]) - graph_y_axis_min) * graph_y_axis_scale);
	    int miny = yp + h;
	    int maxy = yp;
	    int zeroy = yp + h - (int)((0 - graph_y_axis_min) * graph_y_axis_scale);
	    
	    graphic.drawLine( xp, miny, xp-ticklen, miny );
	    
	    if(uniform_scale)
	    {
		tl = min_label;
	    }
	    else
	    {
		//tl = min_label;
		tl = new TextLayout( mview.niceDouble( min, 9, 4 ), font, frc );
		min_label_o = (int)(tl.getBounds().getWidth() / 2.0);
	    }
	    AffineTransform new_at = new AffineTransform();
	    new_at.translate(xp-ticklen, miny+min_label_o);
	    new_at.rotate(Math.toRadians(-90), 0, 0);
	    shape = tl.getOutline(new_at);
	    Graphics2D g2 = (Graphics2D) graphic;
	    g2.fill(shape);
	    
	    if(uniform_scale)
	    {
		tl = max_label;
	    }
	    else
	    {
		//tl = max_label;
		tl = new TextLayout( mview.niceDouble( max, 9, 4 ), font, frc );
		max_label_o = (int)(tl.getBounds().getWidth() / 2.0);
	    }
	    new_at = new AffineTransform();
	    new_at.translate(xp-ticklen, maxy+max_label_o);
	    new_at.rotate(Math.toRadians(-90), 0, 0);
	    shape = tl.getOutline(new_at);
	    g2.fill(shape);
	    graphic.drawLine( xp, maxy, xp-ticklen, maxy );

	    if(draw_zero)
	    {
		//if(spaceForLabel( ticklen, (zero_label_o*2), xp-ticklen, zeroy+zero_label_o ))
		{
		    new_at = new AffineTransform();
		    new_at.translate(xp-ticklen, zeroy+zero_label_o);
		    new_at.rotate(Math.toRadians(-90), 0, 0);
		    shape = zero_label.getOutline(new_at);
		    g2.fill(shape);
		    graphic.drawLine( xp, zeroy, xp-ticklen, zeroy );
		    //storeLabelExtent( ticklen, (zero_label_o*2), xp-ticklen, zeroy+zero_label_o );
		}
	    }

	    // measurement ticks & labels 
	    int exp = xp;
	    int eyp = yp+h+ticklen;
	    int etyp = eyp+ticklen;

	    for(int m=0; m < meas_ids.length; m++)
	    {
		graphic.drawLine( exp, yp+h, exp, eyp );

		if(spaceForLabel( 18, meas_labels_o[m]*2, ticklen, exp-meas_labels_o[m], etyp ))
		{
		    new_at = new AffineTransform();
		    new_at.translate(exp-meas_labels_o[m], etyp);
		    shape = meas_labels[m].getOutline(new_at);
		    g2.fill(shape);
		    storeLabelExtent( meas_labels_o[m]*2, ticklen, exp-meas_labels_o[m], etyp );
		}
		
		exp += graph_x_axis_step;
	    }
	}

	// drawing with selection
	private Color sel_colour, unsel_colour;
	private boolean coloured = false;
	
	
    }
	    
    // ================================================================================
    // ===== spot picker ======================
    // ================================================================================
    //
    //  stores and retrieves ids based on their positions
    //   uses a simle quadtree subdivision of the space
    //
    //   (used to accelerate for mouse tracking)
    //
    //   (shared with HyperCubePlot)
    //
    // ================================================================================

    class SpotPickerNode
    {
	final int max_spots_per_box = 64;
	
	// each node is either a split point or a spot container
	
	boolean is_split;
	
	// things for split node
	SpotPickerNode[] children;
	int x_split, y_split;
	
	// things for container node
	int entries;
	
	int x, y;
	int w, h;
	
	int[] px;
	int[] py;
	int[] id;
	
	// construct a container
	public SpotPickerNode(int x_, int y_, int w_, int h_)
	{
	    x = x_; 
	    y = y_;
	    w = w_;
	    h = h_;

	    is_split = false;
	    
	    px = new int[max_spots_per_box];
	    py = new int[max_spots_per_box];
	    id = new int[max_spots_per_box];
	    
	    //System.out.println("created node[" + w + "x" + h + " @ " + x + "," + y + "]");

	    entries = 0;
	}
	
	public int findSpot(int sx, int sy, int range)
	{
	
	    //System.out.println("checking " + sx + "," + sy +" in node[" + 
	    //	       w + "x" + h + " @ " + x + "," + y + "]");

	    if(is_split)
	    {
		// delegate the search to the correct child
		int child = 0;
		if(sx >= x_split)
		    child += 1;
		if(sy >= y_split)
		    child += 2;
		return children[child].findSpot(sx, sy, range);
	    }
	    else
	    {
		// search these entries
		int best_id = -1;
		int best_dist_sq = (range * range) + 1;
		for(int e=0; e < entries; e++)
		{
		    int dist_sq = ((px[e] - sx) * (px[e] - sx)) + ((py[e] - sy) * (py[e] - sy));
		    if(dist_sq < best_dist_sq)
		    {
			best_dist_sq = dist_sq;
			best_id = id[e];
		    }
		}
		return best_id;
	    }
	}
	
	public void storeSpot(int sx, int sy, int sid)
	{
	    if(is_split)
	    {
		// delegate the storing to the correct child
		int child = 0;
		if(sx >= x_split)
		    child += 1;
		if(sy >= y_split)
		    child += 2;
		children[child].storeSpot(sx, sy, sid);
	    }
	    else
	    {
		if(entries == max_spots_per_box)
		{
		    // this container is full,  convert it into a split point
		    
		    if((w < 2) && (h < 2))
		    {
			// too small for splitting, ignore this spot

			// System.out.println("  container is full, but too small to split....");
		    }
		    else
		    {
			//System.out.println("  splitting node[" + w + "x" + h + 
			//		   " @ " + x + "," + y + ":" + entries + "]");
			
			
			// convert it into a split point and distribute the children
			//
			splitNode();

			// and try the storeSpot metho again
			storeSpot(sx, sy, sid);
		    }
		}
		else
		{
		    // store this entry
		    px[entries] = sx;
		    py[entries] = sy;
		    id[entries] = sid;
		    entries++;

		    /*
		    System.out.println(sid + " @ " + sx + "," + sy + 
				       " stored in node[" + w + "x" + h + 
				       " @ " + x + "," + y + ":" + entries + "]");
		    */

		}
	    }
	}
	
	private void splitNode()
	{
	    is_split = true;
	    
	    // pick x_split and y_split
	    
	    x_split = (w / 2);     // relative to 0,0
	    y_split = (h / 2);
	    
	    int w_1 = x_split;
	    int w_2 = w-x_split;
	    int h_1 = y_split;
	    int h_2 = h-y_split;

	    if(w == 1)
		x_split = 0;
	    if(h == 1)
		y_split = 0;
	    
	    x_split += x;          // make relative to x,y
	    y_split += y;

	    // create the children
	    
	    children = new SpotPickerNode[4];
	    
	    children[0] = new SpotPickerNode(x, y, w_1, h_1);
	    children[1] = new SpotPickerNode(x_split, y, w_2, h_1);
	    children[2] = new SpotPickerNode(x, y_split, w_1, h_2);
	    children[3] = new SpotPickerNode(x_split, y_split, w_2, h_2);
	    
	    // distribute the entries of this node to the children
	    
	    for(int e=0; e < entries; e++)
	    {
		int child = 0;
		if(px[e] >= x_split)
		    child += 1;
		if(py[e] >= y_split)
		    child += 2;
		children[child].storeSpot(px[e], py[e], id[e]);
	    }
	    
	    // free up the now unused arrays of this node
	    
	    px = py = id = null;
	    entries = 0;
	}

	public void dumpStats(final String pad)
	{
	    if(is_split)
	    {
		final String ipad = " " + pad;
		for(int c=0; c < 4; c++)
		    children[c].dumpStats(ipad);
	    }
	    else
	    {
		System.out.println(pad + " [" + 
				   w + "x" + h + " @ " + 
				   x + "," + y + ":" + 
				   entries + "]");
	    }
	}

	public void drawNode(Graphics g)
	{

	    g.setColor(Color.white);
	    
	    if(is_split)
	    {
		for(int c=0; c < 4; c++)
		    children[c].drawNode(g);
	    }
	    else
		g.drawRect(x, y, w-1, h-1);
	}
	
    }

    // ================================================================================
    // ===== spot picker II ======================
    // ================================================================================

  
    public class LineSeg
    {
	public LineSeg( int sx_, int sy_, int ex_, int ey_, int id_ )
	{
	    sx = sx_; sy = sy_; ex = ex_; ey = ey_; id = id_;

	    m_inf = (sx == ex);

	    if(!m_inf)
	    {
		m = (double)( ey-sy ) / (double)( ex-sx );
		c = ((double) sy) - (m * ((double) sx));

		m_intersect = 1.0 / m;
	    }
	}

	int sx, sy, ex, ey, id;
	double m, c;
	boolean m_inf;    // vertical lines have an infinite slope

	double m_intersect;

	public int distanceFrom( int x, int y )
	{
	    if(m_inf)
	    {
		return (x - sx) * (x - sx);
	    }
	    else
	    {
		// find the equation of the line that 
		//  (a) is perpendicular to this segment
		//  (b) passes through (x,y)
		//
		
		// slope is known: m_intersect
		
		double c_intersect = ((double) y) - (m_intersect * ((double) x));

		// from:
		//
		//   1.this segment:    y1 = m1 x1 + c1;
		//   2.the intersect:   y2 = m2 x2 + c2;
		//
		//   the intersection point is (p,q)
		//
		//   y1, x1, y2, x2, m1, m2 and c1 are known
		//
		//   solving intersect for p: 
		//      m1 p + c1 = m2 p + c2
		//      p = (c2 - c1) / (m1 - m2);
		//
		
		double p = (c_intersect - c) / (m - m_intersect);

		double q = (m * p) + c;

		int pi = (int) p;
		int qi = (int) q;

		return ((x-pi) * (x-pi)) + ((y-qi) * (y-qi));
	    }
	}
    }


    public class ProfilePicker
    {
	private final int map_x = 20;
	private final int map_y = 20;
	
	private double scale_x = 1.0;
	private double scale_y = 1.0;
	
	private Vector[][] label_map;

	public ProfilePicker()
	{
	    label_map = new Vector[map_x][map_y];
	}

	public void addSegment( int sx, int sy, int ex, int ey, int spot_id )
	{
	    LineSeg ls = new LineSeg( sx, sy, ex, ey, spot_id );
	    
	    // rasterise this segment into the bins

	    if(sx > ex)
	    {
		int tmp = sx; sx = ex; ex = tmp;
	    }
	    if(sy > ey)
	    {
		int tmp = sy; sy = ey; ey = tmp;
	    }
	    
	    int msx = (int)((double) sx * scale_x);
	    int msy = (int)((double) sy * scale_y);
	
	    int mex = (int)((double) ex * scale_x);
	    int mey = (int)((double) ey * scale_y);

	    if(msx < 0)
		msx = 0;
	    if(mex < 0)
		mex = 0;
	    if(msx < 0)
		msx = 0;

	    if(mey < 0)
		mex = 0;
	    if(msy < 0)
		msy = 0;
	    if(mey < 0)
		mey = 0;

	    if(msx >= map_x)
		msx = map_x-1;
	    if(mex >= map_x)
		mex = map_x-1;

	    if(msy >= map_y)
		msy = map_y-1;
	    if(mey >= map_y)
		mey = map_y-1;
	    
	    for(int mx=msx; mx <= mex; mx++)
		for(int my=msy; my <= mey; my++)
		{
		    Vector vec = label_map[mx][my];
		    if(vec == null)
			vec = (label_map[mx][my] = new Vector());
		    vec.addElement( ls );
		}
	}

	public int findProfile( int x, int y )
	{
	    
	    
	    int dist;
	    int best_id = -1;
	    int min_dist = Integer.MAX_VALUE;

	    // which bin is the pointer in?
	    int mx = (int)((double) x * scale_x);
	    int my = (int)((double) y * scale_y);
	    
	    if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	    {
		// find the segment in this bin that is nearest to the pointer
		Vector vec = label_map[mx][my];
		if(vec != null)
		{
		    for(int seg=0; seg < vec.size(); seg++)
		    {
			LineSeg ls = (LineSeg) vec.elementAt(seg);
			dist = ls.distanceFrom( x, y );
			if(dist < min_dist)
			{
			    min_dist = dist;
			    best_id = ls.id;
			}
		    }
		}
	    }
	    return best_id;
	}

	public void setupPicker( int w, int h )
	{
	    // width = w; height = h;
	    scale_x = (double) map_x / (double) w;
	    scale_y = (double) map_y / (double) h;
	    
	    for(int x=0; x < map_x; x++)
		for(int y=0; y < map_y; y++)
		    if(label_map[x][y] != null)
			label_map[x][y].clear();
	}
    }

    

    // ============================================================================
    // ============================================================================
    // spot labels
    // ============================================================================

    private Hashtable show_spot_label = new Hashtable();
    private int cycled_data = 0;
    final static int n_sel_colours = 8;

    private Integer nextData()
    {
	int res = cycled_data++;
	if(cycled_data >=  n_sel_colours)
	    cycled_data = 0;

	return new Integer( res );
    }

    // -1 if not found, >=0 otherwise
    public int getSpotLabel(int id)
    {
	Integer res = (Integer) show_spot_label.get(new Integer(id));
	if(res != null)
	    return res.intValue();
	else
	    return -1;
    }

    public void showSpotLabel(int id)
    {
	Integer key = new Integer(id);
	if(show_spot_label.get(key) == null)
	{
	    show_spot_label.put( key, nextData() );
	}
    }

    public void clearAllSpotLabels()
    {
	show_spot_label = new Hashtable();
    }

    public void toggleSpotLabel(int id)
    {
	Integer i = new Integer(id);
	
	if(show_spot_label.get(i) == null)
	{
	    show_spot_label.put(i, nextData() );
	    //System.out.println(id + " added");
	}
	else
	{
	    show_spot_label.remove(i);
	    //System.out.println(id + " removed");
	}
	
	pro_panel.repaint();
    }

    public void setSpotLabel(ExprData.Cluster cl)
    {
	if(cl.getIsSpot())
	{
	    final int[] els = cl.getElements();
	    if(els != null)
		for(int e=0; e < els.length; e++)
		    showSpotLabel(els[e]);
	}
    }   
   
    public void setAllSpotLabels()
    {
	if( picker_pick_spots )
	{
	    for(int s=0; s < sel_spot_ids.length; s++)
	    {
		showSpotLabel( sel_spot_ids[s] );
	    }
	}
	else
	{
	    for(int s=0; s < sel_cls.size(); s++)
	    {
		ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);
		int[] ids = include_children ? cl.getAllClusterElements() :  cl.getElements();
		if(ids != null)
		for(int e=0; e < ids.length; e++)
		    showSpotLabel(ids[e]);
	    }
	}
    }

 

    // ============================================================================
    // ============================================================================
    //
    // label space allocator
    //
    // ============================================================================
    // ============================================================================

    private void storeLabelExtent( int lw, int lh, int lx, int ly )
    {
	int mx = (int)((double) lx * scale_x);
	int my = (int)((double) ly * scale_y);

	//System.out.println("storeLabelExtent: " + lw + "x" + lh + " @ " + lx + "," + ly + " ... " + mx + "," + my);

	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    if(label_map[mx][my] == null)
		label_map[mx][my] = new Vector();
	    label_map[mx][my].addElement(new LabelExtent( lw, lh, lx, ly ));
	}
    }
    
    // returns true if the specified label will fit into the map without overlapping anything
    //
    private boolean spaceForLabel( int scale ,int lw, int lh, int lx, int ly )
    {
	if(!auto_space)
	    return true;
	
	int mx = (int)((double) lx * scale_x);
	int my = (int)((double) ly * scale_y);

	//System.out.println("spaceForLabel: " + lw + "x" + lh + " @ " + lx + "," + ly + " ... " + mx + "," + my);

	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    //System.out.println("  spaceForLabel: searching from " + my);

	    int tp = my;
	    int bp = my + 1;

	    while((tp >= 0) || (bp < map_y))
	    {
		if(tp >= 0)
		{
		    if(searchMapLine( scale, mx, tp, lw, lh, lx, ly ))
			return false;
		    tp--;
		}
		if(bp < map_y)
		{
		    if(searchMapLine( scale, mx, bp, lw, lh, lx, ly ))
			return false;
		    bp++;
		}
	    }
	    
	    //System.out.println("  spaceForLabel: onscreen and there is space!");
	    return true;
	}
	return false;
    }
    
    // returns true if the specified label will overlap a label in this cell of the map
    //
    private boolean searchMapCell( int scale, int col, int row, int lw, int lh, int lx, int ly )
    {
	Vector lm = label_map[col][row];
	if(lm == null)
	    return false;

	for(int le=0; le < lm.size(); le++)
	{
	    LabelExtent lex = (LabelExtent) lm.elementAt(le);
	    if(labelsOverlap( scale, lw, lh, lx, ly, lex.lw, lex.lh, lex.lx, lex.ly ))
		return true;
	}
	return false;
    }

    // returns true if the specified label will overlap a label in the map on this line
    //
    private boolean searchMapLine( int scale, int col, int row, int lw, int lh, int lx, int ly )
    {
	//System.out.println("  searchMapLine:  col=" + col  + " row=" + row);

	int lp = col;
	int rp = col + 1;
	while((lp >= 0) || (rp < map_x))
	{
	    if(lp >= 0)
	    {
		if(searchMapCell( scale, lp, row, lw, lh, lx, ly ))
		    return true;
		lp--;
	    }
	    if(rp < map_x)
	    {
		if(searchMapCell( scale, rp, row, lw, lh, lx, ly ))
		    return true;
		rp++;
	    }
	}
	//System.out.println("  searchMapLine: not found");

	return false;
    }

    private void resetLabelMap(int w, int h)
    {
	// width = w; height = h;
	scale_x = (double) map_x / (double) w;
	scale_y = (double) map_y / (double) h;

	for(int x=0; x < map_x; x++)
	    for(int y=0; y < map_y; y++)
		if(label_map[x][y] != null)
		    label_map[x][y].clear();
    }

    private double scale_x = 1.0;
    private double scale_y = 1.0;

    private final int map_x = 10;
    private final int map_y = 10;

    private Vector[][] label_map = new Vector[map_x][map_y];
    
    private class LabelExtent
    {
	public int lw, lh, lx, ly;
	public LabelExtent(int lw_, int lh_, int lx_, int ly_)
	{
	    lw=lw_; lh=lh_; lx=lx_; ly=ly_;
	}
    }

    // scale should be a number in the range 16...32
    // (higher numbers give a large overlap)
    //
    private boolean labelsOverlap( int scale, 
				   int lw1, int lh1, int lx1, int ly1,
				   int lw2, int lh2, int lx2, int ly2 )
    {
	final int hlw1 = (lw1 * scale) / 32;
	final int hlh1 = (lh1 * scale) / 32;
	
	final int hlw2 = (lw2 * scale) / 32;
	final int hlh2 = (lh2 * scale) / 32;

	/*
	  final int hlw1 = lw1 / 2;
	  final int hlh1 = lh1 / 2;
	  
	  final int hlw2 = lw2 / 2;
	  final int hlh2 = lh2 / 2;
	*/

	final int tlx1 = lx1 - hlw1;
	final int tly1 = ly1 - hlh1;
	final int brx1 = lx1 + hlw1;
	final int bry1 = ly1 + hlh1;
	
	final int tlx2 = lx2 - hlw2;
	final int tly2 = ly2 - hlh2;
	final int brx2 = lx2 + hlw2;
	final int bry2 = ly2 + hlh2;
	
	// check the corners
	boolean h_ok = (((tlx1 >= tlx2) && (tlx1 <= brx2)) || ((brx1 >= tlx2) && (brx1 <= brx2)));
	boolean v_ok = (((tly1 >= tly2) && (tly1 <= bry2)) || ((bry1 >= tly2) && (bry1 <= bry2)));
	
	// check for containment
	if(!h_ok)
	    h_ok = ((tlx1 < tlx2) && (brx1 > brx2));
	if(!v_ok)
	    v_ok = ((tly1 < tly2) && (bry1 > bry2));
	
	return (h_ok && v_ok);
    }

    // ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 


    // ============================================================================
    // ============================================================================
    //
    //  history list
    //
    // ============================================================================
    // ============================================================================

    private void goBack()
    {
	System.out.println("goBack(): " + history_v.size() + " things in history");

	if(history_v.size() == 0)
	    return;


	// history_v.removeElementAt( 0 );

	Vector back = (Vector) history_v.elementAt( 0 );
	history_v.removeElementAt( 0 );

	//System.out.println("goBack(): got old selection with size=" + back.size());
	
	Hashtable cl_ht = new Hashtable();

	for(int c=0; c < back.size(); c++)
	    cl_ht.put( back.elementAt(c), back.elementAt(c));

	enable_stores = false;

	selectClustersInTree( cl_tree, cl_ht );

	//System.out.println("goBack(): done");

	enable_stores = true;
    }

    private void goForward()
    {
	System.out.println("goForward(): " + history_v.size() + " things in history");

    }

    private void storePosition()
    {
	if(enable_stores)
	{
	    //System.out.println("storePosition(): saving, sel size=" + sel_cls.size() );
	    
	    history_v.insertElementAt( sel_cls.clone(), 0 );

	    //System.out.println("storePosition(): " + history_v.size() + " things in history");
	    
	    /*
	    System.out.print("  history: ");
	    for(int h=0; h < history_v.size(); h++)
	    {
		Vector back = (Vector) history_v.elementAt( h );
		System.out.print( " [");
		for(int b=0; b < back.size(); b++)
		    System.out.print( ((ExprData.Cluster)back.elementAt(b)).getName() + " ");
		System.out.print( "] " );
	    }
	    System.out.println();
	    */
	}
    }

    private Vector history_v = new Vector(); 
    private int pos = -1;
    private boolean enable_stores = true;

    // ============================================================================
    // ============================================================================
    //
    //  doings
    //
    // ============================================================================
    // ============================================================================

    private TextLayout min_label, zero_label, max_label;
    private TextLayout[] meas_labels;
    private int  min_label_o, zero_label_o, max_label_o;
    private int[] meas_labels_o;
    private Font font;
    private FontRenderContext frc;

    private double spot_font_scale = 1.0;

    private boolean draw_zero = false;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private JFrame frame;

    private ProfilePanel pro_panel;
    private DragAndDropList meas_list;

    private boolean auto_space;
    private boolean col_sel;
    private boolean uniform_scale;
    private boolean show_mean;

    private JCheckBox apply_filter_jchkb;
    private JCheckBox show_mean_jchkb;
    private JCheckBox uniform_scale_jchkb;
    private JCheckBox include_children_jchkb;

    private JPanel pickwrap;  // can contain either a spot list or a cluster tree
    private GridBagLayout pickbag;
	    
    private JPanel spot_picker_panel;
    private JPanel cluster_picker_panel;

    private JComponent pickwrap_parent;

    private boolean picker_pick_spots = false;

    private DragAndDropTree cl_tree;
    private DragAndDropList spot_list;
    private NameTagSelector spot_picker_nts;
    

    private ExprData.NameTagSelection nt_sel;
    private NameTagSelector nts;

    private Vector sel_cls;       // the selected clusters
    private Vector sel_cl_ids;    // the element_ids for the selected clusters

    private int[] sel_spot_ids = null;
    private Hashtable sel_spot_back_map = null;
    private Hashtable sel_spot_name_map = null;

    private int[]  meas_ids;

    private boolean apply_filter;
    private boolean include_children;

    private int n_cols;
    private int n_rows;

    private int graph_w;
    private int graph_h;

    private int ticklen;

    private int graph_sx;   // step size between graphs
    private int graph_sy;

    private int xoff, yoff;

    private int graph_x_axis_step;
    private int graph_y_axis_step;

    private double graph_y_axis_scale;
    private double graph_y_axis_min;
    private double graph_y_axis_max;

    private Color background_col;
    private Color text_col;

    // private SpotPickerNode root_spot_picker;

    private ProfilePicker root_profile_picker;

    private AxisManager axis_man;
    private DecorationManager deco_man;

    private int sel_handle;
}
