import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import javax.swing.tree.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.awt.font.*;
import java.awt.geom.*;
import java.awt.dnd.*;

public class MeasGroupPicker extends JPanel
{
    public Selection getSelection() 
    { 
	return selection; 
    }

    
    public MeasGroupPicker( final maxdView mview, final ExprData edata, final NewProfileViewer viewer )
    {
	this.mview = mview;
	this.edata = edata;
	this.viewer = viewer;


	GridBagLayout main_bag = new GridBagLayout();
	setLayout( main_bag );
	GridBagConstraints c;


	// ========================================================================================
	//
	// controls for selecting either Individuals or Clusters of Measurements
	//
	// ========================================================================================

	
	JPanel pickwrap = new JPanel();
	GridBagLayout pickbag = new GridBagLayout();
	pickwrap.setLayout(pickbag);

	ButtonGroup bg = new ButtonGroup();
	



	// ========================================================================================
	//
	// the Measurement List
	//
	// ========================================================================================

	meas_list = new DragAndDropList();

	populateListWithGroupingAttributes(meas_list);

	meas_list.addListSelectionListener(new ListSelectionListener() 
	    {
		public void valueChanged(ListSelectionEvent e)
		{
		    listSelectionHasChanged();
		}
	    });

	meas_list_scrollpane = new JScrollPane( meas_list );

	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.BOTH;
	main_bag.setConstraints( meas_list_scrollpane, c);
	add( meas_list_scrollpane );


    }
    // ========================================================================================
    //
    // handlers for the Measurement List
    //
    // ========================================================================================


    private void populateListWithGroupingAttributes(JList list)
    {
	/*
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
	*/


	/*
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
	*/
		
    }

    private void listSelectionHasChanged()
    {
	
    }


    // ========================================================================================
    //
    // state
    //
    // ========================================================================================


    private maxdView mview;
    private ExprData edata;
    private NewProfileViewer viewer;

    private Selection selection = null;

    private DragAndDropList meas_list;

    private JScrollPane meas_list_scrollpane;

}