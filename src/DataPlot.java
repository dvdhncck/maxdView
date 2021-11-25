import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.*;
import java.awt.image.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class DataPlot extends JPanel implements ExprData.ExprDataObserver, PrintManager.PrintRequestListener
{ 
    private String welcome = null; 

    // ---------------- --------------- --------------- ------------- ------------

    public DataPlot(maxdView maxd_view_, ExprData expr_data_, boolean first_one_)
    { 
	super();
	edata = expr_data_;
	mview = maxd_view_;
	
	final CompilationInfo ci = new CompilationInfo();

	welcome = "   Welcome to " + mview.getApplicationTitle() + "   -   " + 
	"released March 2005" + "   -   " +
	"updated in this version: Printing, filters, Scatter Plots, QC Plugin and more..." + "   -   " + 
	"compiled on " + ci.compile_time + " with " + ci.compiler + "  on " + ci.compile_host + "  -   ";
	
	logo_ii = new ImageIcon(mview.getImageDirectory() + "maxdView.jpg");

	first_one = first_one_;

	custom_menu = mview.getCustomMenu();
	

	String[] sys_fonts =  GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	
	font_family_names = new String[ sys_fonts.length + 3 ];
	
	for(int f=0; f < sys_fonts.length; f++)
	   font_family_names[f+3] = sys_fonts[f];

	font_family_names[0] = "Helvetica";
	font_family_names[1] = "Courier";
	font_family_names[2] = "Times";

	// default name tags...
	n_name_cols = 0;
 	addNameCol();
		
	if(first_one)
	    initialiseColours();

	text_col       = mview.getTextColour();
	background_col = mview.getBackgroundColour();

	GridBagLayout gridbag = new GridBagLayout();
	setLayout(gridbag);

	setPreferredSize(new Dimension(600, 300));
	
	{ 
	    dplot_panel = new DataPlotPanel(this);


	    //dplot_panel.setToolTipText("hello there...");

	    //dplot_panel.addKeyListener(new CustomKeyListener());

	    ToolTipManager ttm = ToolTipManager.sharedInstance();

	    //ttm.setInitialDelay(250);
	    //ttm.setDismissDelay(50);
	    //ttm.setReshowDelay(50);

	    add(dplot_panel);
	    dplot_panel.setPreferredSize(new Dimension(600, 300));
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(dplot_panel, c);
	}
	{ 
	   vert_sb = new JScrollBar();
	   add(vert_sb);
	   
	   vert_sb.addAdjustmentListener(new AdjustmentListener()
		  {
		     public void adjustmentValueChanged(AdjustmentEvent e)
		     {
			 current_ver_sb_value = vert_sb.getValue();
			 repaint();
		     }
		  });

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 1;
	   c.gridy = 0;
	   c.fill = GridBagConstraints.VERTICAL;
	   gridbag.setConstraints(vert_sb, c);
	}
	{ 
	   hor_sb = new JScrollBar(JScrollBar.HORIZONTAL);
	   add(hor_sb);
	   
	   hor_sb.addAdjustmentListener(new AdjustmentListener()
		  {
		     public void adjustmentValueChanged(AdjustmentEvent e)
		     {
			 current_hor_sb_value = hor_sb.getValue();
			 repaint();
		     }
		  });

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 1;
	   c.fill = GridBagConstraints.HORIZONTAL;
	   gridbag.setConstraints(hor_sb, c);
	}
	
	edata.addObserver(this);

	updateDisplay();
    }

    public void closeDown()
    {
	edata.removeObserver(this);
	if(logo_ticker != null)
	    logo_ticker.stop();
    }

    // ---------------- --------------- --------------- ------------- ------------

    // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.ValuesChanged:
	case ExprData.RangeChanged:
	    updateColourisers();
	    updateDisplay();
	    break;

	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	    updateDisplay();
	    break;

	case ExprData.SizeChanged:       // a filter has changed
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	    updateFilterStatus();
	    findLongestNames();
	    updateDisplay();
	    break;

	case ExprData.VisibilityChanged:
	    updateDisplay();
	    break;

	case ExprData.VisibleNameAttrsChanged:
	    findLongestNames();
	    updateDisplay();
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.VisibilityChanged:
	case ExprData.OrderChanged:
	    updateDisplay();
	    break;
	case ExprData.NameChanged:
	case ExprData.ColourChanged:
	    repaint();
	    break;
	}
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.RangeChanged:
	    edata.generateDataUpdate(ExprData.ColourChanged);
	    break;
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.VisibilityChanged:
	case ExprData.OrderChanged:
	case ExprData.ValuesChanged:
	    mapColourisersToMeasurements();
	    updateDisplay();
	    break;
	case ExprData.NameChanged:
	case ExprData.ColourChanged:
	    repaint();
	    break;
	}
    }


    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	text_col       = mview.getTextColour();
	background_col = mview.getBackgroundColour();

	repaint();
    }
    
   // ---------------- --------------- --------------- ------------- ------------

    public void printDisplay()
    {
	new PrintManager( mview, null, null ).openPrintDialog( this );
    }
    
    // ------------------------------------------------------------------------------------------
    //
    // doPrint() implements the PrintManager's PrintRequestListener interface
    //
    // this method is called by the PrintManager once the user has made the
    // relevant selections in the printer setup dialog
    //
    // ------------------------------------------------------------------------------------------
    
    public void doPrint( final PrintManager.PrintInfo pi )
    {
	System.out.println( "doPrint() called from PrintManager" );
	
	
	if( pi.print_mode == PrintManager.PRINT_TO_PRINTER )
	{
	    
	    //printToPrinter(Graphics g, PageFormat pf, int pg_num);
	    
	     
	    if(dplot_panel.getWidth() > dplot_panel.getHeight())
	    {
		pi.page_format.setOrientation(PageFormat.LANDSCAPE);
	    }
	    else
	    {
		pi.page_format.setOrientation(PageFormat.PORTRAIT);
	    }
	    
	    pi.printer_job.setPrintable( dplot_panel, pi.page_format );
	    
	    if( pi.printer_job.printDialog() == false )
		return;
	    
	    
	    ProgressOMeter pm = new ProgressOMeter("Printing", 3);
	    pm.startIt();
	    pm.setMessage(1,"(don't touch anything...)");
		
	    dplot_panel.new PrinterThread( pi.printer_job, pm ).start();
	}
	else
	{
	    int w = first_line_leftoff + (n_visible_meas * colskip) + spot_cluster_w;
	    int h = first_line_topoff + ( edata.getNumSpots() * rowskip);
	    
	    System.out.println(" total image is " + w + "x" + h);
	    
	    if((w * h) > (5 * 1024 * 1204))
	    {
		if(mview.infoQuestion("Image will be " + w + "x" + h + " pixels,\n"+
				      "the file will be very large.\nContinue?",
				      "Yes", "No") == 1)
		{
		    return;
		}
	    }
	    
	    
	    int old_scroll_mode = scroll_mode;
	    scroll_mode = 0;  // unlock both names and clusters
	    updateDisplay();
	    
	    ProgressOMeter pm = new ProgressOMeter("Saving");
	    //pm.startIt();

	    //dplot_panel.new ImageSaveThread( pi, pm, old_scroll_mode).start();
	    dplot_panel.doImageSave( pi, pm, old_scroll_mode );
	}
    }


    // ---------------- --------------- --------------- ------------- ------------

    // this should be here, not in class maxdView
    public Color getTextColour() { return mview.getTextColour(); }
    public Color getBackgroundColour() { return mview.getBackgroundColour(); }

    public Polygon generateScaledPolygon(double scale, double[] pts_x, double[] pts_y)
    {
	Polygon p = new Polygon();
	
	for(int v=0; v < pts_x.length; v++)
	    p.addPoint((int)(pts_x[v] * scale), (int)(pts_y[v] * scale));
	
	return p;
    }
    
    // scale the glyph polygons to the current box_height
    //
    public Polygon[] getScaledClusterGlyphs(int height)
    {
	 final double[] box_pts_x = { .0, .0, 1.0, 1.0 };
	 final double[] box_pts_y = { .0, 1.0, 1.0, .0, .0 };
	
	 final double[] tri_pts_x = { 0.5, .0, 1.0 };
	 final double[] tri_pts_y = { 0.0, 1.0, 1.0 };
	 
	 final double[] plus_pts_x = { .333, .333, .0, .0, .333, .333, .667, .667, 1.0, 1.0, .667, .667 };
	 final double[] plus_pts_y = { .0, .333, .333, .667, .667, 1.0, 1.0, .667, .667, .333, .333, .0 };
	 
	 final double[] diamond_pts_x = { .5, .0, .5, 1.0 };
	 final double[] diamond_pts_y = { .0, .5, 1.0, .5 };

	 final double[] cross_pts_x = { .25, .0, .25, .0, .25, .5, .75, 1.0, .75, 1.0, .75, 0.5 };
	 final double[] cross_pts_y = {  .0, .25, .5, .75, 1.0, .75, 1.0, .75, .5, .25, .0, .25 };

	 final double[] i_tri_pts_x = { .0, 0.5, 1.0 };
	 final double[] i_tri_pts_y = { .0, 1.0, .0 };

	 final double[] oct_pts_x = { .333, .0, .0, .333, .667, 1.0, 1.0, .667 };
	 final double[] oct_pts_y = { .0, .333, .667, 1.0, 1.0, .667, .333, .0 };
	 
	 final double[] hglass_pts_x = { .0, 0.5, .0, 1., 0.5, 1. };
	 final double[] hglass_pts_y = { .0, 0.5, 1., 1., 0.5, .0 };

	 final double[] shield_pts_x = { .25, .0, .5, 1.0, .75 };
	 final double[] shield_pts_y = { .0, .25, 1.0, .25, .0 };

	 Polygon[] glyph_poly = new Polygon[edata.n_glyph_types];

	 double s = (double) height;

	 glyph_poly[0] = generateScaledPolygon(s, shield_pts_x, shield_pts_y);
	 glyph_poly[1] = generateScaledPolygon(s, tri_pts_x, tri_pts_y);
	 glyph_poly[2] = generateScaledPolygon(s, plus_pts_x, plus_pts_y);
	 glyph_poly[3] = generateScaledPolygon(s, diamond_pts_x, diamond_pts_y);
	 glyph_poly[4] = generateScaledPolygon(s, cross_pts_x, cross_pts_y);
	 glyph_poly[5] = generateScaledPolygon(s, i_tri_pts_x, i_tri_pts_y);
	 glyph_poly[6] = generateScaledPolygon(s, oct_pts_x, oct_pts_y);
	 glyph_poly[7] = generateScaledPolygon(s, hglass_pts_x, hglass_pts_y);
	 
	 return glyph_poly;
    }

    // ---------------- --------------- --------------- ------------- ------------

    public void setTopPosition(int new_top)
    {
	top_position = new_top;
	repaint();
    }
    private int top_position = 0;

    // frac == 0.0 --> top
    // frac == 1.0 --> bottom
    // 
    public void positionDisplayAtFrac(double val)
    {
	vert_sb.setValue((int)((double)vert_sb.getMaximum() * val));
    }

    // anmt == 0 --> one line
    // amnt == 1 --> one page
    //
    public void scrollDisplayVert(int amnt, boolean down)
    {
	int pixel_d = dplot_panel.getHeight();

	int cur_index = min_vis_spot;

	int moved = 0;

	int moves = (amnt == 0 ? 1 : spots_per_page);
	
	// System.out.println("moving " + moves + " lines....");

	if(down)
	{
	    while(((cur_index+1) < edata.getNumSpots()) && (moved < moves))
	    {
		cur_index++;
		if(!edata.filter(edata.getSpotAtIndex(cur_index)))
		    moved++;
	    }
	}
	else
	{
	    while((cur_index > 0) && (moved < moves))
	    {
		cur_index--;
		if(!edata.filter(edata.getSpotAtIndex(cur_index)))
		    moved++;
	    }
	}

	int height = first_line_topoff + (cur_index * rowskip);
	
	vert_sb.setValue(height);
     }

    // anmt == 0 --> one col
    //
    public void scrollDisplayHor(int amnt, boolean down)
    {
	int cur = hor_sb.getValue();
	if(down)
	    cur--;
	else
	    cur++;

	hor_sb.setValue( cur);
     }

    // expects 'proper' Spot Id's
    //
    public void displaySpot(int s_id)
    {
	//	System.out.println("displaying spot id " + s_id + " which is in row " + edata.getIndexOf(s_id));

	if(apply_filter && edata.filter(s_id))
	{
	    System.out.println("displaySpot(): spot is hidden by current filter");
	    return;
	}
	
	// work out where the scroll bar thumb should be to make this row visible...
	// (but what about filtering ? - that's ok because the scroll bar ignores
	//  the filter so it's always the operating on the full length of the data)

	int height = first_line_topoff + (edata.getIndexOf(s_id) * rowskip);

	vert_sb.setValue(height);
    }


     // expects 'proper' Meas Id's 
    //
    public void displayMeasurement(int m_id)
    {
	//	System.out.println("displaying spot id " + s_id + " which is in row " + edata.getIndexOf(s_id));
	
	if( edata.getMeasurementShow(m_id) == false)
	{
	    System.out.println("displayMeasurement(): Measurement is hidden");
	    return;
	}
	
	// work out where the scroll bar thumb should be to make this col visible...
	
	int min_p = edata.getIndexOfMeasurement( m_id );

	
	min_vis_meas = min_p - (n_possible_meas / 2);

	if(min_vis_meas < 0)
	    min_vis_meas = 0;
	
	/*
	if(min_vis_meas < n_possible_meas)
	    min_vis_meas = 0;
	*/

	// System.out.println("displayMeasurement(): min_p=" + min_p + " mvm=" + min_vis_meas);

	hor_sb.setValue( min_vis_meas );
    }


    public void displayMeasurement( ExprData.Measurement meas )
    {
	int id = edata.getMeasurementFromName( meas.getName() );

	if(id >= 0)
	    displayMeasurement( id );
    }

    
    //
    // ---------------- --------------- --------------- ------------- ------------
    //

    private int zoom_scale = 0;

    final public void zoom(int delta)
    {
	setZoom(zoom_scale + delta);
    }

    final public int getZoom()
    {
	return zoom_scale;
    }

    final public void setZoom(int zs)
    {
	// attempt to keep the same spot in the middle of the display
	int sid_index = -1;

	if(spot_id_in_row != null)
	{
	    int mid = spot_id_in_row.length / 2;
	    
	    sid_index = spot_id_in_row[mid]; // sid < edata.getNumSpots() ? edata.getIndexOfSpot(sid) : -1;
	    
	    int sid = -1;
	    if(sid_index >= 0)
		sid = edata.getSpotAtIndex(sid_index);

	    //System.out.println("\nsaving spot_id=" + sid + " index=" + sid_index);
	    //System.out.println("top index=" + spot_id_in_row[0]);
	    //System.out.println("spots per page=" + spots_per_page);
	}

	zoom_scale = zs;

	if(zoom_scale < 0)
	    zoom_scale = 0;
	if(zoom_scale > 4)
	    zoom_scale = 4;

	updateDisplay();

	// dplot_panel.repaint();

	if(sid_index >= 0)
	{
	    //System.out.println("1");
	    
	    //System.out.println("2");
	    
	   
	    // work out what spot to have at the top such that
	    // 'sid' will be in the middle
	    
	    // it will be the spot 'mid' steps up from sid
	    
	    if(sid_index >= 0)
	    {
		int mid = spots_per_page / 2;
		int new_top_index = sid_index - mid;

		//System.out.println("spots per page=" + spots_per_page);
		//System.out.println("top index should be " + new_top_index);

		if(new_top_index < 0)
		    new_top_index = 0;
		if(new_top_index >= edata.getNumSpots() )
		    new_top_index = edata.getNumSpots() -1;

		displaySpot(  edata.getSpotAtIndex( new_top_index ) );
	    }
	}
    }

    final public int getZoomScale()
    {
	return (1 << zoom_scale);
    }

    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // called whan a filter has changed...
    //

    private void updateFilterStatus()
    {
	final int fc = edata.getEnabledFilterCount();
	
	mview.setFilterStatus(fc + " enabled");

	if(filtered_count_is_updating == false)
	{
	    filtered_count_is_updating = true;
	    filter_counter_thread  = new FilterCounter();
	    filter_counter_thread.start();
	}
	else
	   filter_counter_thread.restart(); 
    }

    //
    // filter counting thread 
    // - runs at a low priority and counts how many spots are removed by
    //   the current filter(s)
    //
    private boolean       filtered_count_is_updating = false;
    private FilterCounter filter_counter_thread = null;
    private int           filtered_count = 0;

    private int  check_spot;

    public class FilterCounter extends Thread
    {
	public FilterCounter()
	{
	    super();
	    //System.out.println("FilterCounter created");
	    setPriority(Thread.MIN_PRIORITY);
	    restart();
	}
	public synchronized void restart()
	{
	    filtered_count = 0;
	    check_spot = 0;
	    //System.out.println("filter counting restarts...");
	}
	public synchronized void run()
	{
	    final int fc = edata.getEnabledFilterCount();
	    if(fc > 0)
	    {
		final int n_spots = edata.getNumSpots();
		while(check_spot < n_spots)
		{
		    if(edata.filter(check_spot))
			filtered_count++;
		    
		    check_spot++;
		    
		    yield();
		}
		//System.out.println("filter counting done, " + filtered_count + " Spots filtered");
		
		final String miss_pc = mview.niceDouble((double) ((n_spots-filtered_count) * 100) / (double) n_spots, 7, 3);

		mview.setFilterStatus( fc + " filter enabled : " + 
				       (n_spots-filtered_count) + " spots remain (" + miss_pc + "%), " + filtered_count + " hidden" );
	    }
	    else
	    {
		mview.setFilterStatus("No filters enabled");
	    }

	    filtered_count_is_updating = false;
	}

	private int check_gene;
    }
 
    //
    // ---------------- --------------- --------------- ------------- ------------
    //

    // how long (in pixels) is the longest gene name 
    // when rendered in the current font?
    public void findLongestNames()
    {
	for(int nc=0; nc < n_name_cols; nc++)
	{
	    name_col_width[nc] = 0;
	}
	

	Graphics g = getGraphics();
	if(g != null)// 
	{ 
	    if(spot_label_font != null)
		g.setFont(spot_label_font);

	    FontMetrics fm = g.getFontMetrics();

	    //
    
	    // int sid = -1;

	    // String cchars = null;

	    final int ns =  edata.getNumSpots();

	    // System.out.println("findLongestNames() checking " + n_name_cols + " cols");

	    for(int s=0; s < ns; s++)
	    {
		for(int nc=0; nc < n_name_cols; nc++)
		{
		    // cchars = name_col_sel[nc].getNameTag(s);
		
		    final int ll = fm.stringWidth(getTrimmedNameCol(nc, s));
		
		    if(ll > name_col_width[nc])
			name_col_width[nc] = ll;
		}
	    }
	    
	    /*
	    System.out.print("findLongestNames() ");
	    for(int nc=0; nc < n_name_cols; nc++)
		System.out.print(name_col_sel[nc].getNames() + ":" + name_col_width[nc] + " ");
	    System.out.println("");
	    */

	}
    }

    // called when the any of filter(s), expression data or ploting geometry have changed
    //
    private synchronized void updateDisplay()
    {
	spot_id_in_row = null;
	row_for_spot_id = null;

	row_gap = (int)((float) actual_row_gap / (float) (1 << zoom_scale));
	if(row_gap < 0)
	    row_gap = 0;

	box_height = (int)((float) actual_box_height / (float) (1 << zoom_scale));
	if(box_height < 1)
	    box_height = 1;

	rowskip  = (box_height + row_gap);
	colskip  = (box_width + col_gap);
	topskip  = (border_gap + meas_name_gap + row_gap);

	total_name_col_width = 0;
	for(int nc = 0; nc < n_name_cols; nc++)
	    total_name_col_width += (name_col_width[nc] + name_col_gap);

	leftskip = border_gap + total_name_col_width;
	
	halfrow = box_height / 2;
	
	cluster_line_len = box_height; // (x * 3) / 2;

	// =========================================================================
	// how much space is occupied by the spot clusters?
	//

	ExprData.Cluster rc = edata.getRootCluster();
	Vector rch = rc.getChildren();
	final int nrch = rc.getNumChildren();

	spot_cluster_width = new int[nrch];
	meas_cluster_height = new int[nrch];

	meas_cluster_h = 0;
	spot_cluster_w = 0;
	
	int most_glyphs = 0;

	if((show_branches[0] || show_branches[1]) && (rch != null))
	{
	    final int[] icva = edata.getInVisibleClusterArray();
	    if(icva != null)
	    {
		final int ns = icva.length;
		for(int s=0; s < ns; s++)
		{
		    if(icva[s] > most_glyphs)
			most_glyphs = icva[s];
		}
	    }
	    
	    int sci = 0;
	    int mci = 0;

	    for(int chi=0; chi < rch.size(); chi++)
	    {
		ExprData.Cluster child = ( ExprData.Cluster) rch.elementAt(chi);

		int cd = findClusterDepth(child, child.getIsSpot());
		
		
		// System.out.println("Root." +child.getName() + " depth=" + cd);
		
		if(child.getIsSpot())
		{
		    if(show_glyphs[0])
			cd += (box_height * most_glyphs);
		    
		    if(show_branches[0])
		    {
			if(overlay_root_children[0])
			{
			    if(cd > spot_cluster_w)
				spot_cluster_w = cd;
			}
			else
			{
			    spot_cluster_w += cd;
			}
			spot_cluster_width[sci++] = cd;
		    }
		}
		else
		{
		    if(show_glyphs[1])
			cd += box_height;
		    
		    if(show_branches[1])
		    {
			if(overlay_root_children[1])
			{
			    if(cd > meas_cluster_h)
				meas_cluster_h = cd;
			}
			else
			{
			    meas_cluster_h += cd;
			}
			meas_cluster_height[mci++] = cd;
		    }
		}
	    }
	}

	//System.out.println("spot_cluster_w=" + spot_cluster_w);

	// =========================================================================
	// how much space is occupied by the measurement clusters?
	//
	
	//System.out.println("meas_cluster_h=" + meas_cluster_h);

	first_line_topoff  = meas_cluster_h + topskip + rowskip;
	first_line_leftoff = leftskip + col_gap;

	// where do the measurement clusters start? (after the names and offset by half a column)
	meas_cluster_x_pos = first_line_leftoff + (colskip/2);
	
	// meas_cluster_h += border_gap; //  + rowskip;

	// System.out.println("meas cluster height=" + meas_cluster_h);

	//
	// keep track of which set is drawn into which column
	// and vice versa
	//
	// how wide to we need to be? (some sets might be switched off)

	final int nmeas = edata.getNumMeasurements();

	
	//meas_in_col  = new int[0];
	//col_for_meas_ht = new Hashtable();

	n_visible_meas = 0;
	for(int m=0; m < nmeas; m++)
	    if(edata.getMeasurementShow(m))
		n_visible_meas++;
	
	
	switch(scroll_mode)
	{
	case 3:  // both locked
	    n_possible_meas = (dplot_panel.getWidth() - first_line_leftoff - spot_cluster_w - border_gap) / colskip;
	    if(n_possible_meas < 0)
		n_possible_meas = 0;

	    break;
	case 0:  // both unlocked
	    n_possible_meas = n_visible_meas;
	    break;
	}

	// System.out.println("updateDisplay(): n_p_m = " + n_possible_meas);

	// System.out.println("updateDisplay(): n_v_m = " + n_visible_meas);

	
        // System.out.println("updateDisplay(): there are " +  n_visible_meas + " visible Measurements");

	// how tall do we need to be?
	// (this may well be incorrect (too big), some genes may be filtered out,
	//  or not displayed because of clustering)
	//
	count = edata.getNumSpots();

	int height =  first_line_topoff + (count * rowskip) + border_gap;

	spots_per_page = (int)(Math.ceil((float)(dplot_panel.getHeight() - first_line_topoff - border_gap) / (float)rowskip));

	// System.out.println("height = offset:" + first_line_topoff + " + " + count + 
	//                    " spots of height " + rowskip + " = " + height);
	
	int width  =  first_line_leftoff;
	switch(scroll_mode)
	{
	case 3:  // both locked
	    width += (n_possible_meas * colskip) + spot_cluster_w;
	    break;
	case 0:  // both unlocked
	    width += (n_visible_meas * colskip) + spot_cluster_w;
	    break;
	}

	// double cscale = (double)(ramp_steps - 1);

	//up_scale   = mview.getExprData().getMaxEValue()  / cscale;
	//down_scale = mview.getExprData().getMinEValue()  / cscale;

	//up_err_scale   = mview.getExprData().getMaxErrorValue()  / cscale;
	//down_err_scale = mview.getExprData().getMinErrorValue()  / cscale;
	
	if(dplot_panel == null)
	    return;

	if(dplot_panel.getHeight() <= height)
	{
	    vert_sb.setVisibleAmount(dplot_panel.getHeight());
	    vert_sb.setMaximum(height + dplot_panel.getHeight());
	    //vert_sb.setMaximum(height);
	    vert_sb.setBlockIncrement(rowskip);

	    /*
	    int pixels_avail = dplot_panel.getHeight() - first_line_topoff;
	    
	    if(dplot_panel.getHeight() > 0)
		vert_sb.setBlockIncrement(((box_height + row_gap) * height) / pixels_avail);
	    */

	    vert_sb.setVisible(true);
	   
	    // System.out.println(dplot_panel.getHeight() + " visible from " + height);

	    current_ver_sb_value = vert_sb.getValue();
	}
	else
	{
	    current_ver_sb_value = 0;
	    vert_sb.setVisible(false);

	}
	
	boolean needs_h_sb = false;
	switch(scroll_mode)
	{
	case 3:  // both locked
	    if(n_visible_meas > n_possible_meas)
		needs_h_sb = true;
	    break;
	case 0:  // both unlocked
	    if(dplot_panel.getWidth() <= width)
		needs_h_sb = true;
	    break;
	}

	if(needs_h_sb)
	{
	    // int spots_width = (n_visible_meas * colskip);

	    if(scroll_mode == 3)
	    {
		hor_sb.setVisibleAmount(n_possible_meas);
		hor_sb.setMaximum(n_visible_meas);
		//vert_sb.setMaximum(height);
		hor_sb.setBlockIncrement(1);
	    }
	    else
	    {
		hor_sb.setVisibleAmount(dplot_panel.getWidth());
		hor_sb.setMaximum(width + dplot_panel.getWidth());
		hor_sb.setBlockIncrement(colskip);
	    }

	    hor_sb.setVisible(true);
	   
	    current_hor_sb_value = hor_sb.getValue();
	    // System.out.println(dplot_panel.getHeight() + " visible from " + height);
	}
	else
	{
	    current_hor_sb_value = 0;
	    hor_sb.setVisible(false);
	}


	glyph_poly = getScaledClusterGlyphs(box_height);

	repaint();
    } 
    
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // d a t a    c o l o u r i n g
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    //
    // the mapping of Measurement to Colouriser is not part of ExprData....should it be?
    // 
    //  Colourisers are identified by name
    //
    Hashtable   colouriser_by_name        = null;  // which colourisers are known

    Hashtable    colouriser_by_measurement_name  = null;  // which colouriser is each Measurement using 
    Colouriser[] colouriser_by_measurement_id   = null;   // which colouriser is each Measurement using (for quick lookup...)

    Colouriser default_colouriser = null;

    public Colouriser[] getColouriserArray()
    {
	if(first_one)
	{
	    Colouriser[] ca = new Colouriser[colouriser_by_name.size()];
	    int cai = 0;
	    for (Enumeration e = colouriser_by_name.keys(); e.hasMoreElements() ;) 
	    {
		ca[cai++] = (Colouriser) colouriser_by_name.get(e.nextElement());
	    }
	    return ca;
	}
	else
	{
	    return mview.getDataPlot().getColouriserArray();
	}
    }

    public String[] getColouriserNameArray()
    {
	if(first_one)
	{
	    String[] cna = new String[colouriser_by_name.size()];
	    int cai = 0;
	    for (Enumeration e = colouriser_by_name.keys(); e.hasMoreElements() ;) 
	    {
		cna[cai++] = ((Colouriser) colouriser_by_name.get(e.nextElement())).getName();
	    }
	    return cna;
	}
	else
	{
	    return mview.getDataPlot().getColouriserNameArray();
	}
    }

    public Colouriser getColouriserForMeasurement(int meas_id) 
    {
	try
	{
	    if(first_one)
	    {
		return colouriser_by_measurement_id[meas_id];
	    }
	    else
	    {
		return mview.getDataPlot().getColouriserForMeasurement(meas_id);
	    }
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return default_colouriser;
	}
    }

    public Colouriser getColouriserForMeasurement(String mname) 
    {
	if(first_one)
	{
	    int m_id = edata.getMeasurementFromName(mname);
	    
	    return (m_id >= 0) ? colouriser_by_measurement_id[m_id] : null;
	}
	else
	{
		return mview.getDataPlot().getColouriserForMeasurement(mname) ;
	}
    }

    public void setColouriserForMeasurement( String mname, String cname ) 
    {
	if(first_one)
	{
	    int m_id = edata.getMeasurementFromName(mname);
	    
	    setColouriserForMeasurement(mname, m_id, cname);
	}
	else
	{
	    mview.getDataPlot().setColouriserForMeasurement(mname, cname);
	}
    }

    public void setColouriserForMeasurement( int meas_id, String cname ) 
    {
	if(first_one)
	{
	    setColouriserForMeasurement( edata.getMeasurementName(meas_id), meas_id, cname );
	}
	else
	{
	    mview.getDataPlot().setColouriserForMeasurement( meas_id, cname );
	}  
    }

    public void setColouriserForMeasurements( int[] meas_id, String cname ) 
    {
	for( int m = 0 ; m < meas_id.length; m++ )
	{
	    if(first_one)
	    {
		setColouriserForMeasurement( edata.getMeasurementName( meas_id[ m ] ), meas_id[ m ], cname );
	    }
	    else
	    {
		mview.getDataPlot().setColouriserForMeasurement( meas_id[ m ], cname );
	    }  
	}
    }

    private void setColouriserForMeasurement(String mname, int meas_id, String cname) 
    {
	Colouriser col = getColouriserByName(cname);
	
	if(meas_id >= 0)
	{
	    colouriser_by_measurement_id[meas_id] = col;
	    
	    if(colouriser_by_measurement_id[meas_id] == null)
	    {
		colouriser_by_measurement_id[meas_id] = default_colouriser;
	    }
	}

	if(colouriser_by_measurement_name == null)
	    colouriser_by_measurement_name = new Hashtable();

        // System.out.println(mname + " is using " + cname);

	colouriser_by_measurement_name.put(mname, col);

	updateColourisers();
    }

    public void updateColourisers()
    {
	// work out the min/max for each Colouriser in use...
	if(colouriser_by_measurement_id == null)
	    return;
	
	final int n_cols = colouriser_by_name.size();
	final int n_meas = edata.getNumMeasurements();

	boolean[] used = new boolean[n_cols];
	double[] min = new double[n_cols];
	double[] max = new double[n_cols];

	Hashtable col_use = new Hashtable();

	try
	{
	    for(int m=0; m < n_meas; m++)
	    {
		if(edata.getMeasurementShow(m))
		{
		    Colouriser col = colouriser_by_measurement_id[m];
		    ColRange cr = (ColRange) col_use.get(col);
		    
		    final double mmin = edata.getMeasurementMinEValue(m);
		    final double mmax = edata.getMeasurementMaxEValue(m);

		    if(cr == null)
		    {
			cr = new ColRange();
			col_use.put(col, cr);
		    }
		    cr.count++;
		    if(mmin < cr.min)
			cr.min = mmin;
		if(mmax > cr.max)
		    cr.max = mmax;
		cr.data_arrays.addElement(edata.getMeasurementData(m));
		}
	    }
	    
	    for (Enumeration e = col_use.keys(); e.hasMoreElements() ;) 
	    {
		Colouriser col = (Colouriser) e.nextElement();
		ColRange cr = (ColRange) col_use.get(col);
		
		// System.out.println(col.getName() + " used=" + cr.count + " min=" + cr.min + " max=" + cr.max);
		
		col.setRange(cr.min, cr.max, cr.data_arrays);
	    }
	    

	    repaint();
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    // data updates havenot caught up yet...
	}

    }

    public Colouriser getColouriserByName(String cname) 
    {
	Colouriser col = (Colouriser) colouriser_by_name.get(cname);
	return (col == null) ? default_colouriser : col;
    }

    // used when this is not the first DataPlotPanel is the system
    //
    private void getColourisers()
    {
	/*
	  DataPlot first_dplot = mview.getDataPlot();
	
	colouriser_by_name = first_dplot.colouriser_by_name;
	colouriser_by_measurement_name = first_dplot.colouriser_by_measurement_name
	colouriser_by_measurement_id = first_dplot.
	*/
    }


    private void initialiseColours()
    {
	colouriser_by_name = new Hashtable();
	colouriser_by_measurement_name = new Hashtable();

	RampedColouriser def = new RampedColouriser("(default)", 64, Color.black, Color.gray, Color.white);
	default_colouriser = def;   // but don't add this to the visible list of Colourisers...

	RampedColouriser rc1 = new RampedColouriser("Ramped 1", 64, Color.green, Color.white, Color.red);
	addColouriser(rc1);

	/*
	BlenderColouriser bc1 = new BlenderColouriser("Blender 1", 100, 0);

	DiscreteColouriser dc1 = new DiscreteColouriser("Discrete 1", 5);

	addColouriser(bc1);
	addColouriser(dc1);
	*/
    }

    // 
    // called when the Measurement order has changed, or Colourisers or Measurements have been
    //   added or removed
    //
    // uses the Hashtable of (MeasurementName -> Colouriser) to build an array
    //
    private void mapColourisersToMeasurements()
    {
	if(first_one)
	{
	    final int n_measurements = edata.getNumMeasurements();
	    
	    if(n_measurements == 0)
	    {
		colouriser_by_measurement_id = null;
	    }
	    else
	    {
		colouriser_by_measurement_id = new Colouriser[n_measurements];
		
		for(int m=0; m < n_measurements; m++)
		{
		    colouriser_by_measurement_id[m] = (Colouriser) colouriser_by_measurement_name.get(edata.getMeasurementName(m));
		    
		    if(colouriser_by_measurement_id[m] == null)
			colouriser_by_measurement_id[m] = default_colouriser;
		    
		}
	    }

	    // System.out.println("mapColourisersToMeasurements(): " + n_measurements + " Measurements mapped");
	    
	    updateColourisers();
	}
    }

    // used by input routines, attempts to build a Colouriser from
    // the known types by passing them a map of Name, Values to look at
    //
    public void addColouriser(Hashtable attrs)
    {
	if(first_one)
	{
	    Colouriser c = new RampedColouriser().createFromAttrs(attrs);
	    if(c == null)
	    {
		c = new DiscreteColouriser().createFromAttrs(attrs);
		
		if(c == null)
		{
		    c = new BlenderColouriser().createFromAttrs(attrs);

		    if(c == null)
		    {
			c = new EqualisingColouriser().createFromAttrs(attrs);
		    }
		}
	    }
	    
	    if(c != null)
		addColouriser(c);
	}
	else
	{
	    mview.getDataPlot().addColouriser(attrs);
	}
    }

    // programatic construction of colourisers, useful for scripts
    //
    // the known types by passing them a map of Name, Values to look at
    //
    public void addColouriser( String colouriser_type, String colouriser_name, Hashtable attrs)
    {
	Colouriser c = null;

	attrs.put( "NAME", colouriser_name );
	attrs.put( "TYPE", colouriser_type + "Colouriser" );

	System.out.println("creating a " + colouriser_type + " Colouriser");

	if( colouriser_type.equals("Ramped") )
	{
	    c = new RampedColouriser().createFromAttrs(attrs);
	}

	if( colouriser_type.equals("Discrete") )
	{
	    c = new DiscreteColouriser().createFromAttrs(attrs);
	}

	if( colouriser_type.equals("Blender") )
	{
	    c = new BlenderColouriser().createFromAttrs(attrs);
	}

	if( colouriser_type.equals("Equalising") )
	{
	    c = new EqualisingColouriser().createFromAttrs(attrs);
	}

	if(c != null)
	{
	    System.out.println("  created ok");

	    if(first_one)
	    {
		addColouriser(c);
	    }
	    else
	    {
		mview.getDataPlot().addColouriser( c );
	    }
	}
    }

    public void addColouriser(Colouriser new_c)
    {
	if(first_one)
	{
	    String unique_name = new_c.getName();
	    
	    if(colouriser_by_name.get(unique_name) != null)
	    {
		boolean unique = false;
		int c = 0;
		while(!unique)
		{
		    unique_name = new_c.getName() + "(" + (++c) + ")";
		    unique = (colouriser_by_name.get(unique_name) == null);
		}
	    }
	
	    new_c.setName(unique_name);
	    colouriser_by_name.put(unique_name, new_c);
	    updateColourisers();
	    edata.generateMeasurementUpdate(ExprData.ColouriserAdded);
	}
	else
	{
	    mview.getDataPlot().addColouriser(new_c);
	}
    }

    public void removeAllColourisers()
    {
	if(first_one)
	{
	    if(colouriser_by_measurement_id != null)
	    {
		for(int m=0; m < colouriser_by_measurement_id.length; m++)
		{
		    colouriser_by_measurement_id[m] = default_colouriser;
		}
	    }
	    colouriser_by_measurement_name = new Hashtable();
	    colouriser_by_name = new Hashtable();
	    
	    mapColourisersToMeasurements();
	    updateColourisers();
	    edata.generateMeasurementUpdate(ExprData.ColouriserRemoved);
	}
	else
	{
	    mview.getDataPlot().removeAllColourisers();
	}
     }

    public void deleteColouriser(Colouriser del_c)
    {
	if(first_one)
	{
	    // is it being used?
	    
	    boolean used = false;
	    if(colouriser_by_measurement_id != null)
	    {
		for(int m=0; m < colouriser_by_measurement_id.length; m++)
		{
		    if(colouriser_by_measurement_id[m] == del_c)
			used = true;
		}
	    }
	    if(used)
	    {
		if(mview.infoQuestion("This Colouriser is in use, really delete it?", "Yes", "No") == 1)
		    return;
		
		// change colouriser_by_measurement_id for any measurement currently using this colouriser
		for(int m=0; m < colouriser_by_measurement_id.length; m++)
		{
		    if(colouriser_by_measurement_id[m] == del_c)
		    {
			colouriser_by_measurement_name.remove(edata.getMeasurementName(m));
			colouriser_by_measurement_id[m] = default_colouriser;
		    }
		}
	    }
	    
	    colouriser_by_name.remove(del_c.getName());
	    
	    mapColourisersToMeasurements();
	    updateColourisers();
	    edata.generateMeasurementUpdate(ExprData.ColouriserRemoved);
	}
	else
	{
	    mview.getDataPlot().deleteColouriser(del_c);
	}
    }

    public void renameColouriser(Colouriser ren_c)
    {
	if(first_one)
	{
	    try
	    {
		String new_name = mview.getString("New name for '" + ren_c.getName() + "'", ren_c.getName());
		
		// must also change the key in the lookup table...
		
		colouriser_by_name.remove(ren_c.getName());
		
		ren_c.setName(new_name);
		
		colouriser_by_name.put(new_name, ren_c);
		
		edata.generateMeasurementUpdate(ExprData.ColouriserChanged);
	    }
	    catch(UserInputCancelled e)
	    {
	    }
	}
	else
	{
	    mview.getDataPlot().renameColouriser(ren_c);
	}

    }

    public void cloneColouriser(Colouriser clo_c)
    {
	if(first_one)
	{
	    Colouriser nc = clo_c.cloneColouriser();
	    nc.setName("Copy of (" + nc.getName() + ")");
	    addColouriser(nc);
	}
	else
	{
	    mview.getDataPlot().cloneColouriser(clo_c);
	}
    }

    public Color getDataColour(double p, int meas_id)
    {
	if(first_one)
	{
	    Colouriser col = meas_id < colouriser_by_measurement_id.length ? colouriser_by_measurement_id[meas_id] : default_colouriser;
	    
	    return col.lookup(p);
	}
	else
	{
	    return mview.getDataPlot().getDataColour(p, meas_id);
	}
    }
    
    private class ColRange
    {
	double min, max;
	int count;
	Vector data_arrays;
	
	public ColRange()
	{
	    min = Double.MAX_VALUE;
	    max = -Double.MAX_VALUE;
	    count = 0;
	    data_arrays = new Vector();
	}
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // sorting
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    private void sortMeasurement(int meas_col, int dir)
    {
	// System.out.println("sort meas " + meas_col);

	edata.sortSpots(meas_col, dir, null);
    }

    private void sortTagAttrs(int col)
    {
	// System.out.println("sort tags " + col);
	
	final int n_spots = edata.getNumSpots();
	final ExprData.NameTagSelection nts = name_col_sel[col];
	
	final TagEntry[] tags = new TagEntry[n_spots];
	for(int s=0; s < n_spots; s++)
	    tags[s] = new TagEntry( s, nts.getNameTag(s) );

	Arrays.sort(tags, new TagEntryComparator());

	int[] new_order = new int[n_spots];
	
	for(int s=0; s < n_spots; s++)
	    new_order[s] = tags[s].spot_id;

	edata.setSpotOrder(new_order);
    }

    private class TagEntry
    {
	public String tag;
	public int spot_id;

	public TagEntry( int s, String t) 
	{ 
	    tag = t; 
	    spot_id = s; 
	}
    }

    private class TagEntryComparator implements Comparator
    {
	public int compare(Object o1, Object o2) 
	{ 
	    TagEntry t1 = (TagEntry) o1;
	    TagEntry t2 = (TagEntry) o2;
	    if(t1.tag == null)
	    {
		return (t2.tag == null) ? 0 : 1;
	    }
	    else
	    {
		return (t2.tag == null) ? -1 : t1.tag.compareTo(t2.tag);
	    }
	}
	public boolean equals(Object obj)        { return false; }
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // mouse handling
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    private JPopupMenu cluster_popup;
    private JPopupMenu spot_popup;
    private JPanel spot_popup_panel;
    private JLabel[] spot_popup_label;
    private JPopupMenu spot_name_popup;
    private JPopupMenu meas_name_popup;
    private JPopupMenu nothing_popup;
    private JMenuItem comment_switch_mi;

    //private JMenu select_menu;

    private boolean stop_welcome_animation = false;
    public void stopAnimation()
    {
	if(!stop_welcome_animation)
	{
	    dplot_panel.removeWelcomeMessage();
	    stop_welcome_animation = true;
	}
    }
    
    // hacky hack used to communicate extra data to the popup menus....
    private int popup_data_1;
    private int popup_data_2;

    private CustomMenu custom_menu;

    public class CustomMenuListener implements ActionListener
    {
	private int menu, item;

	public CustomMenuListener(int menu_, int item_)
	{
	    menu = menu_;
	    item = item_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    //System.out.println("menu " + menu + " item " + item);

	    if(cur_pos == null)
	    {    
		try
		{
		    cur_pos = getThingPosUnderMouse( last_mouse_x, last_mouse_y );
		}
		catch(NothingUnderMouse num)
		{
		    return;
		}
	    }
	    
	    stop_welcome_animation = false;

	    switch(menu)
	    {
		// ------------------ the cluster popup ------------------  
		
	    case 0:  // the cluster popup
		
		// which cluster are we currently pointing at?
		
		switch(item)
		{
		case 0: // hide children

		    edata.clusterShowGroup(cur_pos.cluster, false);
		    
		    break;
		case 1: // show children

		    edata.clusterShowGroup(cur_pos.cluster, true);
		    
		    break;
		case 2: // show parent
		    
		    if(cur_pos.cluster != null)
			if(cur_pos.cluster.getParent() != null)
			    if(cur_pos.cluster.getParent().getShow() == false)
				edata.setClusterShow(cur_pos.cluster.getParent(), true);
		    
		    break;
		case 3: // show parent & siblings
		    
		    if(cur_pos.cluster != null)
			if(cur_pos.cluster.getParent() != null)
			    edata.clusterShowGroup(cur_pos.cluster.getParent(), true);
		    
		    break;
		case 4: // hide all others
		    
		    if(cur_pos.cluster.getIsSpot())
			edata.clusterShowAllSpots( edata.getRootCluster(), false);
		    else
			edata.clusterShowAllMeasurements( edata.getRootCluster(), false);

			//edata.clusterShowGroup(edata.getRootCluster(), false);

		    if(cur_pos.cluster != null)
			edata.clusterShowGroup(cur_pos.cluster, true);
		    
		    break;
		case 5: // show all others
		    if(cur_pos.cluster.getIsSpot())
			edata.clusterShowAllSpots( edata.getRootCluster(), true);
		    else
			edata.clusterShowAllMeasurements( edata.getRootCluster(), true);
		    
		    // edata.clusterShowGroup(edata.getRootCluster(), true);
		    
		    break;

		case 6: // hide parent

		    if(cur_pos.cluster.getParent() != null)
			edata.setClusterShow(cur_pos.cluster.getParent(), false);

		    break;

		case 7: // show props
		    
		    if(cur_pos.cluster != null)
		    {
			String[] args = new String[2];
			args[0] = "id";
			args[1] = String.valueOf(cur_pos.cluster.getId());
			
			Plugin pl = mview.startPlugin("Cluster Manager");
			mview.runCommand(pl, "gotoID", args);
		    }
		    break;

		}
		return;

		// ------------------ the spot name popup ------------------  
		
	    case 1:  // the spot name popup
		switch(item)
		{
		    /*
		case 0: // select show gene names
		    //setRowLabelSource(2);
		    break;
		case 1: // select show probe names
		    //setRowLabelSource(1);
		    break;
		case 2: // select show spot names
		    //setRowLabelSource(3);
		    break;
		case 3: // toggle spot comments
		    //setShowComments(!getShowComments());
		    break;
		    */
		case 4: // edit names
		    startNameEditor(-1, -1, edata.getSpotAtIndex(cur_pos.data_row));
		    break;
		case 5: // display anno
		    mview.spotSelected(edata.getSpotName(edata.getSpotAtIndex(cur_pos.data_row)));
		    break;
		case 6: // choose what to display in this column
		    chooseColContents(cur_pos.data_col);
		    break;
		case 7: // add column
		    addNameCol(cur_pos.data_col);
		    break;
		case 8: // remove column
		    removeNameCol(cur_pos.data_col);
		    break;
		case 10: // sort col
		    sortTagAttrs(cur_pos.data_col);
		    break;

		}
		return;

		// ------------------ the measurement name popup ------------------  

	    case 2: // the measurement name popup
		switch(item)
		{
		case 0: // hide this measurement
		    edata.setMeasurementShow(cur_pos.data_col, false);
		    break;
		case 1: // show all
		    edata.showAllMeasurements();
		    break;
		    
		case 2:
		    {
			String[] args = new String[2];
			args[0] = "name";
			args[1] = edata.getMeasurementName(cur_pos.data_col);
			mview.sendCommandToPlugin("Measurement Manager", "showProperties", args);
		    }
		    break;

		case 3: // pick colouriser
		    String[] col_opts = getColouriserNameArray();
		    
		    String[] col_switches = null;
		    
		    boolean offer_selection_option = false;

		    if( edata.getMeasurementSelection().length > 0 )
		    {
			col_switches = new String[] { "Apply to all selected Measurements" };
			offer_selection_option = true;
		    }

		    if(col_opts.length > 0)
		    {
			try
			{
			    int[] result = mview.getChoice( "Select a Colouriser", col_opts, col_switches );
			    
			    if( offer_selection_option )
			    {
				if( result[ 0 ] == 1 ) // switch is selected
				    setColouriserForMeasurements( edata.getMeasurementSelection(), col_opts[ result[ 1 ] ] );
				else
				    setColouriserForMeasurement( popup_data_1, col_opts[  result[ 1 ] ]);
			    }
			    else
			    {
				setColouriserForMeasurement( popup_data_1, col_opts[  result[ 0 ] ]);
			    }
			}
			catch(UserInputCancelled uic)
			{
			}
		    }
		    else
		    {
			if(mview.alertQuestion("No Colourisers defined.\nCreate one now?", "Yes", "No") == 0)
			{
			    new DataPlotColourOptions(mview).addNewColouriser();
			}
		    }
		    
		    break;
		case 4: 
		    {
			String[] args = new String[2];
			args[0] = "name";
			args[1] = edata.getMeasurementName(cur_pos.data_col);
			mview.sendCommandToPlugin("Measurement Manager", "showAttributes", args);
		    }
		    break;

		


		}
		return;
		
		// ------------------ the spot popup ------------------  
		
	    case 3: // the spot popup
		switch(item)
		{
		case 0: // display anno
		    mview.spotSelected(edata.getSpotName(edata.getSpotAtIndex(cur_pos.data_row)));
		    break;
		case 1: // edit names
		    startNameEditor(-1, -1, edata.getSpotAtIndex(cur_pos.data_row));
		    break;
		case 10: // sort meas
		    sortMeasurement(cur_pos.data_col, 0);
		    break;
		case 11: // sort meas
		    sortMeasurement(cur_pos.data_col, 1);
		    break;
		}
		return;

		// ------------------ the custom sub-menu ------------------  
		
		/*
	    case 4: // the custom sub-menu
		if(item == -1)
		{
		    //System.out.println("edit custom actions...");
		}
		else
		{
		    //System.out.println("run custom action " + item + "...");
		}
		break;
		*/

	    case 20: // the select sub-menu for Spots
		switch(item)
		{
		case 0: // "Unselect all"
		    edata.clearSpotSelection();
		    edata.clearMeasurementSelection();
		    break;

		case 1: // "Invert selection"
		    edata.invertSpotSelection();
		    edata.invertMeasurementSelection();
		    break;

		case 4: // "Add filtered spots"
		    edata.addFilteredSpots();
		    //repaint();
		    break;

		case 5: // "Remove filtered spots"
		    edata.removeFilteredSpots();
		    //repaint();
		    break;

		case 3: // "Select all"
		    edata.selectAllSpots( apply_filter );
		    edata.selectAllMeasurements();
		    //repaint();
		    break;

		}
		return;

	    case 21: // the select sub-menu for SpotNames
		switch(item)
		{
		case 0: // "Unselect all"
		    edata.clearSpotSelection();
		    //repaint();
		    break;

		case 1: // "Invert selection"
		    edata.invertSpotSelection();
		    //repaint();
		    break;

		case 4: // "Add filtered spots"
		    edata.addFilteredSpots();
		    //repaint();
		    break;

		case 5: // "Remove filtered spots"
		    edata.removeFilteredSpots();
		    //repaint();
		    break;

		case 3: // "Select all"
		    edata.selectAllSpots( apply_filter );
		    //repaint();
		    break;

		}
		return;

	    case 22: // the select sub-menu for Clusters
		switch(item)
		{
		case 0: // "Unselect all"
		    edata.clearClusterSelection();
		    repaint();
		    break;

		case 1: // "Convert to Measurement or Spot selection"

		    if(cur_pos.cluster != null)
		    {
			if( cur_pos.cluster.getIsSpot() )
			    edata.convertClusterSelectionToSpotSelection();
			else
			    edata.convertClusterSelectionToMeasurementSelection();
		    }

		    //repaint();
		    break;

		
		case 4: // "Add filtered spots"
		    //repaint();
		    break;

		case 5: // "Remove filtered spots"
		    //repaint();
		    break;
		}
		return;

	    case 23: // the select sub-menu for MeasurementNames
		switch(item)
		{
		case 0: // "Unselect all"
		    edata.clearMeasurementSelection();
		    //repaint();
		    break;

		case 1: // "Invert selection"
		    edata.invertMeasurementSelection();
		    //repaint();
		    break;
		    
		case 2: // "Select all"
		    edata.selectAllMeasurements();
		    //repaint();
		    break;

		    // new in 0.9.4

		case 3: // "Hide selected"
		    edata.hideSelectedMeasurements();
		    //repaint();
		    break;

		case 4: // "Hide unselected"
		    edata.hideUnselectedMeasurements();
		    //repaint();
		    break;
 
		}
		return;


	    case 6: // the 'send to' sub-menu for sending Spots to RemoteDataSink
		{
		    ExprData.RemoteDataSink sink =  (ExprData.RemoteDataSink) (edata.getRemoteDataSinks().elementAt(item));
		    //System.out.println("sending selection to " + sink.getName());
		    int[] ss = edata.getSpotSelection();
		    if(ss.length > 0)
		    {
			try
			{
			    sink.consumeSpots( ss );
			}
			catch(java.rmi.RemoteException re)
			{
			    mview.alertMessage("RMI Exception:\n" + re.toString());
			}
		    }
		    else
			mview.alertMessage("No spots are selected");
		}
		return;

	    case 7: // the 'send to' sub-menu for an ExternalDataSink
		{
		    ExprData.ExternalDataSink sink =  (ExprData.ExternalDataSink) (edata.getExternalDataSinks().elementAt(item));
		    //System.out.println("sending selection to " + sink.getName());
		    int[] ss = edata.getSpotSelection();
		    if(ss.length > 0)
		    {
			sink.consumeSpots( ss );
		    }
		    else
			mview.alertMessage("No spots are selected");
		}
		return;

	    case 16: // the 'send to' sub-menu for sending Clusters to RemoteDataSink
		{
		    ExprData.RemoteDataSink sink =  (ExprData.RemoteDataSink) (edata.getRemoteDataSinks().elementAt(item));
		    //System.out.println("sending selection to " + sink.getName());
		    ExprData.Cluster[] cs = edata.getClusterSelection();

		    
		    if(cs.length > 0)
		    {
			try
			{
			    // todo: convert to handles
			    // ....
			    sink.consumeClusters( (ExprData.ClusterHandle[]) null );
			}
			catch(java.rmi.RemoteException re)
			{
			    mview.alertMessage("RMI Exception:\n" + re.toString());
			}
		    }
		    else
			mview.alertMessage("No spots are selected");
		}
		return;

	    case 17: // the 'send to' sub-menu for sending Clusters to an ExternalDataSink
		{
		    ExprData.ExternalDataSink sink =  (ExprData.ExternalDataSink) (edata.getExternalDataSinks().elementAt(item));
		    //System.out.println("sending selection to " + sink.getName());
		    ExprData.Cluster[] cs = edata.getClusterSelection();
		    if(cs.length > 0)
		    {
			sink.consumeClusters( cs );
		    }
		    else
			mview.alertMessage("No spots are selected");
		}
		return;

	    default:
		System.out.println("CustomMenuListener(): menu not handled yet");
	    }
	}
    }

    private int last_mouse_x, last_mouse_y;

    public class PanelMouseMotionListener implements MouseMotionListener
    {
	public void mouseMoved(MouseEvent e) 
	{
	    try
	    {
		last_mouse_x = e.getX();
		last_mouse_y = e.getY();
		
		cur_pos = getThingPosUnderMouse(last_mouse_x, last_mouse_y);
		
		boolean same = false;

		if((last_pos != null) && (last_pos.equals(cur_pos)))
		   same = true;

		String str = null;

		//dplot_panel.setToolTipText("");

		if(!same)
		{
		    switch(cur_pos.element)
		    {
		    case ClusterElement:
			/*
			if(cur_pos.data_row >= 0)
			    str =  new String(" [ " + edata.getProbeNameAtIndex(cur_pos.data_row) + 
					      " " + cur_pos.cluster.getName() + " ]");
			else
			    str =  new String(" [ " + cur_pos.cluster.getName() + " ]");
			*/
			//if(cur_pos.data_row >= 0)
			//    str =  new String(" [ " + getTrimmedSpotLabel(edata.getSpotAtIndex(cur_pos.data_row)) + 
			//		      " " + cur_pos.cluster.getName() + " ]");
			//else
			//str =  new String(" [ " + cur_pos.cluster.getName() + " ]");

			str =  new String("[ " + cur_pos.cluster.getName() + " ");
			final int clsi = cur_pos.cluster.getSize();
			
			if(clsi > 2)
			{
			    str += ("(" + (clsi-1) + " others) ]");
			}
			else
			{
			    if(clsi == 2)
			    {
				str += "(1 other) ]";
			    }
			}
			break;

		    case ClusterBranch:
			str =  new String("[ " + cur_pos.cluster.getName() + " ]");
			break;
		    case DataElement:
			if((cur_pos.data_row < edata.getNumSpots()) && (cur_pos.data_col < edata.getNumMeasurements()))
			{
			    
			    String estr = String.valueOf(edata.eValueAtIndex(cur_pos.data_col, cur_pos.data_row));
			    /*
			    str = new String(" [ " + edata.getSpotNameAtIndex(cur_pos.data_row) + ": " + 
					     edata.getProbeNameAtIndex(cur_pos.data_row) + 
					     ", " + edata.getMeasurementName(cur_pos.data_col) + " ] = " + estr);
			    */
			    str = new String("[ " +  edata.getMeasurementName(cur_pos.data_col) + 
					     ", " + getTrimmedNameCol(0, edata.getSpotAtIndex(cur_pos.data_row)) + " ] = " + estr);

			    //dplot_panel.highlightCurrentSpot(edata.getSpotAtIndex(cur_pos.data_row));
			}
			break;
		    case MeasurementNameElement:
			str = new String("[ " + edata.getMeasurementName(cur_pos.data_col) + " ]");
			//dplot_panel.setToolTipText(String.valueOf(edata.eValueAtIndex(cur_pos.data_col, cur_pos.data_row)));
			break;
		    case NameColElement:
			if(cur_pos.data_col >=0)
			{
			    if(cur_pos.data_row < edata.getNumSpots())
			    {
				String tag = getTrimmedNameCol(cur_pos.data_col, edata.getSpotAtIndex(cur_pos.data_row));
				if((tag != null) && (tag.length() > 0))
				    str = new String("[ " + tag + " ]");
			    }
			}
			/*
			else
			{
			    System.out.println("wierd: name col is " + cur_pos.data_col);

			    for(int n=0; n < n_name_cols; n++)
				System.out.print( name_col_width[n] + "\t");
			    System.out.println("");
			}
			*/

			//dplot_panel.highlightCurrentSpot(edata.getSpotAtIndex(cur_pos.data_row));
			break;
			/*
		    case SpotCommentElement:
			{
			    
			    //String com = edata.getSpotComment(edata.getSpotAtIndex(cur_pos.data_row));

			    if(com != null)
				str = new String(" [ " + getTrimmedNameCol(0, edata.getSpotAtIndex(cur_pos.data_row)) + 
						 " = " + com + " ]");
			    else
				str = new String(" [ " + getTrimmedNameCol(0, edata.getSpotAtIndex(cur_pos.data_row)) + " ]");
			}
			//dplot_panel.highlightCurrentSpot(edata.getSpotAtIndex(cur_pos.data_row));
			break;
			*/
		    }

		    mview.setMessage(str, false);

		    last_pos = cur_pos;

		    if(str != null)
		    {
			dplot_panel.tool_tip_text = str;
			dplot_panel.setToolTipText(str);
		    }
		    else
		    {
			dplot_panel.tool_tip_text = null;
		    }

		}

		//dplot_panel.setToolTipText(str == null ? "" : str);
	    }
	    catch (NothingUnderMouse num)
	    {
		// clear the message display back to normal
		last_pos = null;
		String msg = edata.getNumMeasurements() + " Measurements of " + edata.getNumSpots() + " Spots";
		mview.setMessage(msg, false);
		dplot_panel.setToolTipText("");
	    }
	}

	public void mouseDragged(MouseEvent e) 
	{
	} 
    }
    
    public void showMenu(Component comp, int x, int y)
    {
	if(x == -1)
	    x = last_mouse_x;
	if(y == -1)
	    y = last_mouse_y;

	if(comp == null)
	    comp = dplot_panel;

	if(edata.getNumSpots() == 0)
	    stopAnimation();
	
	// System.out.println("popup trigger");
	
	String str = null;
	
	if(cur_pos == null)
	{    
	    try
	    {
		cur_pos = getThingPosUnderMouse(x, y);
	    }
	    catch(NothingUnderMouse num)
	    {
	    }
	}
	
	if(cur_pos != null)
	{
	    switch(cur_pos.element)
	    {
	    case ClusterElement:
	    case ClusterBranch:
		cluster_popup = makeClusterMenu(); 
		cluster_popup.show(comp,x , y);
		break;
	    case NameColElement:
		//comment_switch_mi.setText(show_comments ? "Hide Spot Comments" : "Show Spot Comments");
		
		// this menu is built on the fly....
		spot_name_popup = new JPopupMenu();
		
		spot_name_popup.add( makeSelectMenu(1,21, 6) );
		
		spot_name_popup.addSeparator();
		
		JMenuItem mi = new JMenuItem("Display Annotation");
		mi.addActionListener(new CustomMenuListener(1,5));
		spot_name_popup.add(mi);
		
		mi = new JMenuItem("Edit Names & Attrs");
		mi.addActionListener(new CustomMenuListener(1,4));
		spot_name_popup.add(mi);
		
		final NameTagSelector nt_sel = new NameTagSelector(mview);
		nt_sel.setNameTagSelection(name_col_sel[cur_pos.data_col]);
		
		// System.out.println("name col editor for col " + cur_pos.data_col);
		final int name_col = (cur_pos == null) ? -1 : cur_pos.data_col;
		
		nt_sel.addActionListener(new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    if( (name_col >= 0) && (name_col < n_name_cols) )
			    {    
				setNameColSelection(name_col, nt_sel.getNameTagSelection()); 
			    }
			}
		    });
		//JMenu name_popup = nt_sel.makeMenu("Show in this column...");
		//spot_name_popup.add(name_popup);
		mi = new JMenuItem("Show in this column...");
		mi.addActionListener(new CustomMenuListener(1,6));
		spot_name_popup.add(mi);
		
		
		mi = new JMenuItem("Add another column");
		mi.addActionListener(new CustomMenuListener(1,7));
		spot_name_popup.add(mi);
		
		mi = new JMenuItem("Remove this column");
		mi.addActionListener(new CustomMenuListener(1,8));
		spot_name_popup.add(mi);
		
		mi = new JMenuItem("Sort this column");
		mi.addActionListener(new CustomMenuListener(1,10));
		spot_name_popup.add(mi);
		
		spot_name_popup.addSeparator();
		
		if( mview.addExternalPopupEntries(spot_name_popup) > 0 )
		    spot_name_popup.addSeparator();
		
		spot_name_popup.add(custom_menu.createMenu());
		
		spot_name_popup.show(comp, x, y);
		break;
		
	    case DataElement:
		boolean is_name = (cur_pos.element == NameColElement) ? true : false;
		int spot_id = cur_pos.data_row;
		int meas_id = (is_name) ? -1 : cur_pos.data_col;
		
		spot_popup = makeSpotMenu();
		populateSpotInfoViewer(is_name, spot_id, meas_id);
		
		spot_popup.show(comp, x, y);
		//spot_popup_frame = startSpotInfoViewer(is_name, spot_id, meas_id, e.getX(), e.getY());
		break;
		
	    case MeasurementNameElement:
		popup_data_1 = cur_pos.data_col;
		meas_name_popup = makeMeasNameMenu();
		meas_name_popup.show(comp, x, y);
		break;
		
	    case Nothing:
		nothing_popup = new JPopupMenu();
		
		if( mview.addExternalPopupEntries(nothing_popup) > 0 )
		    nothing_popup.addSeparator();
		
		nothing_popup.add(custom_menu.createMenu());
		nothing_popup.show(comp, x, y);
		break;
	    }
	}
	else
	{
	    // no data yet 
	    nothing_popup.show(comp, x, y);
	}
	
	// cancel the drag that will have been detected by this mouse event....
	drag_gesture_recogniser.resetRecognizer();
	
    }    

    public class PanelMouseListener implements MouseListener
    {
	private boolean showPopupIfTrigger(MouseEvent e)
	{
	    if(e.isPopupTrigger() || e.isAltDown() || e.isMetaDown() || e.isAltGraphDown()) 
	    {
		showMenu( e.getComponent(), e.getX(), e.getY() );
		return true;
	    }
	    else
	    {
		return false;
	    }
	}
	
	public void mousePressed(MouseEvent e) 
	{
	    showPopupIfTrigger(e);
	}

	
	public void mouseReleased(MouseEvent e) 
	{
	    // showPopupIfTrigger(e);
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	    // if(e.getClickCount() == 2)
	    if( showPopupIfTrigger(e) )
		return;

	    //if(e.getClickCount() == 2)
	    {
		//System.out.println( "select!" );
		if(cur_pos == null)
		{    
		    try
		    {
			cur_pos = getThingPosUnderMouse(e.getX(), e.getY());
		    }
		    catch(NothingUnderMouse num)
		    {
			return;
		    }
		}

		// ===== NameColElement ====== change Spot selection

		if(cur_pos.element == NameColElement)
		{
		    int spot_id = edata.getSpotAtIndex(cur_pos.data_row);
		    
		    if(e.isShiftDown())
		    {
			if(last_select_spot_id >= 0)
			{
			    boolean toggle = (e.isControlDown());
			    
			    // System.out.println( "extend select! start was spot id = " + last_select_spot_id );
			    
			    // need to extend the selection using the current traversal not the spot_id's
			    //
			    
			    int start_pos = edata.getIndexOfSpot( last_select_spot_id );
			    int end_pos   = edata.getIndexOfSpot( spot_id );
			    
			    if(start_pos > end_pos)
			    {
				int tmp = start_pos;
				start_pos = end_pos - 1;
				end_pos = tmp - 1;
			    }
			    
			    if(start_pos < end_pos)
			    {
				for(int p = start_pos + 1; p <= end_pos; p++)
				{
				    final int pi = edata.getSpotAtIndex( p );
				    if((!apply_filter) || (!edata.filter(pi)))
				    {
					// make sure 'notify' is false to keep updates to a minimum
					if(toggle)
					    edata.setSpotSelected(  pi, ! edata.isSpotSelected( pi ), false);
					else
					    edata.setSpotSelected(  pi, true, false);
				    }
				    
				}
				edata.notifySelectionListeners(ExprData.SpotSelection);
			    }
			}
			else
			{
			    int[] ssel = new int[1];
			    ssel[0] = spot_id;
			    edata.setSpotSelection(ssel);
			}
		    }
		    else
		    {
			// not shift down....

			if(e.isControlDown())
			{
			    // no shift, yes ctrl -> single toggle
			    edata.setSpotSelected(  spot_id, ! edata.isSpotSelected( spot_id ));
			}
			else
			{
			    // no shift, no toggle -> single select
			    int[] ssel = new int[1];
			    ssel[0] = spot_id;
			    edata.setSpotSelection(ssel);
			}
		    }

		    last_select_spot_id  = spot_id;
		    
		    return;
		    // repaint();
		}

		// ===== DataElement ====== change both Spot and Measurement selections
		
		if(cur_pos.element == DataElement)
		{
		    int spot_id = edata.getSpotAtIndex(cur_pos.data_row);
		    int meas_id = cur_pos.data_col;
		    
		    if(e.isControlDown())
		    {
			// toggle
			edata.setSpotSelected(  spot_id, ! edata.isSpotSelected( spot_id ));
			edata.setMeasurementSelected(  meas_id, ! edata.isMeasurementSelected( meas_id ));
		    }
		    else
		    {
			if(e.isShiftDown() && (last_select_spot_id >= 0))
			{
			    // System.out.println( "extend select! start was spot id = " + last_select_spot_id );
			    
			    // need to extend the selection using the current traversal not the spot_id's
			    //
			    
			    int start_pos = edata.getIndexOfSpot( last_select_spot_id );
			    int end_pos   = edata.getIndexOfSpot( spot_id );
			    
			    if(start_pos > end_pos)
			    {
				int tmp = start_pos;
				start_pos = end_pos - 1;
				end_pos = tmp - 1;
			    }
			    
			    if(start_pos < end_pos)
			    {
				for(int p = start_pos + 1; p <= end_pos; p++)
				{
				    final int pi = edata.getSpotAtIndex( p );
				    if((!apply_filter) || (!edata.filter(pi)))
				    {
					// make sure 'notify' is false to keep updates to a minimum
					edata.setSpotSelected(  pi, ! edata.isSpotSelected( pi ), false);
				    }
				    
				}
			    }
			    
			    start_pos = edata.getIndexOfMeasurement( last_select_meas_id );
			    end_pos   = edata.getIndexOfMeasurement( meas_id );
			    
			    if(start_pos > end_pos)
			    {
				int tmp = start_pos;
				start_pos = end_pos - 1;
				end_pos = tmp - 1;
			    }
			    
			    if(start_pos < end_pos)
			    {
				for(int p = start_pos + 1; p <= end_pos; p++)
				{
				    final int pi = edata.getMeasurementAtIndex( p );
				    if(edata.getMeasurementShow(pi))
				    {
					// make sure 'notify' is false to keep updates to a minimum
					edata.setMeasurementSelected(  pi, ! edata.isMeasurementSelected( pi ), false);
				    }
				}
				
			    }

			    edata.notifySelectionListeners(ExprData.SpotMeasurementSelection);
			}
			else
			{
			    int[] ssel = new int[1];
			    ssel[0] = spot_id;
			    edata.setSpotSelection(ssel);
			    
			    int[] msel = new int[1];
			    msel[0] = meas_id;
			    edata.setMeasurementSelection(msel);
			}
		    }

		    last_select_meas_id  = meas_id;
		    last_select_spot_id  = spot_id;
		    
		    return;
		    // repaint();
		}

		// ===== MeasurementNameElement ====== change Measurement selection

		if(cur_pos.element == MeasurementNameElement)
		{
		    int meas_id = cur_pos.data_col;
		    
		    // System.out.println( "select! m name =" + edata.getMeasurementName(meas_id));

		    if(e.isControlDown())
		    {
			// toggle
			edata.setMeasurementSelected(  meas_id, ! edata.isMeasurementSelected( meas_id ));
		    }
		    else
		    {
			if(e.isShiftDown() && (last_select_meas_id >= 0))
			{
			    // System.out.println( "extend select! start was spot id = " + last_select_spot_id );
			    
			    // need to extend the selection using the current traversal not the spot_id's
			    //
			    
			    int start_pos = edata.getIndexOfMeasurement( last_select_meas_id );
			    int end_pos   = edata.getIndexOfMeasurement( meas_id );
			    
			    if(start_pos > end_pos)
			    {
				int tmp = start_pos;
				start_pos = end_pos - 1;
				end_pos = tmp - 1;
			    }
			    
			    if(start_pos < end_pos)
			    {
				for(int p = start_pos + 1; p <= end_pos; p++)
				{
				    final int pi = edata.getMeasurementAtIndex( p );
				    if(edata.getMeasurementShow(pi))
				    {
					// make sure 'notify' is false to keep updates to a minimum
					edata.setMeasurementSelected(  pi, ! edata.isMeasurementSelected( pi ), false);
				    }
				}
				edata.notifySelectionListeners(ExprData.MeasurementSelection);
			    }
			}
			else
			{
			    int[] msel = new int[1];
			    msel[0] = meas_id;
			    edata.setMeasurementSelection(msel);
			}
		    }

		    last_select_meas_id  = meas_id;
		    
		    return;
		    // repaint();
		}

		// ===== ClusterElement ====== change Spot or Measurement cluster selection

		if((cur_pos != null) && ((cur_pos.element == ClusterElement) || (cur_pos.element == ClusterBranch)))
		{
		    if(cur_pos.cluster != null)
		    {
			if(e.isShiftDown())
			{
			    edata.setClusterSelected( cur_pos.cluster, true );
			    repaint();
			}
			else
			{
			    if(e.isControlDown())
			    {  
				edata.setClusterSelected( cur_pos.cluster, ! edata.isClusterSelected( cur_pos.cluster ));
				repaint();
			    }
			    else
			    {
				if( cur_pos.cluster.getIsSpot() )
				    edata.unselectSpotClusters();
				else
				    edata.unselectMeasurementClusters();

				edata.setClusterSelected( cur_pos.cluster, true );
				repaint();
			    }
			}
		    }
		}

		
	    }
	    //else
	    //showPopupIfTrigger(e);
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	
	}

	public void mouseExited(MouseEvent e) 
	{
	    //dplot_panel.highlightCurrentSpot(-1); // removes any old highlighting
	    mview.setMessage(null, false);
	}


	private int last_select_spot_id = -1;
	private int last_select_meas_id = -1;
    }
    
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // finally, the dataplot panel itself....
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    public DragGestureRecognizer drag_gesture_recogniser = null;

 
    public class DataPlotPanel extends JPanel 
	                       implements java.awt.print.Printable, 
                                          ComponentListener,
                   			  DropTargetListener,DragSourceListener,DragGestureListener,
                   			  ExprData.ExternalSelectionListener
    { 
	private DataPlot dplot;

	public DragSource drag_source = null;
	public DropTarget drop_target = null;
 	private int sel_handle;

	//public boolean isFocusTraversable() { return true; }

	public DataPlotPanel(DataPlot dplot_)
	{ 
	    //super(false);

	    dplot = dplot_;
	    
	    addComponentListener(this);

	    sel_handle = edata.addExternalSelectionListener(this);

	    drop_target = new DropTarget (this, this);
	    drag_source = DragSource.getDefaultDragSource();
	    drag_gesture_recogniser = drag_source.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);

	    addMouseListener(new PanelMouseListener());
	    addMouseMotionListener(new PanelMouseMotionListener());

	}

	private int printer_page_count = 1;

	//
	// === drop target listener ==============================================
	//
	
	public void dragEnter (DropTargetDragEvent event) 
	{
	    //System.out.println( "DropTarget:dragEnter");
	    event.acceptDrag (DnDConstants.ACTION_MOVE);
	}
	
	public void dragExit (DropTargetEvent event) 
	{
	    //System.out.println( "DropTarget:dragExit");
	}
	
	public void dragOver (DropTargetDragEvent event) 
	{
	    //System.out.println( "DropTarget:dragOver");
	    //event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
	}
	
	public void drop (DropTargetDropEvent event) 
	{
	    System.out.println( "DropTarget:drop attempted");

	    try 
	    {
		Transferable transferable = event.getTransferable();
		
		DragAndDropEntity dnde = null;
		String text = null;
		
		if(event.isLocalTransfer() == false)
		{
		    System.out.println( "transfer between apps...");
		    event.rejectDrop();
		}
		else
		{
		    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
		    {
			event.acceptDrop(DnDConstants.ACTION_MOVE);
			
			dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);
			
			System.out.println( "'" + dnde.getEntityType() + "' dropped");
			System.out.println( "name is '" + dnde.toString() + "'");
			
			try
			{
			    int[] sid_a = dnde.getSpotIds();
			    
			    //System.out.println( "spot! id=" + sid);
			    
			    edata.setSpotSelection(sid_a);

			    if(sid_a.length > 0)
				displaySpot(sid_a[0]);
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			    // maybe it was a cluster?
			    
			    try
			    {
				ExprData.Cluster cl = dnde.getCluster();
				
				//System.out.println( "cluster! " + cl.getName());
				
				int[] els = cl.getElements();
				
				if((els != null) && (els.length >= 0))
				{
				    edata.setSpotSelection(els);
				    displaySpot(els[0]);
				}
			    }
			    catch(DragAndDropEntity.WrongEntityException another_wee)
			    {
				// or maybe it was a measurement name?
			    }
			}
			event.getDropTargetContext().dropComplete(true);

			System.out.println( "drop complete");
			
		    }
		    else
		    {
			event.rejectDrop();
		    }
		}
	    }
	    catch (IOException exception) 
	    {
		exception.printStackTrace();
		System.err.println( "Exception" + exception.getMessage());
	    } 
	    catch (UnsupportedFlavorException ufException ) 
	    {
		ufException.printStackTrace();
		System.err.println( "Exception" + ufException.getMessage());
	    }

	   
	}
	
	public void dropActionChanged ( DropTargetDragEvent event ) 
	{
	    //System.out.println( "DropTarget:dropActionChanged");
	}
 
	//
	// === DragGesture listener ==============================================
	//

	public void dragGestureRecognized( DragGestureEvent event) 
	{
	    // hide anything that might be a Window (i.e. the popups and tooltips)
	    //
	    // (this might fix the broken drag problem)
	    //
	    // System.out.println("dragGestureRecognized() ?");

	    if(cur_pos == null)
		return;	   

	    DragAndDropEntity dnde = null;

	    switch(cur_pos.element)
	    {
	    case ClusterElement:
	    case ClusterBranch:
		dnde = DragAndDropEntity.createClusterEntity(cur_pos.cluster);
		break;
		
	    case MeasurementNameElement:
		int[] ms = edata.getMeasurementSelection();
		if(ms.length > 0)
		{
		    dnde = DragAndDropEntity.createMeasurementNamesEntity(ms);
		}
		else
		{
		    dnde = DragAndDropEntity.createMeasurementNameEntity(cur_pos.data_col);
		}
		break;
		/*
	    case SpotCommentElement:
		{
		    int sid = edata.getSpotAtIndex(cur_pos.data_row);
		    if(edata.getSpotComment(sid) != null)
			dnde = DragAndDropEntity.createSpotCommentEntity(sid);
		}
		break;
		*/
	    case DataElement:
	    case NameColElement:
		{
		    int[] ss = edata.getSpotSelection();
		    if(ss.length > 0)
		    {
			dnde = DragAndDropEntity.createSpotNamesEntity(ss);
		    }
		    else
		    {
			int sid = edata.getSpotAtIndex(cur_pos.data_row);
			
			dnde = DragAndDropEntity.createGeneNameEntity(sid);
		    }
		    
		    /*
		    switch(row_label_src)
		    {
		    case 1:  // probe name
			dnde = DragAndDropEntity.createProbeNameEntity(sid);
			break;
		    case 2: // gene name
		    dnde = DragAndDropEntity.createGeneNameEntity(sid);
		    break;
		    case 3: // spot name
		    dnde = DragAndDropEntity.createSpotNameEntity(sid);
		    break;
		    }
		    */

		}
		break;
	    }
	    
	    if(dnde != null) 
	    {
		//System.out.println( " drag start....(7)");
		
		try
		{
		    //  System.out.println("...hiding windows");
		    /*
		    if(spot_name_popup != null)
			if(spot_name_popup.isVisible())
			    spot_name_popup.setVisible(false);
		    if(cluster_popup.isVisible())
			cluster_popup.setVisible(false);
		    if(spot_popup.isVisible())
			spot_popup.setVisible(false);
		    if(meas_name_popup.isVisible())
			meas_name_popup.setVisible(false);
		    if(nothing_popup.isVisible())
			nothing_popup.setVisible(false);
		    //setToolTipText(null);
		    */

		    drag_source.startDrag (event, DragSource.DefaultMoveDrop, dnde, this);
		}
		catch(java.awt.dnd.InvalidDnDOperationException ide)
		{
		    System.out.println( " BAD drag!\n" + ide);

		    System.out.println( " trying to restart DnD system");

		    drag_gesture_recogniser.resetRecognizer();

		    /*
		    drag_gesture_recogniser.unregisterListeners();

		    //drag_source = DragSource.getDefaultDragSource();
		    
		    drag_gesture_recogniser = drag_source.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);
		    */

		    
		    
		}
	    }
	    else
	    {
		System.out.println( " null drag....");
	    }

	}

	//
	// === DragSource listener ==============================================
	//

	public void dragDropEnd (DragSourceDropEvent event) 
	{   
	    //System.out.println( "DragSource:dragDropEnd");
	}
	
	public void dragEnter (DragSourceDragEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dragEnter");
	}
	
	public void dragExit (DragSourceEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dragExit");
	}
	
	public void dragOver (DragSourceDragEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dragOver");
	}
	
	public void dropActionChanged ( DragSourceDragEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dropActionChanged");
	}

	//
	// === component listener ==============================================
	//

	public void componentResized(ComponentEvent e) 
	{
    	    updateDisplay();

	    // System.out.println("resize....");

	    logo_ixp = (getWidth()  - logo_iw)/2;
	    logo_iyp = (getHeight() - logo_ih)/2 - 16;

	    logo_mxp = logo_ixp;
	    logo_myp = logo_iyp + logo_ih + 5;
	}

	public void componentHidden(ComponentEvent e) 
	{
	}
	
	public void componentMoved(ComponentEvent e) 
	{
	}
	

	public void componentShown(ComponentEvent e) 
	{
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
	    repaint();
	}
	
	public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }
	public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
 
	//
	// =============================================================
	//

	private int logo_ixp, logo_iyp, logo_iw, logo_ih;           // logo image
	private int logo_mxp, logo_myp, logo_mh, logo_mw, logo_tp;  // logo message
	private int logo_cx, logo_cy;
	private int[] logo_chunk_seq;
	private Image welcome_offscreen_buffer = null;
	private boolean currently_painting_logo = false;
	
	final int logo_chunk_xs = 5;
	final int logo_chunk_ys = 3;
	
	private int welcome_status_message_ticker = 0;
	private int welcome_status_message_count = 0;

	public void removeWelcomeMessage()
	{
	    logo_painter = logo_paint_chunks;
	    if(edata.getNumSpots() == 0)
	    {
		Graphics g = getGraphics();
		g.setColor(background_col);
		g.fillRect(logo_mxp, logo_myp, logo_iw, logo_mh);
		g.drawImage(logo_ii.getImage(), logo_ixp, logo_iyp, null);
	    }
	}

	private void repaintWelcomeMessage( Graphics g)
	{
	    if((logo_painter < logo_paint_chunks) && (logo_painter >= 0) && (logo_painter < logo_chunk_seq.length))
	    {
		for(int lp=0; lp < logo_painter; lp++)
		{
		    int px = (logo_chunk_seq[lp] % logo_cx) * logo_chunk_xs;
		    int py = (logo_chunk_seq[lp] / logo_cx) * logo_chunk_ys;
		    
		    g.drawImage(logo_ii.getImage(), 
				logo_ixp + px, logo_iyp + py, logo_ixp + px + logo_chunk_xs, logo_iyp + py + logo_chunk_ys,
				px, py, px + logo_chunk_xs, py + logo_chunk_ys, 
				null);
		}
	    }
	}

	private void setupWelcomeMessage()
	{
	    logo_pos = 0;
	    
	    logo_cx = (int)(Math.ceil((double)logo_iw / (double)logo_chunk_xs));
	    logo_cy = (int)(Math.ceil((double)logo_ih / (double)logo_chunk_ys));
	    
	    logo_paint_chunks = logo_cx * logo_cy;
	    
	    logo_chunk_seq = new int[logo_paint_chunks];

	    
	    int dir_mode = mview.getIntProperty("DataPlot.welcome_dir_mode", -1);

	    if(++dir_mode > 4)
		dir_mode = 0;

	    mview.putIntProperty("DataPlot.welcome_dir_mode", dir_mode);

	    double max_offset_d = (dir_mode < 2) ? (double) (logo_cx*3) : (double) (logo_cy*6);

	    int pos = 0;

	    // generate the visiting order for the blocks that make up the image...

	    switch(dir_mode)
	    {
	    case 0:
		for(int y=0; y < logo_cy; y++)
		    for(int x=0; x < logo_cx; x++)
			logo_chunk_seq[pos++] = (y * logo_cx) + x;
		break;

	    case 1:
		for(int y=(logo_cy-1); y >= 0; y--)
		    for(int x=0; x < logo_cx; x++)
			logo_chunk_seq[pos++] = (y * logo_cx) + x;
		break;

	    case 2:
		for(int x=0; x < logo_cx; x++)
		    for(int y=0; y < logo_cy; y++)
			logo_chunk_seq[pos++] = (y * logo_cx) + x;
		break;

	    case 3:
		for(int x=(logo_cx-1); x >= 0; x--)
		    for(int y=0; y < logo_cy; y++)
			logo_chunk_seq[pos++] = (y * logo_cx) + x;
		break;

	    case 4: // diagonal is a bit trickier....
		final int step = 1 + (int) (Math.random() * 6);
		
		int[] xp = new int[logo_cy];
		int[] xd = new int[logo_cy];
		
		for(int y=0; y < logo_cy; y++)
		    xp[y] = -(y * step);
		
		max_offset_d = (double) (logo_cy * 4);

		boolean done = false;
		while(!done)
		{
		    done = true;
		    
		    for(int y=0; y < logo_cy; y++)
		    {
			if( xp[y] >= 0)
			{
			    if( xp[y] < logo_cx )
			    {
				done = false;
				logo_chunk_seq[pos++] = (y * logo_cx) + xp[y];
			    }
			    
			}
			xp[y]++;
		    }
		}
		break;
	    }

	    // randomly shuffle the ordering a bit...

	    final double logo_paint_chunks_d = (double) logo_paint_chunks;

	    for(int s=0; s < logo_paint_chunks; s++)
	    {
		final int isrc  = (int)(Math.random() * logo_paint_chunks_d);
		
		final int shift = (int)(Math.random() * max_offset_d);
		
		final int idest = isrc - shift;

		if(idest > 0)
		{
		    final int tmp = logo_chunk_seq[isrc];
		    logo_chunk_seq[isrc] = logo_chunk_seq[idest];
		    logo_chunk_seq[idest] = tmp;
		}
	    }
	    
	}
	
	private void updateWelcomeMessage()
	{
	    // prevent reentrant calling of this method....
	    currently_painting_logo = true;

	    Graphics g = getGraphics();
		
	    if(welcome_status_message_ticker <= 0)
	    {
		welcome_status_message_ticker = 30;
		
		switch(welcome_status_message_count)
		{
		
		case 0:
		    GregorianCalendar gc = new GregorianCalendar();
		    int hour24 = gc.get(Calendar.HOUR_OF_DAY);

		    // int summer_time = gc.get(Calendar.DST_OFFSET);
		    // int zone_off = gc.get(Calendar.ZONE_OFFSET);

		    String time = "Good ";

		    if((hour24 >= 4) && (hour24 < 12))
			time += "morning";
		    if((hour24 >= 12) && (hour24 < 18))
			time += "afternoon";
		    if((hour24 >= 18) || (hour24 < 4))
			time += "evening";

		    mview.setMessage( time, false );
		    break;

		case 1:
		    mview.setMessage( mview.getAppTitle(), false );
		    break;

		case 2:
		    mview.setMessage( "Copyright \u00A9 2000-2005 David Hancock & The University of Manchester's Microarray Bioinformatics Group", false);
		    break;

		case 3:
		    mview.setMessage( "Run count: " + mview.getAppRunCount(), false );
		    break;

		case 4:
		    mview.setMessage( "JVM: " + 
				      System.getProperty("java.vm.version") +
				      " from " +
				      System.getProperty("java.vm.vendor"), false );
		    break;

		case 5:
		    mview.setMessage( "OS: " + 
				      System.getProperty("os.name") +
				      " " +
				      System.getProperty("os.version") +
				      " on " +
				      System.getProperty("os.arch"), false );
		    break;
		
		}
		
		if(++welcome_status_message_count > 5)
		    welcome_status_message_count = 1;

	    }
	    else
	    {
		welcome_status_message_ticker--;
	    }

	    if(welcome_offscreen_buffer == null)
	    {
		Font logo_of = g.getFont();
		Font logo_nf = new Font("Helvetica", Font.BOLD, 18);
		
		g.setFont(logo_nf);
		FontMetrics fm = g.getFontMetrics();
		logo_tp = fm.getAscent();
		logo_mh = logo_tp + fm.getDescent();
		logo_mw = fm.stringWidth(welcome) + logo_iw;  //include a blank gap at the start
		g.setFont(logo_of);

		welcome_offscreen_buffer = createImage(logo_mw, logo_mh);

		Graphics og = welcome_offscreen_buffer.getGraphics();

		if(og != null)
		{
		    og.setFont(logo_nf);
		    
		    if(background_col != null)
		    {
			//logo_iw = logo_ii.getIconWidth();
			//logo_ixp = (getWidth()  - logo_iw)/2;
		
			og.setColor(background_col);
			og.fillRect(0,0,logo_mw,logo_mh);
			
			// shadow
			og.setColor(background_col.darker());
			og.drawString(welcome, logo_iw-2, logo_tp);
			og.drawString(welcome, logo_iw, logo_tp+2);
			
			// highlight
			og.setColor(background_col.brighter());
			og.drawString(welcome, logo_iw+1, logo_tp);
			og.drawString(welcome, logo_iw, logo_tp-1);
			
			// body
			og.setColor(text_col);
			og.drawString(welcome, logo_iw, logo_tp);
		    }
		}
		
		// System.out.println("drawn welcome text");
	    }

    
	    if(!stop_welcome_animation)
	    {
		if((logo_painter < logo_paint_chunks) && (logo_painter >= 0))
		{
		    // pick a chunk to paint..
		    
		    for(int c=0; c < 40; c++)
		    {
			if(logo_painter < logo_chunk_seq.length)
			{
			    int px = (logo_chunk_seq[logo_painter] % logo_cx) * logo_chunk_xs;
			    int py = (logo_chunk_seq[logo_painter] / logo_cx) * logo_chunk_ys;
			    
			    // and paint it
			    
			    if( logo_ii != null )
			    {
				Image iii = logo_ii.getImage();

				if( iii != null )
				{
				    g.drawImage( iii , 
				    		 logo_ixp + px, logo_iyp + py, 
						 logo_ixp + px + logo_chunk_xs, logo_iyp + py + logo_chunk_ys,
				    		 px, py, px + logo_chunk_xs, py + logo_chunk_ys, 
				    		 null );
				}
			    }
			}
			logo_painter++;
		    }
		}
		//if(logo_painter == logo_paint_chunks)
		//    g.drawImage(logo_ii.getImage(), logo_ixp, logo_iyp, null);

		
		logo_iw = logo_ii.getIconWidth();
		logo_ih = logo_ii.getIconHeight();

		logo_ixp = (getWidth()  - logo_iw)/2;
		logo_iyp = (getHeight() - logo_ih)/2 - 16;
		
		logo_mxp = logo_ixp;
		logo_myp = logo_iyp + logo_ih + 5;
		
		int leftlen = logo_iw;
		// int rightlen = 
		
		int nsteps = logo_mh / 2;
		//final int stepl = 2;
		
		int xp = logo_ixp; /// (getWidth() - logo_iw)/2; 
		int yp = logo_myp; // logo_iyp + logo_ih; // logo_myp;

		// the LHS compression steps

                /*

                  drawImage() arguments:

    		     dx1,dy1 - the x,y coordinates of the first corner of the destination rectangle.
    		     dx2,dy2 - the y,y coordinates of the second corner of the destination rectangle.
    		     sx1,sy1 - the x,y coordinates of the first corner of the source rectangle.
    		     sx2,sy2 - the x,y coordinates of the second corner of the source rectangle.

                */

		int usedx = 0;
		for(int i=1; i < nsteps; i++)
		{
		    int iyp = nsteps - i; // (2*nsteps)-(i*2);
		    int stepl = i / 2;
		    if(stepl < 1)
			stepl = 1;
		    if(stepl > 3)
			stepl = 3;

		    //System.out.println( xp + "," +  (logo_myp+iyp) + "," +
		    //			(xp+stepl) + "," + (logo_myp+logo_mh-iyp) + "," +
		    //			(logo_pos+usedx) + "," + 0 + "," +
		    //			(logo_pos+usedx+stepl) + "," + logo_mh );

		    if(logo_pos+usedx < logo_mw)
			g.drawImage(welcome_offscreen_buffer, 
				    xp, logo_myp+iyp, 
				    xp+stepl, logo_myp+logo_mh-iyp,
				    logo_pos+usedx, 0, 
				    logo_pos+usedx+stepl, logo_mh, 
				    null);

		    xp += stepl;
		    usedx += stepl;
		}
		

		g.drawImage(welcome_offscreen_buffer, 
			    logo_mxp+usedx, logo_myp, 
			    logo_mxp+leftlen-usedx, logo_myp+logo_mh,
			    logo_pos+usedx, 0, 
			    logo_pos+leftlen-usedx, logo_mh, 
			    null);

		
		// and the RHS compression
		xp = logo_mxp+leftlen-usedx;
		usedx = leftlen-usedx;

		for(int i=0; i < nsteps; i++)
		{
		    int iyp = i;
		    int stepl = (nsteps-i) / 2;
		    if(stepl < 1)
			stepl = 1;
		    if(stepl > 3)
			stepl = 3;

		    if(logo_pos+usedx < logo_mw)
			g.drawImage(welcome_offscreen_buffer, 
				    xp, logo_myp+iyp, 
				    xp+stepl, logo_myp+logo_mh-iyp,
				    logo_pos+usedx, 0, 
				    logo_pos+usedx+stepl, logo_mh, 
				    null);

		    xp += stepl;
		    usedx += stepl;
		}


		// and scroll...
		if((logo_pos += 5) >= logo_mw)
		    logo_pos = 0; 	    
	    }

	    currently_painting_logo = false;

	}

	// ------------------------------------------------------------------------------------------
	//
	//
	// ==========  Printing  ====================================================================
	//
	//
	// ------------------------------------------------------------------------------------------

	private int printer_first_spot = 0;
	private boolean printer_spot_limit_enabled = false;
	private int printer_spot_limit = 0;
	

	// ------------------------------------------------------------------------------------------
	//
	// printing to an image file
	//
	// ------------------------------------------------------------------------------------------

	public class ImageSaveThread extends Thread
	{
	    public ImageSaveThread( final PrintManager.PrintInfo pi_, final ProgressOMeter pm_, final int old_scroll_mode_)
	    {
		pi = pi_;
		pm = pm_;
		old_scroll_mode = old_scroll_mode_;
	    }

	    public void run()
	    {
		doImageSave( pi, pm, old_scroll_mode );
	    }
	    private int old_scroll_mode;
	    private ProgressOMeter pm;
	    private PrintManager.PrintInfo pi;

	}

	private void doImageSave( final PrintManager.PrintInfo pi, final ProgressOMeter pm, final int old_scroll_mode )
	{
	    /*
	    try
	    {
		Thread.sleep(1000);
	    }
	    catch(InterruptedException ie)
	    {
	    }
	    */

	    try
	    {
		int printer_n_spots_expected = edata.getNumSpots() - filtered_count;
		
		int w = first_line_leftoff + (n_visible_meas * colskip) + spot_cluster_w;
		int h = first_line_topoff + ( edata.getNumSpots() * rowskip );
		
		System.out.println( printer_n_spots_expected + " spots to print, w=" + w + " h=" + h );
		
		//System.out.println(" total image is " + w + "x" + h);
		
		scroll_horiz_offset = 0;
		
		if(pm != null)
		    pm.setMessage( "Drawing " + w + "x" + h + " image" );
		
		
		BufferedImage img = (BufferedImage) createImage(w, h);
		
		Graphics g = img.getGraphics();
		g.setClip(0,0,w,h);
		
		System.out.println(" drawing data...");
		
		dplot_panel.paintRegion(g, 2, 0);
		
		System.out.println("...data drawn");
		
		try
		{
		    System.out.println("...writing '" + pi.image_format + "' to " + pi.destination.getPath() );
		    
		    javax.imageio.ImageIO.write( img, pi.image_format, pi.destination );
		    
		    System.out.println("...written" );
		}
		catch( java.io.IOException ioe )
		{
		    //pm.stopIt();
		    mview.alertMessage( "Unable to write the image\n\n" + ioe.getMessage() );
		}
		
	    }
	    catch( java.lang.OutOfMemoryError oome )
	    {
		System.gc();
		mview.alertMessage( "Insufficient memory available to create the image." );
	    }
	    
	    scroll_mode = old_scroll_mode;
	    
	    //if(pm != null)
	    //	pm.stopIt();
	    
	    System.out.println("preparing to updateDisplay()..." );
	    
	    updateDisplay();
	    
	    System.out.println("...display updated" );
	}


	// ------------------------------------------------------------------------------------------
	//
	// printing to hardcopy
	//
	// ------------------------------------------------------------------------------------------
	
	public class PrinterThread extends Thread
	{
	    PrinterJob job;
	    ProgressOMeter pm;

	    public PrinterThread( PrinterJob job, final ProgressOMeter pm )
	    {
		this.job = job;
		this.pm  = pm;
	    }
	    
	    public void run()
	    {
		int old_scroll_mode = scroll_mode;
		scroll_mode = 0;  // unlock both names and clusters
		updateDisplay();
		
		try 
		{
		    job.print();

		    pm.stopIt();
		}
		catch (PrinterAbortException ex) 
		{
		    pm.stopIt();
		    mview.alertMessage("Print job aborted.");
		}
		catch (PrinterIOException ex) 
		{
		    pm.stopIt();
		    mview.alertMessage("Unable to print:\n\n" + ex);
		}
		catch (Exception ex) 
		{
		    pm.stopIt();
		    mview.alertMessage("Unable to print:\n\n" + ex);
		}
		
		scroll_mode = old_scroll_mode;

		updateDisplay();
	    }
	}

	// ------------------------------------------------------------------------------------------
	//
	// print() implements the java.awt.Printable interface for printing 
	//
	// ------------------------------------------------------------------------------------------

	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    if(pg_num >= printer_page_count) 
	    {
		return Printable.NO_SUCH_PAGE;
	    }
	    
	    // area of one page
	    //
	    int pw = (int)pf.getImageableWidth();
	    int ph = (int)pf.getImageableHeight();

	    System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    // total width, height of all spots to be output
	    //
	    //int w = getWidth(); // first_line_leftoff + (n_visible_meas * colskip);

	    //
	    //

	    int printer_n_spots_expected = edata.getNumSpots() - filtered_count;

	    double n_spots_per_page = (double)(ph - first_line_topoff ) / (double)rowskip;

	    int w = first_line_leftoff + (n_visible_meas * colskip) + spot_cluster_w;
	    int n_h_pages = (int)(Math.ceil((double) w / (double) pw));

	    int h = (first_line_topoff + ((int)n_spots_per_page * rowskip)) * n_h_pages;
	    int n_v_pages = (int)(Math.ceil((double) printer_n_spots_expected / n_spots_per_page));

	    
	    int n_pages = n_v_pages * n_h_pages;

	    
	    /*
	    if(printer_print_single_page)
	    {
		System.out.println(" single mode page");

		Paper cp = pf.getPaper();
		Paper pa = new Paper();

		pw = w;

		h =  first_line_topoff + (printer_n_spots_expected * rowskip);
		ph = first_line_topoff + (printer_n_spots_expected * rowskip);

		pa.setImageableArea(cp.getImageableX(), cp.getImageableY(), (double)w, (double)h);
		pf.setPaper( pa );

		n_pages = 1;
		n_h_pages = n_v_pages = 1;
	    }
	    */

	    System.out.println(" total image is " + w + "x" + h + 
			       /*" (for " + printer_n_spots_expected + " spots,\n" + 
				 "  starting from number " + printer_first_spot + ",\n" + */
			       "  with " + n_spots_per_page + " spots/page)");
	    System.out.println(" total pages = " + n_pages + " as " + n_v_pages + " rows and " + n_h_pages + " cols");
	    
	    if (w == 0 || h == 0)
		return NO_SUCH_PAGE;
	    
	    // total cols, rows of pages given the above sizes
	    //
	    //int nCol = Math.max( (int) Math.ceil( (double) w / pw), 1);
	    //int nRow = Math.max( (int) Math.ceil( (double) h / ph), 1);
	    
	    // as we currently can't scroll sideways, limit the number
	    // of columns to 1
	    //
	    //nCol = 1;


	    // this is used to work out when we have finished
	    //
	    printer_page_count = n_pages;

	    //
	    // what page are we printing given the current page number?
	    //
	    //  index the pages like this:
	    //
	    //     0  1  2
	    //     3  4  5
            //     6  7  8
	    //
	    // (so we can ignore anything after the first column)
	    //

	    int iCol = pg_num % n_h_pages;
	    int iRow = pg_num / n_h_pages;
	    
	    System.out.println("  page at col " + iCol + ", row " + iRow);

	    // position and size of the image region that should
	    // be drawn for this page
	    //
	    int ix = iCol*pw;
	    int iy = iRow*ph;
	    
	    // trim the side/bottom of the edge pages
	    //
	    int iw = pw;//Math.min(pw, w-ix);
	    int ih = ph;//Math.min(ph, h-iy);

	    System.out.println("  print " + iw + "x" + ih + " at " + ix + "," + iy);

	    
	    // work out what is the spot at the top of this page...
	    //
	    
	    // what is the spots/per count for this size?
	    // spots_per_page = (int)(Math.ceil((float)(ih - first_line_topoff) / (float)rowskip));

	    //int spot_at_top_of_page = (iRow * spots_per_page);

	    int spot_at_top_of_page = printer_first_spot + (iRow * (int)(Math.floor(n_spots_per_page)));

	    System.out.println("  start at spot number " + spot_at_top_of_page);

	    // which index would this be given the current ordering and filtering situation....
	    
	    int search_spot = 0;
	    int count = 0;
	    while(count < spot_at_top_of_page)
	    {
		if(!edata.filter(edata.getSpotAtIndex(search_spot)))
		    count++;
		search_spot++;
	    }

	    if(search_spot > edata.getNumSpots())
	    {
		System.out.println("print(): WEIRD! cannot locate spot index " + spot_at_top_of_page + "\n" + 
				   "  has the data or any filters changed since the print run started?");
		search_spot  = edata.getNumSpots() - 1;
	    }

	    spot_at_top_of_page = search_spot;

	    System.out.println("  start at spot id " + spot_at_top_of_page);

	    // setup the scroll_horiz_offset to reflect which column this page is in
	    scroll_horiz_offset = pw * iCol;

	    g.translate((int)pf.getImageableX(), 
			(int)pf.getImageableY());

	    dplot_panel.paintRegion(g, 1, spot_at_top_of_page);
	    
	    System.gc();
	    
	    return Printable.PAGE_EXISTS;
	}


	// ------------------------------------------------------------------------------------------
	// end of printing section
	// ------------------------------------------------------------------------------------------


	public void spotQueried(int meas_id, int spot_id)
	{
	    
	}


	public String tool_tip_text = null;
	public String getToolTipText(MouseEvent event)
	{
	    return tool_tip_text;
	}

	
	public void paintComponent(Graphics g)
	{
	    // work out what the spot index at the top of the page should be
	    min_vis_spot = (current_ver_sb_value /*vert_sb.getValue()*/ - first_line_topoff) / rowskip;
	    if(min_vis_spot < 0)
		min_vis_spot = 0;
	    
	    //System.out.println(" paintComponent(): scrollbar at " + vert_sb.getValue() + ", line = " + min_vis_spot);
	    
	    if(scroll_mode == 3) // both locked
	    {
		min_vis_meas = current_hor_sb_value; // hor_sb.getValue();

		scroll_horiz_offset = 0;

		if(min_vis_meas < 0)
		    min_vis_meas = 0;
	    }
	    else
	    {
		min_vis_meas = 0;
		scroll_horiz_offset = current_hor_sb_value;
	    }

	    paintRegion(g, 0, min_vis_spot);
	}

	final boolean debug_paint = false;

	// modified to use the current clip bounds to decide how many rows and
	// cols to draw.....
	//
	// dest 0 --> window on image
	// dest 1 --> printer
	// dest 2 --> fullsize image

	private int paint_count = 0;

	private synchronized void paintRegion(Graphics g, int dest, int min_vis_spot_to_paint)
	{
	    //System.out.println("paintRegion(): " + (++paint_count) + " starts");
	    
	    // invalidate the current ThingUnderMouse
	    
	    cur_pos = null;

	    if(debug_clusters)
		System.out.println( "cluster debug info:" );

	    int meas, spot_index;
	    
	    int xp, yp;
	    
	    if(first_paint && (g != null))
	    { 
		first_paint = false;
		updateFonts(1 | 2);
		updateDisplay();
	    }

	    Rectangle r = g.getClipBounds();

    
	    int width  = (int)(r.getX() + r.getWidth());
	    int height = (int)(r.getY() + r.getHeight());
	    
	    Color actual_text_col = text_col;
	    Color actual_background_col = background_col;

	    switch(dest)
	    {
	    case 0: // window
		// make sure we paint all the spots to the screen as the painting
		// process also build the mouse-tracking data
		//
		height = getHeight();
		break;

	    case 1: // printer
		// only use the width of the screen when printing to
		// make the hardcopy output look more like the display
		//
		//if(printer_print_black_text)
		//    actual_text_col = Color.black;
		//if(printer_print_white_bg)
		//    actual_background_col = Color.white;
		break;

	    case 2: // fullsize
		spots_per_page = edata.getNumSpots();
		break;

	    }

		

	    /*
	    if(printer_spot_limit_enabled)
	    {
		if(spots_per_page > printer_spot_limit)
		    spots_per_page = printer_spot_limit;
	    }
	    */

	    //System.out.println(" viewport at " + pt.x + "," + pt.y + 
	    //  " size " + vis.width + "x" + vis.height );
	    
	    // frame.setVisible(false);ear the currently visible area of background 
	    
	    if(debug_paint)
	    {
		System.out.println("paintRegion():  clip bounds are " + r.getX() + "," + r.getY() + " size " +
				   r.getWidth() + "x" + r.getHeight() );
		
		System.out.println("paintRegion():  starting from spot index " + min_vis_spot_to_paint);
		
		System.out.println( "paintRegion(): allowing up to " + spots_per_page + " spots/page");
	    }
	    
	    g.setColor(actual_background_col);

	    g.fillRect(0, 0, width, height);  // getWidth(), getHeight());
	    
	    
	    
	    final int n_spots = edata.getNumSpots();

	    if(n_spots == 0)
	    {
		logo_iw = logo_ii.getIconWidth();
		logo_ih = logo_ii.getIconHeight();

		logo_ixp = (getWidth()  - logo_iw)/2;
		logo_iyp = (getHeight() - logo_ih)/2 - 16;

		if(logo_painter >= logo_paint_chunks)
		    g.drawImage(logo_ii.getImage(), logo_ixp, logo_iyp, null);
		else
		{
		    // System.out.println("repaint(): ticker has been reset from " + logo_painter);
		    repaintWelcomeMessage( g );
		    if(logo_painter < 0)
			logo_painter = 0;
		}

		if(logo_ticker == null)
		{
		    setupWelcomeMessage();
		    
		    //System.out.println(logo_paint_chunks + " chunks");

		    logo_ticker = new javax.swing.Timer(75, new ActionListener() 
			{
			    public void actionPerformed(ActionEvent evt) 
			    {
				if(!currently_painting_logo)
				    updateWelcomeMessage();
				//else
				//    System.out.println("overcall");
			    }
			    
			});
		}
		
		// System.out.println("ticker started");

		logo_ticker.start();

		return;
	    }
	    else
	    {
		if(logo_ticker != null)
		{
		    logo_ticker.stop();
		}
	    }

	    // make sure any highlighting is ignored and not repainted incorrectly later
	    // (because the display will now be different from how it was when the highlight was drawn)
	    //
	    //last_spot_highlight = -1;
	    
	    // max_spots_fit_in_window = (dplot_panel.getHeight() - first_line_topoff) / (rowskip);
	

	    // ========================================
	    // work out which measurements to draw
	    // ========================================

	    // n_visible_meas = 0;
	    
	    final int nmeas = edata.getNumMeasurements();
	    
	    meas_in_col  = new int[n_possible_meas];
	    col_for_meas_ht = new Hashtable();

	    // System.out.println("skipping " + min_vis_meas + " meas'");

	    n_visible_meas = 0;

	    //System.out.println("mvm=" + min_vis_meas + " nm=" + nmeas + " npm=" + n_possible_meas + " mic.l=" + meas_in_col.length);
	    
	    try
	    {
		
		for(int col=min_vis_meas; col < nmeas; col++)
		{
		    // use the current traversal order for Measurements...
		    int mi = edata.getMeasurementAtIndex(col);
		    
		    if(edata.getMeasurementShow(mi))
		    {  
			if(n_visible_meas < n_possible_meas)
			{
			    // System.out.println("col " +  n_visible_meas + " is meas " + mi);
			    
			    col_for_meas_ht.put(new Integer(mi), new Integer( n_visible_meas));
			    meas_in_col[n_visible_meas] = mi;
			    n_visible_meas++;
			    
			//System.out.print(n_visible_meas + "=" + mi + " ");
			}
		    }
		}
		//System.out.println();
		//System.out.println("nvm=" + n_visible_meas);
		
		// blank out any unused entries in the meas_in_col array
		int mc = n_visible_meas;
		while(mc < n_possible_meas)
		    meas_in_col[mc++] = -1;
		
		// System.out.println("there are " + n_visible_meas + " cols in use");
		
		// final int n_meas =  n_visible_meas; // edata.getNumMeasurements();
		
		// ========================================
		// draw measurement clusters
		// ========================================
		
		
		if(show_branches[0] || show_branches[1])
		{
		    cluster_h_lines   = new Hashtable();
		    cluster_v_lines   = new Hashtable();
		    cluster_glyph_pos = new Hashtable();
		}

		// at the top of the display are the Measurement clusters (if any)
		//

		meas_cluster_skip = 0;
		
		if(show_branches[1])
		{
		    cluster_h_lines   = new Hashtable();
		    cluster_v_lines   = new Hashtable();
		    cluster_glyph_pos = new Hashtable();
		    //ClustExt ce = drawMeasurementClusters(g, edata.getRootCluster(), border_gap);
		    
		    ClustContext cc = new ClustContext(g, 
						       meas_cluster_x_pos-scroll_horiz_offset-col_gap, border_gap, 
						       min_vis_meas, n_visible_meas, 
						       meas_cluster_x_pos-scroll_horiz_offset-col_gap+(n_visible_meas*colskip), 
						       border_gap+meas_cluster_h,
						       col_for_meas_ht);
		    
		    ExprData.Cluster rc = edata.getRootCluster();
		    Vector ch = rc.getChildren();
		    if(ch != null)
		    {
			final int meas_clust_sx = cc.b_off - (box_width/2);
			final int meas_clust_sy = cc.d_off;
			final int meas_clust_w  = cc.b_max - meas_clust_sx - (box_width/2);
			final int meas_clust_h  = cc.d_max - cc.d_off;
			
			// debug
			//cc.g.setColor( Color.blue );
			//cc.g.drawRect( meas_clust_sx, meas_clust_sy, meas_clust_w, meas_clust_h );
			// end debug
			
			// set the clip region to the official area devoted for the measurement clusters
			cc.g.setClip( meas_clust_sx, meas_clust_sy, meas_clust_w, meas_clust_h );
				
		    
			int ci = 0;
			for(int chi=0; chi < ch.size(); chi++)
			{
			    ExprData.Cluster child = ( ExprData.Cluster) ch.elementAt(chi);
			    if(!child.getIsSpot())
			    {
				ClustExt ce = drawCluster(cc, false, child, meas_cluster_height[ci]);
								if(ce != null)
				{
				    if(!overlay_root_children[1])
				    {
					cc.d_off += meas_cluster_height[ci];
					cc.d_max += meas_cluster_height[ci];
				    }
				}
				
				ci++;
			    }
			}
		    
			// and restore the clip region to full-screen
			cc.g.setClip( 0,0, dplot_panel.getWidth(), dplot_panel.getHeight() );
		    }
		
		    meas_cluster_skip = meas_cluster_h; // ((int) (ce.height)); // * branch_scale;
		    
		    /*
		      System.out.println("border gap was " + border_gap );
		      System.out.println("branch scale was " + branch_scale );
		      System.out.println("measurement clusters height= " + meas_cluster_skip );
		    */
		    
		    // shift other components downwards to accomodate the Measurement clusters
		    
		    topskip           = border_gap + meas_name_gap + row_gap + meas_cluster_skip;
		    first_line_topoff = topskip + rowskip;
		    
		}
		
		
		// ========================================
		// prepare to draw measurement names
		// ========================================
		
		String name_str = null;
		
		g.setFont(meas_label_font);
		
		label_fm = g.getFontMetrics();
		font_height = label_fm.getAscent();
		
		g.setColor(actual_text_col);
		
		yp = topskip - row_gap; // meas_name_pos + meas_cluster_skip;
		xp = leftskip - scroll_horiz_offset;
		
		// ========================================================
		// now we finally know how many spots will fit on the 'page'
		// ========================================================
		
		spots_per_page = (int)(Math.ceil((float)(height - first_line_topoff - border_gap) / (float)rowskip));
		
		// ========================================================
		// draw the measurement names
		// ========================================================
		
		final int total_sel_col_h = ((1+spots_per_page) * rowskip)+meas_name_gap+actual_row_gap;

		boolean has_meas_sel = false;

		if( meas_label_font_antialias )
		{
		    ((Graphics2D) g).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
						     RenderingHints.VALUE_ANTIALIAS_ON );
		}

		for(int m=0; m < n_visible_meas; m++)
		{
		    if(meas_in_col[m] >= 0)
		    {
			
			if(edata.isMeasurementSelected(meas_in_col[m]))
			{
			    g.setColor(actual_text_col);
			    g.fillRect(xp-col_gap, yp-meas_name_gap-actual_row_gap, colskip+col_gap, total_sel_col_h);
			    g.setColor(actual_background_col);

			    has_meas_sel = true;
			}
			else
			{
			    g.setColor(actual_text_col);
			}
			
			g.setClip( xp, yp-meas_name_gap, box_width, meas_name_gap );

			int mlxp = xp;
			String mn = edata.getMeasurementName(meas_in_col[m]);

			switch(meas_label_align)
			{
			case 1: // center
			    mlxp += ((box_width - label_fm.stringWidth(mn)) / 2);
			    break;
			case 2: // right
			    mlxp += (box_width - label_fm.stringWidth(mn));
			    break;
			}
			
			g.drawString(mn, mlxp, yp - meas_name_offset );

			xp += colskip;

			g.setClip(0,0,width,height);
		    }
		}
		
		// switch antialiasing back off again to speed up the non-text rendering
		//
		if( meas_label_font_antialias )
		{
		    ((Graphics2D) g).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
						       RenderingHints.VALUE_ANTIALIAS_OFF );
		}


		yp = topskip;
		
		// assume the worst to start off with....
		
		max_vis_spot = n_spots - 1;
		
		// ========================================
		// auto-scroll-back
		// ========================================
		
		// make sure that some spots are visible
		// (i.e. if the box geometry, or filtering has changed and the 
		//  scrollbar height and thumb posn are no longer sensible)
		//
		n_visible_spots = 0;
		int search_ahead_pos = min_vis_spot_to_paint;
		boolean found_something = false;
		while(((!found_something) && (search_ahead_pos < n_spots)))
		{
		    if(apply_filter == true)
		    {
			if(!edata.filter(edata.getSpotAtIndex(search_ahead_pos)))
			    found_something = true;
		    } 
		    else
		    {
			found_something = true;
		    }
		    
		    search_ahead_pos++;
		}
		
		if(!found_something)
		{
		    if(min_vis_spot_to_paint >= n_spots)
			min_vis_spot_to_paint = n_spots-1;
		    
		    //System.out.println("auto scroll back.... (from" +  min_vis_spot_to_paint);
		    
		    while((n_visible_spots < (spots_per_page-1)) && (min_vis_spot_to_paint >= 0))
		    {
			if((apply_filter == false) || (!edata.filter(edata.getSpotAtIndex(min_vis_spot_to_paint))))
			n_visible_spots++;
			min_vis_spot_to_paint--;
		    }
		    if(min_vis_spot_to_paint < 0)
		    min_vis_spot_to_paint = 0;
		    
		    // move the scrollbar thumb to the new position
		    vert_sb.setValue(first_line_topoff + (min_vis_spot_to_paint * rowskip));
		    
		    //System.out.println("   ... end at " + min_vis_spot_to_paint + ")");
		}
		
		// ========================================
		// spot_id -> row mappings
		// ========================================

		// spot_id_in_row provides a forwards mapping from row index to gene index
		//
		if(((spot_id_in_row == null) || (spot_id_in_row.length != (spots_per_page+1))))
		{
		    spot_id_in_row = new int[spots_per_page+1];
		    // System.out.println("rebuild imap");
		}
		
		// and row_for_spot_id provides in the inverse mapping, from the gene index to row index
		//
		if(row_for_spot_id == null)
		    row_for_spot_id = new Hashtable();
		else
		    row_for_spot_id.clear();
		
		spot_index = min_vis_spot_to_paint;
		int line = 0;

		/*
		  // keep track of the name strings that are drawn so they can be redrawn
		  // quickly as part of the highlighting....
		  //
		  name_highlight_data_v = new Vector();
		*/
		
		final int sel_spot_width = total_name_col_width + (n_visible_meas * colskip);

		final int sel_spot_height =  box_height+(2*actual_row_gap);
		final int sel_spot_off = (actual_row_gap < 1) ? 0 : actual_row_gap;
		
		boolean last_spot_was_selected_too = false;

		g.setFont(spot_label_font);
		
		label_fm = g.getFontMetrics();
		font_height = label_fm.getAscent();
		
		while((spot_index <= max_vis_spot) && (n_visible_spots < spots_per_page))
		{   
		    int spot_id = edata.getSpotAtIndex(spot_index);
		    
		    // the filter method expects actual gene indices, not those
		    // specified using the current traversal
		    if((apply_filter == false) || (!edata.filter(spot_id)))
		    {
			{
			    boolean is_selected = edata.isSpotSelected( spot_id );
			    
			    xp = border_gap - scroll_horiz_offset;
			    
			    if(is_selected)
			    {
				// avoid the top border is the last spot was also
				// selected (gives a nicer appearance as the bottom
				// of the text doesn't get clipped)

				int y_off = last_spot_was_selected_too ? 0 : sel_spot_off;

				g.setColor(actual_text_col);
						
				g.fillRect(border_gap, yp-y_off, sel_spot_width,  sel_spot_height);

				g.setColor(actual_background_col);

				last_spot_was_selected_too = true;
			    }
			    else
			    {
				g.setColor(actual_text_col);

				last_spot_was_selected_too = false;
			    }

			    //
			    if( spot_label_font_antialias )
			    {
				((Graphics2D) g).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
								   RenderingHints.VALUE_ANTIALIAS_ON );
			    }

			    for(int nc=0; nc < n_name_cols; nc++)
			    {
				name_str = getTrimmedNameCol(nc, spot_id);    
				if(name_col_align[nc] == 0)
				{
				    g.drawString(name_str, xp, yp+font_height); 
				}
				else
				{
				   int sw = label_fm.stringWidth(name_str);
				   if(name_col_align[nc] == 2)
				   {
				       g.drawString(name_str, xp + name_col_width[nc] - sw, yp+font_height);
				   }
				   else
				   {
				       g.drawString(name_str, xp + ((name_col_width[nc]) - sw)/2, yp+font_height);
				   }
				}
				xp += name_col_width[nc] + name_col_gap; 
			    }

			    // switch antialiasing back off again to speed up the non-text rendering
			    //
			    if( spot_label_font_antialias )
			    {
				((Graphics2D) g).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
								   RenderingHints.VALUE_ANTIALIAS_OFF );
			    }



			    // keep track of which spot index is drawn in which line
			    //
			    // spot_id_in_row records which spot is in which line
			    // of the current display, key is spot id, value is line
			    // (note, actual spot ids are used, not those of the 
			    //  current traversal, this makes looking up where to draw
			    //  the cluster glyphs much easier)
			    //
			    row_for_spot_id.put(new Integer(spot_id), new Integer(line));
			    
			    // this mapping is used by the mouse tracker to work out
			    // which line of the data we are pointing at...
			    //
			    spot_id_in_row[line] = spot_index; // edata.getGeneAtIndex(gene);
			    
			    line++;
			    
			    for(meas=0; meas < meas_in_col.length; meas++)
			    {
				if((meas < n_visible_meas) && (meas_in_col[meas] >= 0))
				{
				    double ev = edata.eValue(meas_in_col[meas], spot_id);
				    
				    if( Double.isNaN( ev ) || Double.isInfinite( ev ) )
				    {
					g.setColor(actual_text_col);
					g.drawRect(xp+1, yp+1, box_width-2, box_height-2);
				    }
				    else
				    {
					g.setColor(getDataColour(ev, meas_in_col[meas]));
					g.fillRect(xp, yp, box_width, box_height);
				    }
				    
				    xp += colskip;
				}
			    }
			    yp += rowskip;
			    n_visible_spots++;
			    
			} // end if (edata.inVisibleClusters(edata.getGeneAtIndex(gene)) > 0)))
		    }     // end if (!edata.filter(edata.getIndexOf(gene)))
		    
		    spot_index++;
		}

		if(line < spot_id_in_row.length)
		{
		    // blank out any measurements highlights that go beyond the end of the data

		    if(has_meas_sel)
		    {
			g.setColor(actual_background_col);
			
			g.fillRect(border_gap,  yp, width-(2*border_gap), height-yp);
		    }
			
		    while(line < spot_id_in_row.length)
		    {
			spot_id_in_row[line++] = -1;
		    }
		}
		
	    
		
		
		max_vis_spot = min_vis_spot_to_paint + n_visible_spots - 1;
		
		// --- --- --- --- --- ---  --- --- --- --- --- ---  --- --- --- --- --- --- 
		// --- --- --- --- --- ---  --- --- --- --- --- ---  --- --- --- --- --- --- 
		//
		// draw glyphs showing which clusters the displayed probes are in
		//
		
		int max_glyphs = 0;  // most seen on one line (so the tree can be drawn after them)
		
		
		// --- --- --- --- --- ---  --- --- --- --- --- ---  --- --- --- --- --- --- 
		// --- --- --- --- --- ---  --- --- --- --- --- ---  --- --- --- --- --- --- 
		//
		// yet another new improved dendrogram drawing technology
		//
		if(show_branches[0])
		{
		    int tree_x_pos = leftskip + (n_visible_meas * colskip) + col_gap;
		    
		    cluster_top_pos = topskip + halfrow;
		    
		    ClustContext cc = new ClustContext(g, 
						       cluster_top_pos, tree_x_pos-scroll_horiz_offset,  
						       min_vis_spot_to_paint, spots_per_page, 
						       cluster_top_pos+(spots_per_page*rowskip),
						       tree_x_pos-scroll_horiz_offset+spot_cluster_w,
						       row_for_spot_id);
		    
		    
		    ExprData.Cluster rc = edata.getRootCluster();
		    Vector ch = rc.getChildren();
		    
		    

		    if(ch != null)
		    {
			if(overlay_root_children[0])
			{
			    cc.d_off += spot_cluster_w;
			    cc.d_max += spot_cluster_w;
			}
			

			final int spot_clust_sx = cc.d_off - spot_cluster_w;
			final int spot_clust_sy = cc.b_off - (box_height/2);

			final int spot_clust_h  = cc.b_max - spot_clust_sy - (box_height/2);
			final int spot_clust_w  = spot_cluster_w;

			
			// debug
			//cc.g.setColor( Color.blue );
			//cc.g.drawRect( spot_clust_sx, spot_clust_sy, spot_clust_w, spot_clust_h );
			// end debug
			
			// set the clip region to the official area devoted for the measurement clusters
			//

			final int width_remaining = getWidth() - spot_clust_sx;

			cc.g.setClip( spot_clust_sx, spot_clust_sy, width_remaining, spot_clust_h );
			
			
			int ci = 0;
			for(int chi=0; chi < ch.size(); chi++)
			{
			    ExprData.Cluster child = ( ExprData.Cluster) ch.elementAt(chi);
			    if(child.getIsSpot())
			    {
				if(!overlay_root_children[0])
				    cc.d_off += spot_cluster_width[ci];
				
				
				ClustExt ce = drawCluster(cc, true, child, spot_cluster_width[ci]);
				
				ci++;
			    }
			}

			// and restore the clip region to full-screen
			cc.g.setClip( 0,0, dplot_panel.getWidth(), dplot_panel.getHeight() );
		    }
		}
		
	    }
	    catch(NullPointerException npe)
	    {
		// something was probably updating one of the lookup tables during a load
		// when something caused the display to repaint...
		// ...indicates that some more methods should probably been synchronized
	    }
	    catch(ArrayIndexOutOfBoundsException npe)
	    {
		// as above...
	    }

	}
    }
	
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    //   new improved dendrogram drawing technology
    //

    class ClustExt
    {
	public float mid, min, max, emin, emax, height;
	public boolean has_extent;  // any part of this cluster onscreen?
	public boolean onscreen;
	public Vector children;

	public ClustExt(int mi, int ma, int h)
	{
	    emin = min = (float)mi; 
	    emax = max = (float)ma; 
	    height = (float)h;
	    mid = (min+max) * 0.5f;
	    has_extent = false;
	    onscreen = false;
	    children = new Vector();
	}
    }

    /**
     *  enough info to work out which measurements or spots are visible, and where the
     *  the dendrogram should appear relative to the expression data
     */
    class ClustContext
    {
	public Graphics g;

	public int d_off;  // depth offset
	public int b_off;  // breadth offset

	public int d_max;  // depth maximum size
	public int b_max;  // breadth maximum size

	public int min_visible;
	public int max_visible;
	
	// which measurements or spots are appearing in the rows onscreen?
	// (because of sorting and/or filtering, elements may not appear consecutively,
	//  there might be gaps in the sequence from min_visible to max_visible)
	
	public Hashtable pos_for_id;

	public int[] glyph_count;

	public ClustContext(Graphics g_, 
			    int b_off_, int d_off_, 
			    int min_visible_, int max_visible_, 
			    int b_max_, int d_max_,
			    Hashtable pos_for_id_)
	{
	    g = g_;
	    b_off = b_off_;
	    d_off = d_off_;
	    b_max = b_max_;
	    d_max = d_max_;
	    min_visible = min_visible_;
	    max_visible = max_visible_; 
	    pos_for_id = pos_for_id_;

	    //System.out.println("d_off=" + d_off + " d_max=" + d_max);
	}

    }

    public final boolean debug_dendrodraw = false;

    //
    //

    //private Hashtable cluster_location = null;

    private Hashtable cluster_h_lines   = null;
    private Hashtable cluster_v_lines   = null;
    private Hashtable cluster_glyph_pos = null;
    private class VisibleClusterLine
    {
	public int start;
	public int end;
	public ExprData.Cluster cluster;

	public VisibleClusterLine(ExprData.Cluster c, int s, int e) { cluster = c; start = s; end = e; }
	public boolean onLine(int p) { return (p >= start && p<= end); }
    }
    private class VisibleCluster
    {
	public int x, y;
	public ExprData.Cluster cluster;
	
	public VisibleCluster(ExprData.Cluster c, int x_, int y_) { cluster = c; x = x_; y = y_; }
	public boolean onGlyph(int px, int py) { return ((px>=x) &&  (py>=y) && (px<(x+box_height)) && (py<(y+box_height))); }
    }
    
    // ================================================================================================

    public void dumpDebugInfo()
    {
	/*
	System.out.println("debug....");
	debug_clusters = true;
	//dplot_panel.paintComponent( dplot_panel.getGraphics() );
	dplot_panel.repaint();
	debug_clusters = false;
	*/
	
	debug_clusters = !debug_clusters;
	System.out.println("debug is " + (debug_clusters ? "on" : "off"));
	dplot_panel.repaint();
	repaint();
    }

    private boolean debug_clusters = false;
	
    private ClustExt drawCluster(final ClustContext cc, final boolean is_spot, final ExprData.Cluster c, final int depth)
    {
	cc.glyph_count = new int[cc.max_visible];
	return drawCluster(cc, is_spot, c, depth, null);
    }

    // draws the elements of cluster 'c' and stores the min,max and mid in 'ce'
    // 
    private final void drawElements(final ClustContext cc, final ClustExt ce, final int depth, final ExprData.Cluster c)
    {
	// if this cluster is visible, 
	// we need to consider the positions of any elements
	// this cluster may contain
	
	int[] ve = c.getElements();
	Polygon poly = null;
	int polyx = 0;
	int polyy = 0;
	
	if(c.getShow() && (ve != null))
	{
	    Integer sl = null;
	    final int nels = ve.length;
	    
	    int gly = c.getGlyph();
	    cc.g.setColor(c.getColour());

	    final boolean is_spot = c.getIsSpot();
	    final int cl_type = is_spot ? 0 : 1;

	    final boolean algl = align_glyphs[cl_type] && show_glyphs[cl_type];

	    final float skip_f = is_spot ? (float) rowskip : (float) colskip;

	    int xp, yp;
	    int xg, yg;
	    int pxp, pyp;

	    if(show_glyphs[cl_type])
	    {
		poly = new Polygon(glyph_poly[gly].xpoints, 
				   glyph_poly[gly].ypoints,
				   glyph_poly[gly].npoints);
	    }

	    for(int ei=0; ei< nels; ei++)
	    {
		sl = (Integer) cc.pos_for_id.get( new Integer(ve[ei]) );
		
		if(sl != null)
		{
		    // this meas or spot is visible onscreen
		    //
		    final int pos = sl.intValue();
		    if(pos > ce.max)
		    {
			ce.max = pos;
		    }
		    if(pos < ce.min)
		    {
			ce.min = pos;
		    }
		    
		    if(debug_clusters)
		    {
			System.out.println( c.getName() + ": element id " + ve[ei] + " in pos " + pos);
		    }
		    

		    final int gc = cc.glyph_count[pos]++;

		    // draw the glyph and a joining line to the ClustExt
		    //
		    if(is_spot)
		    {
			yp = cc.b_off + (int)((float) pos * skip_f);

			if((yp >= cc.b_off) && (yp <= cc.b_max))
			{
			    // therefore this cluster does have a visible extent
			    //
			    ce.has_extent = true;			
		    
			    // and it also has visible glyphs
			    //
			    ce.onscreen = true;
		   
			    xp = cc.d_off + (int)ce.height;
			    xg = algl ? (cc.d_off - depth + box_height) : xp - branch_scale[cl_type];
			    
			    xg -= (gc *  box_height);

			    cc.g.drawLine(xp, yp, xg, yp);
			    
			    storeLineInTable( cluster_h_lines, new Integer(yp), 
					      new VisibleClusterLine(c, xg, xp) );
			    
			    if(show_glyphs[cl_type])
			    {
				yg = yp - (box_height/2);
				xg -= box_height;

				poly.translate( xg - polyx, yg - polyy );
				
				polyx = xg;
				polyy = yg;
				
				cc.g.fillPolygon(poly);

				storeGlyphInTable( cluster_glyph_pos, xg, yg, c );
			    }
			}
		    }
		    else
		    {
			// it's a measurement not a spot...

			xp = cc.b_off + (int)((float) pos * skip_f);
			
			if((xp >= cc.b_off) && (xp <= cc.b_max))
			{
			    // therefore this cluster does have a visible extent
			    //
			    ce.has_extent = true;			
			    
			    // and it also has visible glyphs
			    //
			    ce.onscreen = true;
			    
			    yp = cc.d_off + (int)ce.height;
			    yg = algl ? (cc.d_off + depth - box_height) : yp + branch_scale[cl_type];

			    yg += (gc *  box_height);
			    
			    cc.g.drawLine(xp, yp, xp, yg);
			    
			    storeLineInTable( cluster_v_lines, new Integer(xp), 
					      new VisibleClusterLine(c, yp, yg) );
			    
			    
			    if(show_glyphs[cl_type])
			    {
				xg = xp - (box_height/2);
				
				poly.translate( xg - polyx, yg - polyy );
				polyx = xg;
				polyy = yg;
				
				cc.g.fillPolygon(poly);

				storeGlyphInTable( cluster_glyph_pos, xg, yg, c );
			    }
			}
		    }
		}
		else
		{
		    // this measurement or spot not visible onscreen
		    
		    // either it has been filtered out,
		    //
		    // or it is somewhere offscreen
		    //
		    // we are only interested in the case where
		    // it is not filtered, but offscreen, as
		    // the extent of this cluster must include
		    // spots which are not currently onscreen
		    //
		    final int iv = ve[ei];
		    
		    if(is_spot)
		    {
			if((apply_filter == false) || (!edata.filter(iv)))
			{
			    if(iv >= 0)
			    {
				int line = edata.getIndexOf(iv) - cc.min_visible;
				
				if(line > ce.max) // off to the bottom somewhere...
				{
				    ce.max = line; 
				}
				
				if(line < ce.min) 
				{
				    ce.min = line; 
				}
				
				// this cluster does indeed have a presence
				//
				ce.has_extent = true;

				if(debug_clusters)
				{
				    System.out.println( c.getName() + ": spot id " + ve[ei] + " offscreen in " + line);
				}
		    
			    }
			}
		    }
		    else
		    {
			int col = edata.getIndexOfMeasurement(iv) - cc.min_visible; // edata.getNumMeasurements();
			
			if(col > ce.max) // off to the right somewhere...
			{
			    ce.max = col; 
			}
			
			if(col < ce.min) 
			{
			    ce.min = col; 
			}

			if(debug_clusters)
			{
			    System.out.println( c.getName() + ": meas id " + ve[ei] + " offscreen in " + col);
			}

			ce.has_extent = true;
		    }
		}
	    }

	    ce.emin = ce.min;
	    ce.emax = ce.max;
	}
    }

    // changed: 14.3.01  so that mixed hide/show paths are handled properly
    //
    //          10.7.01  so that offscreen edges are shown properly
    //
    private ClustExt drawCluster(final ClustContext cc, final boolean is_spot, 
				 final ExprData.Cluster c, final int depth, final ClustExt parent)
    {
	if((c == null)/* || (c.getIsSpot() == is_spot)*/)
	    return null;

	ClustExt new_ce = null;

	final int cl_type = is_spot ? 0 : 1;

	// ====================================================================
	// first, if the cluster is visible, draw the elements
	// and setup a ClustExt recording their extent
	// ====================================================================

	if(c.getShow())
	{
	    int height = 0;

	    
	    if(parent != null)
	    {
		height = is_spot ? ((int)parent.height - branch_scale[cl_type]) : ((int)parent.height + branch_scale[cl_type]);
	    }

	     
	    if(c.getIsSpot() == is_spot)
	    {
		new_ce = new ClustExt(is_spot ? edata.getNumSpots() : edata.getNumMeasurements(), 0, height );
	   
		drawElements(cc, new_ce, depth, c);
		
		if(parent != null)
		    parent.children.addElement(new_ce);
	    }
	}
	else
	{
	    new_ce = parent;
	}

	// ====================================================================
	// then recursively draw the children (storing their ClustExts)
	// ====================================================================

	Vector ch = c.getChildren();
	if(ch != null)
	{
	    final int nkids = ch.size();
	    
	    for(int ci=0; ci < nkids; ci++)
	    {
		drawCluster(cc, is_spot, (ExprData.Cluster) ch.elementAt(ci), depth, new_ce);
	    }
	}

	// ====================================================================
	// now look at the ClustExts of this node and draw the line marking the
	// extent of this cluster (which will include any children and elements)
	// ====================================================================

	if(new_ce != null)
	{
	    final int nce = new_ce.children.size();
	    
	    int start_e;
	    int start_p;
	    
	    final float skip_f = is_spot ? (float) rowskip : (float) colskip;
	    
	    final int trim_pos = cc.b_off - (int)skip_f;

	    cc.g.setColor(c.getColour());

	    for(int ce=0; ce < nce; ce++)
	    {
		ClustExt cce = (ClustExt ) new_ce.children.elementAt(ce);
		
		if(cce.has_extent)
		{
		    new_ce.has_extent = true;


		    if(cce.mid < new_ce.min)
			new_ce.min = cce.mid;
		    if(cce.mid > new_ce.max)
			new_ce.max = cce.mid;

		    // keep track of the min and max of the full extent of this cluster & it's children
		    if(cce.emin < new_ce.emin)
			new_ce.emin = cce.emin;
		    if(cce.emax > new_ce.emax)
			new_ce.emax = cce.emax;

		    if(cce.onscreen)
			new_ce.onscreen = true;

		    // draw a line to this child ClustExt
		    
		    int child_pos = cc.b_off + (int)((float) cce.mid * skip_f);
		    
		    start_e = cc.d_off + (int)new_ce.height;
		    start_p = cc.d_off + (int)cce.height;
		    
		    if(is_spot)
		    {
			if((child_pos >= cc.b_off) && (child_pos <= cc.b_max))
			{
			    cc.g.drawLine(start_p, child_pos, start_e, child_pos);
			    
			    storeLineInTable( cluster_h_lines, new Integer(child_pos), 
					  new VisibleClusterLine(c, start_p, start_e) );
			}
			
		    }
		    else
		    {
			if((child_pos >= cc.b_off) && (child_pos <= cc.b_max))
			{
			    cc.g.drawLine(child_pos, start_p, child_pos, start_e);
			    
			    storeLineInTable( cluster_v_lines, new Integer(child_pos), 
					      new VisibleClusterLine(c, start_p, start_e) );
			}
		    }
		}
	    }
	    
	    new_ce.mid = (new_ce.min+new_ce.max) / 2;
	    
	    start_p = cc.d_off + (int)(new_ce.height);
	    
	    if(/*(new_ce.has_extent) ||*/ (new_ce.onscreen))
	    {
		int max_pos   = (int)(new_ce.max * skip_f) + cc.b_off;
		
		int min_pos   = (int)(new_ce.min * skip_f) + cc.b_off;
		
		int e_max_pos = (int)(new_ce.emax * skip_f) + cc.b_off;
		
		int e_min_pos = (int)(new_ce.emin * skip_f) + cc.b_off;
		
				
		boolean draw_it = true;
		
	        //if(min_pos < cc.b_max)
		if((e_min_pos < cc.b_max) || (e_max_pos < cc.b_max))
		{
		    if(max_pos > cc.b_max)
			max_pos = cc.b_max;
		    
		    if(min_pos < trim_pos)
			min_pos = trim_pos;
		    
		    if(max_pos > min_pos)
		    {
			if(is_spot)
			{
			    cc.g.drawLine(start_p, min_pos, start_p, max_pos);
			    storeLineInTable( cluster_v_lines, new Integer(start_p), 
					      new VisibleClusterLine(c, min_pos, max_pos) );
			    
			    if(edata.isClusterSelected(c))
			    {
				int p = cc.d_off + (int)new_ce.height;
			        int ox = (cc.d_off - depth + box_height);

				if(show_glyphs[cl_type])
				    ox -= box_height;

				int w = (p < ox) ? ox-p : p-ox;

				int oy =  e_min_pos - (box_height/2);
				int h  = (e_max_pos-e_min_pos) + box_height;
				
				//System.out.println("d_off=" + cc.d_off + " d_max=" + cc.d_max);

				//System.out.println(c.getName() + " (h=" + (int)new_ce.height + 
				//		   ") is " + w + "x" + 
				//		   h + " @ " + ox + "," + oy);

				cc.g.setColor(text_col);

				cc.g.drawRect(ox, oy, w, h);
				if(w > 2)
				{
				    w -= 2;
				    ox += 1;
				}
				if(h > 2)
				{
				    h -= 2;
				    oy += 1;
				}
				cc.g.drawRect(ox, oy, w, h);

				cc.g.setColor(c.getColour()); // switch back to the right colour
			    }

			}
			else
			{
			    cc.g.drawLine(min_pos, start_p, max_pos, start_p);
			    storeLineInTable( cluster_h_lines, new Integer(start_p), 
					      new VisibleClusterLine(c, min_pos, max_pos) );

			    if(edata.isClusterSelected(c))
			    {
				//System.out.println("sel meas clust:" + c.getName());

				// work out the maximum extent of the cluster

				// ::::TODO::::
				
				int p = cc.d_off + (int)new_ce.height;
			        
				// note: box_height here is to compensate the glyph height, not the column width
				int ox = e_min_pos - (box_height/2);
				int w = (e_max_pos-e_min_pos) + box_height; 

				// start_p = cc.d_off + (int)(new_ce.height);

				int oy = p;                // (cc.d_off - depth + box_height);
				int h  = cc.d_max - p - 1; // p + box_height; // (p < ox) ? ox-p : p-ox;
				

				cc.g.setColor(text_col);

				cc.g.drawRect(ox, oy, w, h);
				if(w > 2)
				{
				    w -= 2;
				    ox += 1;
				}
				if(h > 2)
				{
				    h -= 2;
				    oy += 1;
				}
				cc.g.drawRect(ox, oy, w, h);


				
				cc.g.setColor(c.getColour());
			    }
			    

			}
		    }
		}

		// draw a bow round the cluster if it is in the selection

		
	    }
	}
	
	if(debug_clusters)
	{
	    if((new_ce.has_extent) || (new_ce.onscreen))
	    {
		System.out.println( c.getName() + 
				    " min=" + new_ce.min + 
				    " max=" + new_ce.max +
				    " mid=" + new_ce.mid +
				    " ext=" + new_ce.has_extent +
				    " ons=" + new_ce.onscreen );
	    }
	}

		
	return new_ce;
    }


    private final void storeLineInTable( Hashtable hasht, Integer pos, VisibleClusterLine vcl )
    {
	Vector lines_at_this_pos = (Vector) hasht.get(pos);
	if(lines_at_this_pos == null)
	{
	    // first cluster to use this X pos
	    lines_at_this_pos = new Vector();
	    hasht.put(pos, lines_at_this_pos);
	}
	lines_at_this_pos.addElement(vcl);
    }
    private final void storeGlyphInTable( final Hashtable hasht, final int x, final int y, final ExprData.Cluster cl )
    {
	final Integer pos = new Integer(y);
	Vector glyphs_at_this_pos = (Vector) hasht.get(pos);
	if(glyphs_at_this_pos == null)
	{
	    // first cluster to use this X pos
	    glyphs_at_this_pos = new Vector();
	    hasht.put(pos, glyphs_at_this_pos);
	}
	glyphs_at_this_pos.addElement(new VisibleCluster(cl ,x, y));

	// System.out.println(x + "," + y + " stored as " + glyphs_at_this_pos.size() +"th thing with id=" + pos.intValue());
    }


    private int findClusterDepth(final ExprData.Cluster c, final boolean is_spot)
    {
	if(c == null)
	    return 0;
	
	final int cl_type = is_spot ? 0 : 1;

	int depth = ((c.getShow() == true) && (c.getIsSpot() == is_spot)) ? branch_scale[cl_type] : 0;
	
	Vector ch = c.getChildren();
	int c_d_max = 0;
	if(ch != null)
	{
	    for(int ci=0; ci < ch.size(); ci++)
	    {
		int c_d = findClusterDepth(((ExprData.Cluster) ch.elementAt(ci)), is_spot);
		if(c_d > c_d_max)
		    c_d_max = c_d;
	    }
	}

	/*
	else
	{
	    if(c.getNumElements() > 0)
		depth += (show_glyphs ? box_height : 0);
	}
	*/

	return depth + c_d_max;
    }

    // ================================================================================================
    

    // ---------------- --------------- --------------- ------------- ------------
    //
    // geometry and display options
    //
    public void setBoxGeometry(int bw, int bh, int cg, int rg)
    {
	box_width = bw;
	actual_box_height =  bh;
	col_gap = cg;
	actual_row_gap = rg;

	// System.out.println("new geometry: " + bw + " x " + bh + ", gaps: " + cg + "," + rg);

	updateDisplay();
    }

    public int getBoxWidth() { return box_width; }
    public int getBoxHeight() { return actual_box_height; }
    public int getColGap() { return col_gap; }
    public int getRowGap() { return actual_row_gap; }

    public void setBoxWidth(int v)  { box_width = v; updateDisplay(); }
    public void setBoxHeight(int v) { actual_box_height = v; updateDisplay(); }
    public void setColGap(int v)    { col_gap = v; updateDisplay(); }
    public void setRowGap(int v)    { actual_row_gap = v; updateDisplay(); }

    public int  getBorderGap()       { return border_gap; }
    public void setBorderGap(int g)  { border_gap = g; updateDisplay(); }
    
    public int  getNameColGap()       { return name_col_gap; }
    public void setNameColGap(int g)  { name_col_gap = g; updateDisplay(); }
    
    public int  getMeasurementLabelAlign()        { return meas_label_align; }
    public void setMeasurementLabelAlign(int sla) { meas_label_align = sla; updateDisplay(); }

    // new
    // scroll_mode is a bitfield
    //  bit0 = keep_names_onscreen
    //  bit1 = keep_clusters_onscreen
    //
    public int  getScrollMode()       { return scroll_mode; }
    public void setScrollMode(int sm) { scroll_mode = sm; updateDisplay(); }
    private int scroll_mode = 3;  // keep both

    // =========================================================================
 
    private ExprData.NameTagSelection[] name_col_sel;
    private int      n_name_cols;
    public int[]     name_col_trim_length;
    public boolean[] name_col_trim;
    public int[]     name_col_align;

    public int[]     name_col_width; 
    public int       total_name_col_width;

    public void    setNameColTrimEnabled(int nc, boolean te) { name_col_trim[nc] = te; findLongestNames(); updateDisplay(); }
    public boolean getNameColTrimEnabled(int nc )           { return name_col_trim[nc]; }

    public void  setNameColTrimLength(int nc, int tl) { name_col_trim_length[nc] = tl; findLongestNames(); updateDisplay(); }
    public int   getNameColTrimLength(int nc )       { return name_col_trim_length[nc]; }

    public int getNumNameCols() { return n_name_cols; }

    public void setNameColSelection(int nc, ExprData.NameTagSelection nts) 
    { 
	name_col_sel[nc] = nts; 
	edata.generateDataUpdate(ExprData.VisibleNameAttrsChanged);
    }

    public ExprData.NameTagSelection getNameColSelection(int nc) { return  name_col_sel[nc]; }

    public int  getNameColAlign(int nc )        { return name_col_align[nc]; }
    public void setNameColAlign(int nc, int nca) { name_col_align[nc] = nca; updateDisplay(); }

    public void addNameCol()
    {
	if(n_name_cols > 0)
	    addNameCol(n_name_cols-1);
	else
	    addNameCol(0);
    }

    public void addNameCol(int insert_pos)
    {
	// System.out.println("addNameCol): inserting reqested at " + insert_pos + ", currently " + n_name_cols + " cols");

	final int new_len = n_name_cols + 1;
	
	if((insert_pos < 0) || (insert_pos >= new_len))
	{
	    mview.alertMessage("illegal name column insert position ("  + insert_pos + ")");
	    return;
	}

	ExprData.NameTagSelection[] new_name_col_sel = new ExprData.NameTagSelection[new_len];

	int[]     new_name_col_trim_length  = new int[new_len];
	boolean[] new_name_col_trim         = new boolean[new_len];
	int[]     new_name_col_align        = new int[new_len];
	int[]     new_name_col_width        = new int[new_len];

	int cp = 0;
	if(n_name_cols > 0)
	{
	    insert_pos++;
	    for(int c=0; c < n_name_cols; c++) 
	    {
		
		//System.out.println("moving " + c + " to " + cp);

		new_name_col_sel[cp]         = name_col_sel[c];
		new_name_col_trim_length[cp] = name_col_trim_length[c];
		new_name_col_trim[cp]        = name_col_trim[c];
		new_name_col_align[cp]       = name_col_align[c];
		new_name_col_width[cp]       = name_col_width[c];

		cp++;

		if(cp == insert_pos)
		    cp++;

	    }
	}

	new_name_col_sel[insert_pos]         = pickUnusedNameAttr(); //edata.new NameTagSelection();
	new_name_col_trim_length[insert_pos] = 32;
	new_name_col_trim[insert_pos]        = true;
	new_name_col_align[insert_pos]       = 2;
	new_name_col_width[insert_pos]       = 0;

	//System.out.println("inserting at " + insert_pos);

	n_name_cols = new_len;

	name_col_sel         = new_name_col_sel;
	name_col_trim_length = new_name_col_trim_length;
	name_col_trim        = new_name_col_trim;
	name_col_align       = new_name_col_align;
	name_col_width       = new_name_col_width;

	edata.generateDataUpdate(ExprData.VisibleNameAttrsChanged);

	//findLongestNames();

	//updateDisplay();
    }

    private boolean isUsedNameAttr(int name, int attr)
    {
	// System.out.println("isUsedNameAttr(): name=" + name + " attr=" + attr + " ncols=" + n_name_cols);

	for(int nc=0; nc < n_name_cols; nc++)
	{
	    /*
	    System.out.println("c=" + nc + 
			       " #ga=" + name_col_sel[nc].g_attrs.length +
			       " #pa=" + name_col_sel[nc].p_attrs.length +
			       " #sa=" + name_col_sel[nc].s_attrs.length);
			       
	    */

	    switch(name)
	    {
	    case 0:
		if((attr >= 0) && (attr < name_col_sel[nc].g_attrs.length))
		    if(name_col_sel[nc].g_attrs[attr])
			return true;
		if((attr == -1) && name_col_sel[nc].g_names)
		    return true;
		break;

	    case 1:
		if((attr >= 0) && (attr < name_col_sel[nc].p_attrs.length))
		    if(name_col_sel[nc].p_attrs[attr])
			return true;
		if((attr == -1) && name_col_sel[nc].p_name)
		    return true;
		break;

	    case 2:
		if((attr >= 0) && (attr < name_col_sel[nc].s_attrs.length))
		    if(name_col_sel[nc].s_attrs[attr])
			return true;
		if((attr == -1) && name_col_sel[nc].s_name)
		    return true;
		break;
	    }
	}
	return false;
    }

    private ExprData.NameTagSelection pickUnusedNameAttr()
    {
	ExprData.NameTagSelection nts = edata.new NameTagSelection();
	
	if(!isUsedNameAttr(0, -1))
	{
	    nts.g_names = true; 
	    return nts;
	}
	for(int a=0; a < nts.g_attrs.length; a++)
	    if(!isUsedNameAttr(0, a))
	    {
		nts.g_attrs[a] = true; 
		return nts;
	    }
	    
	if(!isUsedNameAttr(1, -1))
	{
	    nts.p_name = true; 
	    return nts;
	}
	for(int a=0; a < nts.p_attrs.length; a++)
	    if(!isUsedNameAttr(1, a))
	    {
		nts.p_attrs[a] = true; 
		return nts;
	    }

	for(int a=0; a < nts.s_attrs.length; a++)
	    if(!isUsedNameAttr(2, a))
	    {
		nts.s_attrs[a] = true; 
		return nts;
	    }
	if(!isUsedNameAttr(2, -1))
	{
	    nts.s_name = true; 
	    return nts;
	}

	// all names and attrs are used, reuse gene names
	nts.g_names = true; 
	return nts;
    }

    public void removeAllNameCols()
    {
	name_col_sel          = new ExprData.NameTagSelection[0];
	name_col_trim_length  = new int[0];
	name_col_trim         = new boolean[0];
	name_col_align        = new int[0];
	name_col_width        = new int[0];
	n_name_cols = 0;   
	edata.generateDataUpdate(ExprData.VisibleNameAttrsChanged);
    }

    public void removeNameCol(int nc)
    {
	if((nc < n_name_cols) && (n_name_cols > 1))
	{
	    final int new_len = n_name_cols - 1;

	    ExprData.NameTagSelection[] new_name_col_sel = new ExprData.NameTagSelection[new_len];
	    
	    int[]     new_name_col_trim_length  = new int[new_len];
	    boolean[] new_name_col_trim         = new boolean[new_len];
	    int[]     new_name_col_align        = new int[new_len];
	    int[]     new_name_col_width          = new int[new_len];
	    
	    int cp = 0;
	    for(int c=0; c < n_name_cols; c++) 
	    {
		if(c != nc)
		{
		    new_name_col_sel[cp]         = name_col_sel[c];
		    new_name_col_trim_length[cp] = name_col_trim_length[c];
		    new_name_col_trim [cp]       = name_col_trim[c];
		    new_name_col_align[cp]       = name_col_align[c];
		    new_name_col_width[cp]       = name_col_width[c];
		    cp++;
		}
	    }
	    
	    n_name_cols = new_len;
	    
	    name_col_sel         = new_name_col_sel;
	    name_col_trim_length = new_name_col_trim_length;
	    name_col_trim        = new_name_col_trim;
	    name_col_align       = new_name_col_align;
	    name_col_width       = new_name_col_width;
	    
	    edata.generateDataUpdate(ExprData.VisibleNameAttrsChanged);

	    //findLongestNames();

	    //updateDisplay();
	}	    
    }

    public void chooseColContents( final int nc )
    {
	if(nc >= name_col_sel.length)
	    return;

	final NameTagSelector nt_sel = new NameTagSelector(mview);
	
	nt_sel.setNameTagSelection( name_col_sel[ nc ] );
	
	nt_sel.addActionListener( new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    setNameColSelection(nc, nt_sel.getNameTagSelection()); 
		}
	    });

	nt_sel.showPopup();
    }

    public String getTrimmedNameCol(int nc, int spot_id)
    {
	if(nc >= name_col_sel.length)
	    return null;

	try
	{
	    String name_str = name_col_sel[nc].getNameTag(spot_id);
	    
	    if((name_str == null) || (name_str.length() == 0))
		return "";
	    
	    // check for max len
	    //
	    if(name_col_trim[nc])
	    {
		if(name_str.length() > name_col_trim_length[nc])
		{
		    return name_str.substring(0, (name_col_trim_length[nc]-3)) + "...";
		}
	    }
	    
	    return name_str;
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    System.out.println("aioobe: nc=" + nc +  " s_id=" + spot_id);
	    return null;
	}
    }

    public String getTrimmedSpotLabel(int spot_id)
    {
	return getTrimmedNameCol(0, spot_id);
    }
    
    // =========================================================================


    private int[]     branch_scale  = new int[] {5 ,5};
    private boolean[] show_branches = new boolean[] {true, true};
    private boolean[] show_glyphs   = new boolean[] {true, true};
    private boolean[] overlay_root_children = new boolean[] {true, true};
    private boolean[] align_glyphs = new boolean[] {true, true};

    public void    setShowGlyphs(int cl_type, boolean sg) { show_glyphs[cl_type] = sg;  updateDisplay(); }
    public boolean getShowGlyphs(int cl_type)        { return show_glyphs[cl_type]; }

    public void    setShowBranches(int cl_type, boolean sb) { show_branches[cl_type] = sb;  updateDisplay(); }
    public boolean getShowBranches(int cl_type)        { return show_branches[cl_type]; }

    public void    setOverlayRootChildren(int cl_type, boolean orc) { overlay_root_children[cl_type] = orc;  updateDisplay(); }
    public boolean getOverlayRootChildren(int cl_type)        { return overlay_root_children[cl_type]; }

    public void    setAlignGlyphs(int cl_type, boolean ag) { align_glyphs[cl_type] = ag;  updateDisplay(); }
    public boolean getAlignGlyphs(int cl_type)           { return align_glyphs[cl_type]; }
    
    public void setBranchScale(int cl_type, int bs)    { branch_scale[cl_type] = bs;  updateDisplay(); }
    public int  getBranchScale(int cl_type)          { return branch_scale[cl_type]; }

    // set all params with a single method (to avaiod generating lots of events)
    public void setClusterLayout(int cl_type, 
				 boolean sg, boolean sb, boolean orc, boolean ag, int bs)
    {
	show_glyphs[cl_type] = sg;  
	show_branches[cl_type] = sb;
	overlay_root_children[cl_type] = orc;
	align_glyphs[cl_type] = ag;
	branch_scale[cl_type] = bs;
	updateDisplay();
    }

    public int getWindowWidth()  { return (dplot_panel == null) ? 0 : dplot_panel.getWidth(); }
    public int getWindowHeight() { return (dplot_panel == null) ? 0 : dplot_panel.getHeight(); }

    public void setWindowGeometry(int w, int h) 
    { 
	if(dplot_panel != null) 
	    dplot_panel.setPreferredSize(new Dimension(w, h));
    }

    public int getMinVisSpot() { return min_vis_spot; }
    public int getMaxVisSpot() { return max_vis_spot; } 
    private int min_vis_spot, max_vis_spot;


    private int min_vis_meas, max_vis_meas;

     // ---- fonts ------------------------------------------------------------------

    public final Font getFont() { return spot_label_font; }
    public final int getFontHeight() { return spot_label_font_height; }

    public final int getFontFamily()
    {
	return spot_font_family;
    }

    public final void setSpotFontAntialiasing( boolean aa )
    {
	spot_label_font_antialias = aa;
	updateDisplay();
    }

    public final void setFontFamily(int fm)
    {
	spot_font_family = fm;
	updateFonts(1);
    }

    public final void setFontStyle(int fs)
    {
	spot_font_style = fs;
	updateFonts(1);

    }
    public final int getFontStyle()
    {
	return spot_font_style;
    }

    public final void setFontSize(int nfs) 
    { 
	spot_font_size = nfs;
	updateFonts(1);
    }
    public final int getFontSize() { return spot_font_size; }


    public final Font 	 getSpotFont()        	   { return getFont(); }
    public final int  	 getSpotFontFamily()  	   { return getFontFamily(); }
    public final int  	 getSpotFontStyle()   	   { return getFontStyle(); }
    public final int  	 getSpotFontSize()    	   { return getFontSize(); }
    public final boolean getSpotFontAntialiasing() { return meas_label_font_antialias; }

    public final void setSpotFontStyle(int fs)  { setFontStyle(fs); }
    public final void setSpotFontFamily(int fm) { setFontFamily(fm); }
    public final void setSpotFontSize(int fs)   { setFontSize(fs); }
   
    // ---------------

    public final Font    getMeasurementFont()             { return meas_label_font; }
    public final int     getMeasurementFontFamily()       { return meas_label_font_family; }
    public final int     getMeasurementFontStyle()        { return meas_label_font_style; }
    public final int     getMeasurementFontSize()         { return meas_label_font_size; }
    public final boolean getMeasurementFontAntialiasing() { return meas_label_font_antialias; }
    
    public final void setMeasurementFontAntialiasing( boolean aa )
    {
	meas_label_font_antialias = aa;
	updateDisplay();
    }

    public final void setMeasurementFontFamily(int fm)
    {
	meas_label_font_family = fm;
	updateFonts(2);
    }

    public final void setMeasurementFontStyle(int fs)
    {
	meas_label_font_style = fs;
	updateFonts(2);

    }
    public final void setMeasurementFontSize(int nfs) 
    { 
	meas_label_font_size = nfs;
	updateFonts(2);
    }

    // ---------------

    public int fontToFamily(Font fo)
    {
	if(font_family_names == null)
	    return -1;
	
	String name = fo.getName();
	
	for(int fi=0; fi < font_family_names.length; fi++)
	{
	    if(name.equals(font_family_names[fi]))
		return fi;
	}
	return 0;
    }
 
    public int fontToStyle(Font f)
    {
	if(f.isBold())
	{
	    return f.isItalic() ? 3 : 1;
	}
	if(f.isItalic())
	    return 2;

	return 0;
    }

    public int fontStyle(int s)
    {
	if(s == 0)
	    return Font.PLAIN;
	if(s == 1)
	    return Font.BOLD;
	if(s == 2)
	    return Font.ITALIC;
	
	return Font.ITALIC | Font.BOLD;
    }

    public String fontName(int i)
    {
	return(i < font_family_names.length) ? font_family_names[i] : font_family_names[0];
    }

    public String[] font_family_names = null;
    public final String[] font_style_names = { "Plain", "Bold", "Italic", "BoldItalic" };

    private void updateFonts( int font_bitfield )
    {
	// recompute label widths and heights
	Graphics g = getGraphics();

	if(g != null)
	{
	    if( (font_bitfield & 1 ) > 0 )
	    {
		spot_label_font = new Font( fontName( spot_font_family ), fontStyle( spot_font_style ), spot_font_size);

		g.setFont(spot_label_font);

		FontMetrics fm = g.getFontMetrics();
		
		spot_label_font_height = fm.getAscent() ;
				
		findLongestNames();

	    }

	    if( (font_bitfield & 2 ) > 0 )
	    {
		meas_label_font = new Font( fontName( meas_label_font_family ), fontStyle( meas_label_font_style ), meas_label_font_size);

		g.setFont(meas_label_font);

		FontMetrics fm = g.getFontMetrics();
		
		meas_label_font_height = fm.getAscent() + fm.getDescent();
		
		meas_name_gap = meas_label_font_height;

		meas_name_offset = fm.getDescent();

	    }

	    // rejig everything based on the new font(s)...
	    updateDisplay();
	}
    }

    private Font spot_label_font = null;
    private Font meas_label_font = null;

    private int  spot_font_size = 12;
    private int  spot_font_family = 0;
    private int  spot_font_style  = 0;

    private int  meas_label_font_size = 14;
    private int  meas_label_font_family = 0;
    private int  meas_label_font_style  = 1;

    private int  spot_label_font_height;
    private int  meas_label_font_height;

    private boolean spot_label_font_antialias = true;
    private boolean meas_label_font_antialias = true;
    

    private int box_width = 70;

    private int actual_box_height = 14;
    private int box_height = 14;  // this is the scaled height

    private int col_gap = 1;
    private int actual_row_gap = 1;
    private int row_gap = 1;
    private int name_col_gap = 8;

    private int spots_per_page;  // derived from window height and box height....
    private int meas_per_page;   // derived from window width and box width....

    private int border_gap = 5;

    private int rowskip;
    private int colskip;
    private int topskip;
    private int leftskip;

    private int meas_name_gap;      // the space for measurement names at the top
    private int meas_name_offset;   // the font descent, so the font baseline is correctly positioned
    private int meas_label_align = 1;

    private int meas_cluster_skip;
    private int meas_cluster_h;

    private int spot_cluster_w;

    private int first_line_topoff ;
    private int first_line_leftoff;

    private int scroll_horiz_offset = 0;   // used for 'unlocked' scrolling modes

    private int halfrow;

    int font_height = 0;
    FontMetrics label_fm = null;

    //private int row_label_pos   = 1;
    //private int row_label_align = 0;
    //private int row_label_src = 1;


    //private boolean show_comments = true;

    //private boolean trim_labels = true;
    //private int     trim_length = 32;

    private int meas_cluster_x_pos;

    //private int longest_gene_name;

    public boolean getUseFilter()        { return apply_filter; }

    private int cluster_line_len = 20;

    public void setUseFilter(boolean apply_filter_)
    {
	apply_filter = apply_filter_;
	repaint();
    }

    private boolean apply_filter = true;

    // ---------------- --------------- --------------- ------------- ------------
    // cell tracking

    private class NothingUnderMouse extends Exception {}

    public static final int Nothing                = 0;
    public static final int DataElement            = 1;
    public static final int ClusterElement         = 2;
    public static final int ClusterBranch          = 3;
    public static final int MeasurementNameElement = 4;
    public static final int NameColElement         = 5;
    
    private class ThingPos
    {
	public int screen_col, screen_row;
	public int data_col,  data_row;

	public int element;   // what sort of thing is the mouse over?

	public ExprData.Cluster cluster;

	public boolean equals(ThingPos tp) { return ((last_pos.data_row == cur_pos.data_row) && 
						     (last_pos.data_col == cur_pos.data_col) && 
						     (last_pos.element == cur_pos.element) &&
						     (last_pos.cluster == cur_pos.cluster)); }
    }

    private ThingPos last_pos = null;
    private ThingPos cur_pos = null;

    private ExprData.Cluster findNearestCluster(int x, int y)
    {
	if(cluster_glyph_pos != null)
	{
	    final Integer pos = new Integer(y);

	    //System.out.println(x + "," + y + " would be id=" + pos.intValue());
	    
	    Vector glyphs_at_this_pos = (Vector) cluster_glyph_pos.get(pos);
	    if(glyphs_at_this_pos != null)
	    {
		
		final int np = glyphs_at_this_pos.size();

		//System.out.println(np + "hits @ " + x + "," + y + " id=" + pos.intValue());
		
		for(int p=0; p < np; p++)
		{
		    VisibleCluster vc = (VisibleCluster) glyphs_at_this_pos.elementAt(p);
		    //System.out.println("checking " + vc.x  + "," + vc.y);
		    if(vc.onGlyph(x,y))
		    {
			//System.out.println("...hit");
			return vc.cluster;
		    }
		}
	    }
	}
	return null;
    }

    private ExprData.Cluster findNearestBranch(int x, int y)
    {
	if(cluster_h_lines != null)
	{
	    Vector h_lines_at_this_y_pos = (Vector) cluster_h_lines.get(new Integer(y));
	    if(h_lines_at_this_y_pos != null)
	    {
		final int nl = h_lines_at_this_y_pos.size();
		for(int l=0; l < nl; l++)
		{
		    VisibleClusterLine vcl = (VisibleClusterLine) h_lines_at_this_y_pos.elementAt(l);
		    if(vcl.onLine(x))
		    {
			return vcl.cluster;
		    }
		}
	    }
	}
	if(cluster_v_lines != null)
	{
	    Vector v_lines_at_this_x_pos = (Vector) cluster_v_lines.get(new Integer(x));
	    if(v_lines_at_this_x_pos != null)
	    {
		final int nl = v_lines_at_this_x_pos.size();
		for(int l=0; l < nl; l++)
		{
		    VisibleClusterLine vcl = (VisibleClusterLine) v_lines_at_this_x_pos.elementAt(l);
		    if(vcl.onLine(y))
		    {
			return vcl.cluster;
		    }
		}
	    }
	}
	return null;
    }
    

    private ThingPos getThingPosUnderMouse(int x, int y) throws NothingUnderMouse
    {
	ThingPos cp = new ThingPos();

	try
	{
	    boolean in_name_col = false;

	    int name_x = x;
	    if(scroll_mode == 0) // unlocked
	    {
		name_x += scroll_horiz_offset;
	    }

	
	    if(x > first_line_leftoff) //(border_gap + spot_name_gap + col_gap))
	    {
		cp.screen_col = (name_x -  first_line_leftoff /*(border_gap + spot_name_gap + col_gap)*/) / colskip;
	    }
	    else
	    {
		// which name column is it in?
		int nc = 0;
		int xp = border_gap;

		//for(int n=0; n < n_name_cols; n++)
		//    System.out.print( name_col_width[n] + "\t");
		//System.out.print(" ? " + (x-border_gap));
		
		while((!in_name_col) && (nc < n_name_cols))
		{
		    xp += name_col_width[nc] + name_col_gap;
		    
		    if(name_x <= xp)
		    {
			cp.data_col = nc;
			cp.screen_col = -1; // make sure it's not detected as a cluster glyph
			in_name_col = true;
			//System.out.println(" = hit in " + nc);
		    }
		    nc++;
		}
		//if(!in_name_col)
		//System.out.println(" = miss");
	    }

	    if(y > topskip) //(border_gap + meas_name_gap))
	    {
		cp.screen_row = (y - topskip /*(border_gap + meas_name_gap)*/) / rowskip;
	    }
	    else
	    {
		//System.out.println("  above spots");
		cp.screen_row = -1;
	    }
	    
	    /*
	    if((cp.screen_col < 0) && (cp.screen_row < 0))
	    {
		cp.data_col = cp.data_row = -1;
		cp.element = Nothing;
		return cp;
	    }
	    */

	    //System.out.println(cp.screen_col + "," + cp.screen_row + 
	    //		   "max=[" + n_visible_meas + "," +  n_visible_spots + "]");
	    
	    cp.element = Nothing; // assume this to start off with...
	    
	    
	    
	    // is the pointer in the spot or measurement cluster area?
	    //
	    if(((cp.screen_col >= n_visible_meas) && (cp.screen_row >= 0)) ||
	       (cp.screen_row < 0))
	    {
		// it is over a glyph?

		//if( cp.screen_row < 0 )
		//    System.out.println("meas clust: col=" + cp.screen_col);
		    
		// perhaps over one of the stored cluster positions?

		if(show_glyphs[0] || show_glyphs[1])
		{
		    int d = 0;
		    boolean found = false;
		    int off = box_height / 2;
		    while(!found && (d < 4))
		    {
			cp.cluster = findNearestCluster(x, (y-off)-d);
			if(cp.cluster == null)
			{
			    cp.cluster = findNearestCluster(x, (y-off)+d);
			}
			found = (cp.cluster != null);
			d++;
		    }
		    
		    if(cp.cluster != null)
		    {
			cp.element = ClusterElement;
			
			// System.out.println("found " + cp.cluster.getName());
			
			return cp;
		    }
		}

		// or perhaps over one of the stored cluster branch lines?
		
		if(show_branches[0] || show_branches[1])
		{
		    // find the nearest cluster branch

		    cp.cluster = findNearestBranch(x, y);
		    
		    if(cp.cluster == null)
		    {
			// no exact match, relax the constraints and search again....
			int d = 1;
			boolean found = false;

			while(!found && (d < 4))
			{
			    if((cp.cluster = findNearestBranch(x, y+d)) == null)
			    {
				if((cp.cluster = findNearestBranch(x, y-d)) == null)
				{
				    if((cp.cluster = findNearestBranch(x-d, y)) == null)
				    {
					if((cp.cluster = findNearestBranch(x+d, y)) != null)
					    found = true;
				    }
				    else
				    {
					found = true;
				    }
				}
				else
				{
				    found = true;
				}
			    }
			    else
			    {
				found = true;
			    }
			    
			    d++;
			    
			}

			//if(!found)
			//    throw (new NothingUnderMouse());  
		    
			if(found)
			{
			    cp.element = ClusterBranch;
			    return cp;
			}
		    }
		}
	    }
	    
	    // not over a cluster, must be over some data or a name
	    //
	    if(in_name_col)
	    {
		cp.data_row = spot_id_in_row[cp.screen_row];
		cp.element = (cp.data_row >= 0) ? NameColElement : Nothing;
		
		// System.out.println("  ColName: " + cp.data_col + "," + cp.data_row);

		return cp;
	    }

	    if(cp.screen_row < 0)
	    {
		cp.data_col = meas_in_col[cp.screen_col];
		if(cp.data_col >= 0)
		{
		    cp.element = MeasurementNameElement;
		    //System.out.println("  MeasurementName " + cp.screen_col + "," + cp.screen_row);
		    return cp;
		}
	    }  

	    // if we have got this far, it's probably a DataElement...

	    if((cp.screen_col >= 0) && (cp.screen_row >= 0))
	    {

		cp.data_row = spot_id_in_row[cp.screen_row];
		cp.data_col = meas_in_col[cp.screen_col];

		/*
		if((cp.data_col >= edata.getNumMeasurements()) && (cp.data_row >= edata.getNumSpots()))
		{
		    cp.element = DataElement;
		    return cp;
		}
		else
		{
		    cp.element = DataElement;
		    return cp;
		}
		*/
		if((cp.data_row >= 0) && (cp.data_col >= 0))
		{
		    cp.element = DataElement;
		    return cp;
		}
	    }
	    else
	    {
		cp.element = Nothing;
		// System.out.println("  fallen through to here...");
	    }
	    
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    cp.element = Nothing;
	    cp.data_col = cp.data_row = -1;
	}
	catch(NullPointerException npe)
	{
	    cp.element = Nothing;
	    cp.data_col = cp.data_row = -1;
	}

	return cp;
    }


    // ---------------- --------------- --------------- ------------- ------------
    // popup menus
 
    public JPopupMenu makeClusterMenu()
    {
	JPopupMenu popup = new JPopupMenu();
	
	//customMenuListener menu_listener = new CustomMenuListener();
	
	// different wording on the select menu depending on whether we have a spot or a measurement cluster
	int cluster_type_code = 3;
	if((cur_pos.cluster != null) && (cur_pos.cluster.getIsSpot() == false))
	    cluster_type_code = 4;

	popup.add( makeSelectMenu( cluster_type_code,22, 16) );
			
	popup.addSeparator();
	
	JMenuItem mi = new JMenuItem("Show properties");
	mi.addActionListener(new CustomMenuListener(0,7));
	popup.add(mi);
	
	popup.addSeparator();
	
	mi = new JMenuItem("Hide children");
	mi.addActionListener(new CustomMenuListener(0,0));
	popup.add(mi);
	
	mi = new JMenuItem("Show children");
	mi.addActionListener(new CustomMenuListener(0,1));
	popup.add(mi);
	
	popup.addSeparator();
	
	mi = new JMenuItem("Hide parent");
	mi.addActionListener(new CustomMenuListener(0,6));
	popup.add(mi);
	
	mi = new JMenuItem("Show parent");
	mi.addActionListener(new CustomMenuListener(0,2));
	popup.add(mi);
	
	mi = new JMenuItem("Show parent and siblings");
	mi.addActionListener(new CustomMenuListener(0,3));
	popup.add(mi);
	
	popup.addSeparator();
	
	mi = new JMenuItem("Hide all others");
	mi.addActionListener(new CustomMenuListener(0,4));
	popup.add(mi);
	
	mi = new JMenuItem("Show all others");
	mi.addActionListener(new CustomMenuListener(0,5));
	popup.add(mi);
	
	popup.addSeparator();

	if( mview.addExternalPopupEntries(popup) > 0 )
	    popup.addSeparator();

	popup.add(custom_menu.createMenu());

	return popup;
    }

    public JPopupMenu makeMeasNameMenu()
    {
   	JPopupMenu popup = new JPopupMenu();
	
	popup.add( makeSelectMenu(2,23,6) );
	
	popup.addSeparator();

	JMenuItem mi = new JMenuItem("Hide this Measurement");
	mi.addActionListener(new CustomMenuListener(2,0));
	popup.add(mi);
	
	mi = new JMenuItem("Show all Measurements");
	mi.addActionListener(new CustomMenuListener(2,1));
	popup.add(mi);
	
	popup.addSeparator();

	mi = new JMenuItem("Show Properties");
	mi.addActionListener(new CustomMenuListener(2,2));
	popup.add(mi);
	
	mi = new JMenuItem("Pick Colouriser");
	mi.addActionListener(new CustomMenuListener(2,3));
	popup.add(mi);
	
	mi = new JMenuItem("Show Attributes");
	mi.addActionListener(new CustomMenuListener(2,4));
	popup.add(mi);
	
	/*
	mi = new JMenuItem("Sort this column");
	mi.addActionListener(new CustomMenuListener(2,10));
	popup.add(mi);
	*/
	
	popup.addSeparator();

	if( mview.addExternalPopupEntries(popup) > 0 )
	    popup.addSeparator();

	popup.add(custom_menu.createMenu());

	return popup;
    }

    public JPopupMenu makeSpotMenu()
    {
	JPopupMenu popup = new JPopupMenu();
	
	popup.add( makeSelectMenu(1,20,6) );
	
	popup.addSeparator();

	JMenuItem mi = new JMenuItem("Display annotation");
	mi.addActionListener(new CustomMenuListener(3,0));
	popup.add(mi);
	
	mi = new JMenuItem("Edit Names & Attrs");
	mi.addActionListener(new CustomMenuListener(3,1));
	popup.add(mi);
	
	mi = new JMenuItem("Sort ascending");
	mi.addActionListener(new CustomMenuListener(3,10));
	popup.add(mi);

	mi = new JMenuItem("Sort descending");
	mi.addActionListener(new CustomMenuListener(3,11));
	popup.add(mi);

	popup.addSeparator();
	
	if( mview.addExternalPopupEntries(popup) > 0 )
	    popup.addSeparator();

	popup.add(custom_menu.createMenu());
	
	popup.addSeparator();
	
	spot_popup_panel = new JPanel();
	popup.add(spot_popup_panel);
	
	return popup;
    }


    private final void addToMenu(JMenu menu, String name, int code, int send_code)
    {
	JMenuItem mi = new JMenuItem(name);
	mi.addActionListener(new CustomMenuListener(code,send_code));
	menu.add( mi );
    }

    private JMenu makeSelectMenu(int mode, int code, int send_code)
    {
	JMenu select_menu =  new JMenu("Selection");
	
	if(mode == 1) // name col, spot
	{
	    addToMenu(select_menu, "Select all", code, 3);
	    addToMenu(select_menu, "Invert selection", code, 1);
	    addToMenu(select_menu, "Unselect all", code, 0);

	    addToMenu(select_menu, "Add filtered spots", code, 4);
	    addToMenu(select_menu, "Remove filtered spots", code, 5);
	}
	if(mode == 2) // meas name
	{
	    addToMenu(select_menu, "Select all", code, 2);
	    addToMenu(select_menu, "Invert selection", code, 1);
	    addToMenu(select_menu, "Unselect all", code, 0);

	    select_menu.addSeparator();
	    addToMenu(select_menu, "Hide selected",   code, 3);
	    addToMenu(select_menu, "Hide unselected", code, 4);
	}
	if(mode == 3) // spot cluster
	{
	    addToMenu(select_menu, "Unselect all", code, 0);
	    addToMenu(select_menu, "Convert to Spot selection", code, 1);
	}
	if(mode == 4) // measurement cluster
	{
	    addToMenu(select_menu, "Unselect all", code, 0);
	    addToMenu(select_menu, "Convert to Measurement selection", code, 1);
	}

	select_menu.addSeparator();
	
	JMenu targets_menu = new JMenu("Send to...");
	JMenuItem mi = null;

	Vector rdsv = edata.getRemoteDataSinks();
	for(int r=0; r < rdsv.size(); r++)
	{
	    mi = new JMenuItem( ((ExprData.RemoteDataSink) rdsv.elementAt(r)).getName() );
	    mi.addActionListener(new CustomMenuListener(send_code,r));
	    targets_menu.add(mi);
	}

	Vector edsv = edata.getExternalDataSinks();
	for(int e=0; e < edsv.size(); e++)
	{
	    mi = new JMenuItem( ((ExprData.ExternalDataSink) edsv.elementAt(e)).getName() );
	    mi.addActionListener(new CustomMenuListener(send_code+1,e));
	    targets_menu.add(mi);
	}

	if( ( rdsv.size() + edsv.size() ) == 0)
	    targets_menu.setEnabled( false );

	select_menu.add(targets_menu);

	return select_menu;
    }

	// ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  ==  
	
	
    // ---------------- --------------- --------------- ------------- ------------
    // popup spot information viewer
	
    final int max_label_len = 32;

    public void populateSpotInfoViewer(boolean is_name, int spot_id, int meas_id)
    {
	spot_popup_panel.removeAll();

	String pname = edata.getProbeNameAtIndex(spot_id);
	String gname = edata.getGeneNameAtIndex(spot_id);
	
	JLabel label = new JLabel("dummy");
	Font f = label.getFont();
	Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);

	Color label_col = label.getForeground().brighter();
	
	GridBagConstraints c = null;
	GridBagLayout gbag = new GridBagLayout();
	spot_popup_panel.setLayout(gbag);
	
	int line = 0;
	
	label = new JLabel(" Gene name(s): ");
	label.setFont(small_font);
	label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = line;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);
	
	// break very long gene names into chunks....
	line += addMultiLineLabel(spot_popup_panel, gbag, line, max_label_len,  small_font, edata.getGeneNameAtIndex(spot_id));
	
	/*
	  String gname_remaining =;
	  while(gname_remaining.length() > 0)
	  {
	  int this_time = (gname_remaining.length() > 40) ? 40 : gname_remaining.length();
	  String portion = gname_remaining.substring(0, this_time);
	  
	  label = new JLabel(portion);
	  c = new GridBagConstraints();
	  c.anchor = GridBagConstraints.WEST;
	  c.gridx = 1;
	  c.gridy = line++;
	  gbag.setConstraints(label, c);
	  panel.add(label);
	  
	  gname_remaining = gname_remaining.substring(this_time);
	  }
	*/
	
	label = new JLabel(" Probe name: ");
	label.setFont(small_font);
	label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = line;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);
	
	// break very long probe names into chunks....
	line += addMultiLineLabel(spot_popup_panel, gbag, line, max_label_len, small_font, edata.getProbeNameAtIndex(spot_id));
	
	/*
	  label = new JLabel(edata.getProbeNameAtIndex(spot_id));
	  c = new GridBagConstraints();
	  c.anchor = GridBagConstraints.WEST;
	  c.gridx = 1;
	  c.gridy = line++;
	  gbag.setConstraints(label, c);
	  panel.add(label);
	*/
	/*
	label = new JLabel(" Spot comment: ");
	label.setFont(small_font);
	label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = line;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);
	
	// break very long spot names into chunks....
	line += addMultiLineLabel(spot_popup_panel, gbag, line, max_label_len, small_font, edata.getSpotCommentAtIndex(spot_id));
	*/

	label = new JLabel(" Spot name: ");
	label.setFont(small_font);
	label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = line;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);
	
	// break very long spot names into chunks....
	line += addMultiLineLabel(spot_popup_panel, gbag, line, max_label_len, small_font, edata.getSpotNameAtIndex(spot_id));

	if(is_name == false)
	{
	    label = new JLabel(" Measurement: ");
	    label.setFont(small_font);
	    label.setForeground(label_col);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    gbag.setConstraints(label, c);
	    spot_popup_panel.add(label);
	    
	    label = new JLabel(edata.getMeasurementName(meas_id));
	    label.setFont(small_font);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 1;
	    c.gridy = line++;
	    gbag.setConstraints(label, c);
	    spot_popup_panel.add(label);
	
	    label = new JLabel(" Value: ");
	    label.setFont(small_font);
	    label.setForeground(label_col);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    gbag.setConstraints(label, c);
	    spot_popup_panel.add(label);
	    
	    label = new JLabel(String.valueOf(edata.eValue(meas_id, edata.getSpotAtIndex(spot_id))));
	    label.setFont(small_font);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 1;
	    c.gridy = line++;
	    gbag.setConstraints(label, c);
	    spot_popup_panel.add(label);
	}
	
	label = new JLabel(" Colouriser: " );
	label.setFont(small_font);
	label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = line;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);
	
	Colouriser col = getColouriserForMeasurement(meas_id);
	label = new JLabel(col == null ? "" : col.getName());
	label.setFont(small_font);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 1;
	c.gridy = line++;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);

	/*
	label = new JLabel(" Annotation: " );
	label.setFont(small_font);
	label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = line;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);

	String ann_stat = mview.getAnnotationLoader().isCached(pname) ? "cached" : "not cached";
	
	label = new JLabel(ann_stat);
	label.setFont(small_font);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 1;
	c.gridy = line++;
	gbag.setConstraints(label, c);
	spot_popup_panel.add(label);
	*/


	spot_popup_panel.updateUI();
	spot_popup.updateUI();
	
    }

    private int addMultiLineLabel(final JPanel panel, final GridBagLayout gbag, 
				  final int line,  final int max_len,  
				  Font font, 
				  final String src)
    {
	if(src == null)
	    return 1;
	
	int line_count = 0;
	
	String src_remaining = null;

	if(src.length() > 128)
	    src_remaining = src.substring(0, 128) + "...";
	else
	    src_remaining = src;
	
	while(src_remaining.length() > 0)
	{
	    int this_time = (src_remaining.length() > max_len) ? max_len : src_remaining.length();
	    String portion = src_remaining.substring(0, this_time);
	    
	    JLabel label = new JLabel(portion);
	    label.setFont(font);
	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 1;
	    c.gridy = line + line_count;
	    gbag.setConstraints(label, c);
	    panel.add(label);
	    
	    src_remaining = src_remaining.substring(this_time);
	    line_count++;
	}
	return line_count;
    }
    
    // ---------------- --------------- --------------- ------------- ------------
    // name editor
    
    private void startNameEditor(final int x, final int y, final int spot_id)
    {
	new TagEditor(mview, x, y, spot_id);
    }
    


    // ---------------- --------------- --------------- ------------- ------------

    private Color text_col;
    private Color background_col;

    private boolean first_one;

    private int[] meas_in_col;
    private Hashtable col_for_meas_ht;

    private int[] spot_id_in_row = null;
    private Hashtable row_for_spot_id = null;

    private int[] spot_cluster_width = null;
    private int[] meas_cluster_height = null;

    // private Vector[] clusters_in_row = null;

    private Polygon[] glyph_poly = null;

    private Hashtable clust_elpos_cache = null;

    private JScrollBar vert_sb, hor_sb;
    private DataPlotPanel dplot_panel;

    JFrame spot_popup_frame = null;

    protected ExprData edata;
    protected maxdView mview;

    private int pos, min, max, count;

    private int cluster_top_pos;

    private int n_visible_meas;
    private int n_possible_meas;

    private int current_hor_sb_value;
    private int current_ver_sb_value;

    private int n_visible_spots;

    private boolean first_paint = true;
    private boolean show_logo = false;

    private javax.swing.Timer logo_ticker = null;
    private int logo_paint_chunks = 0;
    private int logo_painter = -1;
    private int logo_pos = 0;

    private ImageIcon logo_ii = null;
}
