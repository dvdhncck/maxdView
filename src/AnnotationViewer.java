import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.net.URL;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import java.io.*;

/*
    import java.awt.datatransfer.*;
    import java.awt.dnd.*;
*/

public class AnnotationViewer 
    extends JFrame 
    implements ExprData.ExprDataObserver
{
    public AnnotationViewer(maxdView mview_, AnnotationLoader al_)
    {
	super("Annotation Viewer");


	//System.out.println("++ AnnotationViewer is constructed ++");
	//dragSource = new DragSource();
	//dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_COPY_OR_MOVE, this);

	mview = mview_;
	al = al_;

	mview.decorateFrame( this );

	addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  saveProps();
				  mview.annotationViewerHasClosed(AnnotationViewer.this);
			      }
			  });

	addComponents();
	pack();
	setVisible(true);

	//edata.addObserver(this);
    }

    public void cleanUp()
    {
	//edata.removeObserver(this);
    }

    public void saveProps()
    {
	// System.out.println("w=" + view_scroller.getWidth() +  " h=" + view_scroller.getHeight());

	mview.putIntProperty("AnnotationViewer.width", wrapper_panel.getWidth());
	mview.putIntProperty("AnnotationViewer.height", wrapper_panel.getHeight());
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
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  interface to AnnotationLoader
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void setStatusText(String s)
    {
	//status_label.setText(s);
	//name_label.setText(s);
	//html_panel.requestFocus();
    }

    /*
    public void appendPanelText(String s)
    {
	System.out.println("appending ' " + s + "'");

	String tmp = html_panel.getText();
	if(tmp == null)
	{
	    tmp = s;
	}
	else
	{
	    tmp += s;
	}
	if(html_panel != null)
	{
	    System.out.println("setting text to: '" + tmp + "'");
	    html_panel.setContentType("text/plain");
	    if(tmp != null)
		html_panel.setText(tmp);
	    html_panel.setContentType("text/plain");
	}


	//System.out.println("text is now: " + tmp);
    }
    */

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  interface to expression viewer
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private String convertToHTML(String raw)
    {
	if(raw.indexOf("<HTML") < 0)
	{
	    StringBuffer rbuf = new StringBuffer();
	    rbuf.append("<HTML><BODY><PRE>");
	    rbuf.append(raw);
	    rbuf.append("</PRE></BODY></HTML>");
	    return rbuf.toString();
	}
	else
	{
	    return raw;
	}
    }

    int pending_loads = 0;

    // called by loader when a source has delivered part of the annotation
    public void displayAnnotationPart(int p, String name, String ann)
    {
	//System.out.println("AV: part " + p + " received");

	if(p >= n_panels)
	{
	    // this is probably an old request finally being satisified...
	    System.out.println("old part " + p + "ignored..");
	    return;
	}

	if(--pending_loads <= 0)
	{
	    //System.out.println("AV: all parts received");
	    stopAnimation();
	}

	if((ann != null) && (ann.length() > 0))
	{
	   try
	   {
		 html_panel[p].setContentType("text/html");

		 html_panel[p].setText( convertToHTML(ann) );

		 panel_title[p].setText(name + "  (" + ann.length() + " chars)");
	    }
	   catch(Throwable e)
	   {
	       panel_title[p].setText("Error: " + e.toString());
	       System.out.println("exception in panel " + p + " : stack trace:\n");
	       e.printStackTrace();
	       try
	       {
		   html_panel[p].setText("Error: " + e.toString());
	       }
	       catch(Exception e2)
	       {
	       }
	   }
	}
	else
	{
	    panel_title[p].setText("(empty result)");
	    html_panel[p].setText("");
	}
	
	view_scroller[p].scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }

    public void setSpot(String spot_name, boolean reload)
    {
	startAnimation();
	
	//String probe_name = mview.getExprData().getProbeName);
	
	int spot_id = mview.getExprData().getIndexBySpotName(spot_name);

	current_spot_name = spot_name;

	setupHTMLPanels( al.getNumPartsExpected( spot_id, reload ) );

	for(int p=0; p < n_panels; p++)
	{
	    panel_title[p].setText("Loading....");
	    html_panel[p].setText("");
	}

	pending_loads = n_panels;

	al.requestLoadAnnotationInParts(this, spot_id, reload);
	
	//System.out.println("AV: load requested");
    }

    private String current_spot_name = null;

    // display the annotation for the specified spot
    //
    public void setSpot(String spot_name)
    {
	setSpot(spot_name, false);
    }

    public boolean isLocked() { return locked; }

    public void lock() 
    {
	locked = true; 
	lock_jb.setIcon(locked_image);
    }
    
    public void goBack()
    {

    }

    public void gotoURL(URL url, int panel_id)
    {
	pending_loads++;
	startAnimation();
	panel_title[panel_id].setText("Loading...");
	//html_panel[panel_id].setText("");
	new GotoURLThread(url, panel_id).start();
    }

    public class GotoURLThread extends Thread
    {
	private URL url;
	private int panel_id;

	public GotoURLThread(URL url_, int panel_id_)
	{
	    url = url_;
	    panel_id = panel_id_;
	}

	public void run()
	{
	    System.out.println("GotoURLThread() loading " + url + " for panel " + panel_id);

	    if(locked)
	    {
		AnnotationViewer new_av = new AnnotationViewer(mview, al);
		new_av.gotoURL(url, panel_id);
	    }
	    else
	    {
		try 
		{
		    // initForHTML();
		    html_panel[panel_id].setPage(url);
		    html_panel[panel_id].setContentType("text/html");
		    panel_title[panel_id].setText(url.toString());
		    // setStatusText(url.toString());
		} 
		catch(java.io.FileNotFoundException fnfe)
		{
		    mview.alertMessage("gotoURL: panel=" + panel_id + " url=" + url.toString() + "\n  " + fnfe);
		}
		catch(java.lang.NullPointerException npe)
		{
		    npe.printStackTrace();
		    mview.alertMessage("gotoURL:\n panel=" + panel_id + "\n url=" + url.toString() + "\nNullPointerException absorbed");
		}
		catch(java.util.EmptyStackException ese)
		{
		    mview.alertMessage("gotoURL:\n panel=" + panel_id + "\n url=" + url.toString() + "\nEmptyStackException absorbed");
		}
		catch (Exception e) 
		{
		    mview.alertMessage("gotoURL: unhandled exception whilst loading\nfrom URL " + url.toString() + "\n  " + e);
		}
	    }
	    if(--pending_loads == 0)
		stopAnimation();
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	getContentPane().setLayout(new BorderLayout());
	JToolBar bottom_tool_bar = new JToolBar();

	
	{
	    locked_image = new ImageIcon(mview.getImageDirectory() + "locked.gif");
	    unlocked_image = new ImageIcon(mview.getImageDirectory() + "unlocked.gif");
	}

	{
	    // the popup menu
	    //
	    popup = new JPopupMenu();
	    customMenuListener menu_listener = new customMenuListener(0);

	    back_mi = new JMenuItem("Back");
	    back_mi.addActionListener(menu_listener);
	    back_mi.setEnabled(false);
	    popup.add(back_mi);

	    reload_mi = new JMenuItem("Reload");
	    reload_mi.addActionListener(menu_listener);
	    popup.add(reload_mi);

	    popup.addSeparator();

	    save_mi = new JMenuItem("Save text");
	    save_mi.addActionListener(menu_listener);
	    popup.add(save_mi);

	    create_cluster_mi = new JMenuItem("Find occurences");
	    create_cluster_mi.addActionListener(menu_listener);
	    popup.add(create_cluster_mi);

	}

	{

	    int pw = mview.getIntProperty("AnnotationViewer.width", 500);
	    int ph = mview.getIntProperty("AnnotationViewer.height", 200);
	    
	    wrapper_panel = new JPanel();
	    wrapper_panel.setPreferredSize(new Dimension(pw, ph));
	    getContentPane().add(wrapper_panel);	

	    setupHTMLPanels(1);
	}

	{
	    // the toolbar
	    //
	    bottom_tool_bar.setFloatable(false);
	    bottom_tool_bar.setBackground(Color.white);
	    GridBagLayout gridbag = new GridBagLayout();
	    bottom_tool_bar.setLayout(gridbag);

	    {
		lock_jb = new JButton(unlocked_image);
		bottom_tool_bar.add(lock_jb);
		lock_jb.addActionListener(new ActionListener() 
					  {
					      public void actionPerformed(ActionEvent e) 
					      {
						  if(locked)
						  {
						      locked = false;
						      //lock_jb.setText(" Lock ");
						      lock_jb.setIcon(unlocked_image);
						  }
						  else
						  {
						      locked = true;
						      //lock_jb.setText("Unlock"); 
						      lock_jb.setIcon(locked_image);
						  }
					      }
					  });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		//c.weightx = 1.0;
		gridbag.setConstraints(lock_jb, c);
	    }
	    {
		final JButton options_jb = new JButton(" Options ");
		bottom_tool_bar.add(options_jb);
		options_jb.addActionListener(new ActionListener() 
					   {
					       public void actionPerformed(ActionEvent e) 
					       {
						   createOptionsDialog();
					       }
					   });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.VERTICAL;
		c.weightx = 1.0;
		gridbag.setConstraints(options_jb, c);
	    }
	    
	    {
		animate_panel = new JPanel();
		animate_panel.setBackground(Color.white);
		animate_i = new ImageIcon(mview.getImageDirectory() + "maxdViewscale.jpg").getImage();
		animate_panel.setPreferredSize(new Dimension(image_pw, image_ph));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1.0;
		gridbag.setConstraints(animate_panel, c);
		bottom_tool_bar.add(animate_panel);

		animate_timer = new Timer(100, new ActionListener() 
		    {
			public void actionPerformed(ActionEvent evt) 
			{
			    int ay = (animation_cycle > animation_frames) ? ((2*animation_frames)-animation_cycle) : animation_cycle;
			    int py = ay * image_ph;
			    Graphics g = animate_panel.getGraphics();
			    // System.out.println("frame " + animation_cycle + " ay=" + ay + " -> " + py + "..." + (py+image_ph-1));
			    g.drawImage(animate_i,
					0, 0,  image_pw, image_ph, 
					0, py, image_pw, py+image_ph,
					null);
				
			    if(animate_timer_stop == true)
				if(animation_cycle == 0)
				    animate_timer.stop();
			    
			    if(++animation_cycle >= (2*animation_frames))
			    {
				animation_cycle = 0;
			    }
			}
		    });

	    }

	    {
		final JButton options_jb = new JButton(" Help ");
		bottom_tool_bar.add(options_jb);
		options_jb.addActionListener(new ActionListener() 
					   {
					       public void actionPerformed(ActionEvent e) 
					       {
						   mview.getHelpTopic("AnnotationViewer");
					       }
					   });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.VERTICAL;
		c.weightx = 1.0;
		gridbag.setConstraints(options_jb, c);
	    }
	    {
		final JButton close_jb = new JButton(" Close ");
		bottom_tool_bar.add(close_jb);
		close_jb.addActionListener(new ActionListener() 
					   {
					       public void actionPerformed(ActionEvent e) 
					       {
						   saveProps();
						   mview.annotationViewerHasClosed(AnnotationViewer.this);
						   setVisible(false);
					       }
					   });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.VERTICAL;
		//c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(close_jb, c);
	    }
	}

	getContentPane().add(bottom_tool_bar, BorderLayout.SOUTH);
    }

    JPanel animate_panel;
    Image animate_i;
    Timer animate_timer = null;

    final int image_pw = 114;
    final int image_ph = 20;
    int animation_cycle = 0;
    final int animation_frames = 8;
    boolean animate_timer_stop = false;

    private void startAnimation()
    {
	if(!animate_timer.isRunning())
	{
	    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    animate_timer_stop = false;
	    animate_timer.start();
	}
    }
    private void stopAnimation()
    {
	if(animate_timer.isRunning())
	{
	    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    //animate_timer.stop();
	    animate_timer_stop = true;
	}
    }
    
    private int n_panels = 0;
    private JPanel wrapper_panel;

    private void setupHTMLPanels(final int new_n_panels)
    {
	if(new_n_panels == n_panels)
	    return;

	n_panels = new_n_panels;
	wrapper_panel.removeAll();

	GridBagLayout gridbag = new GridBagLayout();
	wrapper_panel.setLayout(gridbag);

	html_panel = new DropHTMLPane[n_panels];
	view_scroller = new JScrollPane[n_panels];
	panel_title = new JLabel[n_panels];

	GridBagConstraints c = null;
	
	Component com = makeHTMLPanels( n_panels, 0 );

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	gridbag.setConstraints(com, c);
	wrapper_panel.add(com);	
	
	wrapper_panel.updateUI();
	//System.out.println("setup for " + n_panels + " panels");
    }

    private Component makeOneHTMLPanel( int pid )
    {
	//System.out.println("making panel id= " + pid );

	JPanel pwrap = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = null;
	pwrap.setLayout(gridbag);

	panel_title[pid] = new JLabel("Panel " + pid);
	
	html_panel[pid] = new DropHTMLPane();
	html_panel[pid].setEditable(false);
	
	dropTarget = new DropTarget (html_panel[pid], DnDConstants.ACTION_COPY_OR_MOVE, html_panel[pid]);
	
	html_panel[pid].addHyperlinkListener(new CustomLinkListener(pid));
	
	MouseListener popupListener = new PopupListener(pid);
	html_panel[pid].addMouseListener(popupListener);

	view_scroller[pid] = new JScrollPane(html_panel[pid]);

	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(panel_title[pid], c);
	pwrap.add(panel_title[pid]);
	
	c = new GridBagConstraints();
	c.gridy = 1;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	gridbag.setConstraints(view_scroller[pid], c);
	pwrap.add(view_scroller[pid]);

	return pwrap;
    }

    private Component makeHTMLPanels(final int total_panels,  final int start_pid)
    {
	if(total_panels == 1)
	{
	    return makeOneHTMLPanel( start_pid );
	}
	else
	{
	    JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	    
	    jsp.setTopComponent( makeOneHTMLPanel( start_pid ));

	    jsp.setBottomComponent(  makeHTMLPanels( total_panels-1, start_pid+1 ) );

	    double pl = (double) 1.0 / (double) (n_panels-start_pid);

	    jsp.setDividerLocation(pl);

	    return jsp;
	}
    }

    JPopupMenu popup; 

    class PopupListener extends MouseAdapter 
    {
	private int panel_id;
	public PopupListener(int panel_id_)
	{
	    panel_id = panel_id_;
	}

	public void mousePressed(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
	

	private void maybeShowPopup(MouseEvent e) 
	{
	    if (e.isPopupTrigger() || e.isAltDown() || e.isAltGraphDown() || e.isControlDown()) 
	    {
		// set the configuration of the menus based on context
		//
		String str = html_panel[panel_id].getText();
		create_cluster_mi.setEnabled((str != null) && (str.length() > 0));
		    

		popup.show(e.getComponent(),
			   e.getX(), e.getY());
	    }
	}
    }

    class customMenuListener implements ActionListener
    {
	private int panel_id;
	public customMenuListener(int panel_id_)
	{
	    panel_id = panel_id_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    JMenuItem source = (JMenuItem)(e.getSource());
	    if(source == create_cluster_mi)
	    {
		//System.out.println("creating a cluster for " + html_panel.getSelectedText());
		createClusterFromAnnotation(html_panel[panel_id].getSelectedText());
	    }
	    if(source == search_text_mi)
	    {
		createFindTextDialog();
	    }
	    if(source == save_mi)
	    {
		saveText(panel_id);
	    }
	    if(source == reload_mi)
	    {
		if(current_spot_name != null)
		{
		    // System.out.println("reload " + current_spot_name);
		    setSpot(current_spot_name, true);
		}

	    }
	}
    }
    
    public class CustomLinkListener implements HyperlinkListener 
    {
	private int panel_id;
	private String cur_title;

	public  CustomLinkListener(int panel_id_)
	{
	    panel_id = panel_id_;
	}

	public void hyperlinkUpdate(HyperlinkEvent e) 
	{
	    // System.out.println("hyperlink event in panel " + panel_id + " : url is " + e.getURL());

	    if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) 
	    {
		cur_title = panel_title[panel_id].getText();
		
		String new_text = (e.getURL() != null) ? e.getURL().toString() : "(empty link)";

		panel_title[panel_id].setText( new_text );
		html_panel[panel_id].setToolTipText( new_text );

		return;
	    }
	    if (e.getEventType() == HyperlinkEvent.EventType.EXITED) 
	    {
		panel_title[panel_id].setText(cur_title);
		html_panel[panel_id].setToolTipText( null);
		return;
	    }
	    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) 
	    {
		// System.out.println("hyperlink me up to " + e.getURL());

		if (e instanceof HTMLFrameHyperlinkEvent) 
		{
		    HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
		    HTMLDocument doc = (HTMLDocument) html_panel[panel_id].getDocument();
		    doc.processHTMLFrameHyperlinkEvent(evt);

		    // System.out.println("framed hyperlink me up to " + e.getURL() + " (HTMLFrameHyperlinkEvent)");

		    // saveURLInHistory( e.getURL() );
		} 
		else 
		{
		    try 
		    {
			// System.out.println("hyperlink me up to " + e.getURL());

			URL url = e.getURL();

			if(url != null)
			    gotoURL(url, panel_id);
			else
			    mview.alertMessage("Empty link");
		    }
		    catch (Throwable t) 
		    {
			t.printStackTrace();
		    }
		}

	    }
	}
    }

    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // save
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //

    public final void saveText(int panel)
    {
	try
	{
	    //int mode = mview.infoQuestion("Save just this panel, or all panels?", "This one", "All of them");
	    
	    String[] s_options = new String[panel_title.length];

	    for(int s=0;s < panel_title.length; s++)
		s_options[s] = panel_title[s].getText();
	    
	    int[] pans = mview.getChoice("Save which sources?", s_options, null, -1, true);
	    
	    JFileChooser jfc = mview.getFileChooser();
	    int val = jfc.showSaveDialog(null);
	    
	    if(val == JFileChooser.APPROVE_OPTION) 
	    {
		File file = jfc.getSelectedFile();
		try
		{
		    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		    
		    for(int p=0; p < pans.length; p++)
			savePanelText(writer, pans[p]);
		    
		    writer.flush();
		}
		catch(java.io.IOException ioe)
		{
		    mview.alertMessage("File '" + file.getName() + "' cannot be written\nError: " + ioe);
		    return;
		}
	    }
	}
	catch(UserInputCancelled uic)
	{

	}
    }
    
    private void savePanelText(BufferedWriter writer, int panel) throws java.io.IOException
    {
	writer.write(html_panel[panel].getText() + "\n");
	// System.out.println("text of panel "+ panel + " written");
    }
    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // the options dialog frame (which actually comes from the AnnotationLoader)
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //

    private void createOptionsDialog()
    {
	al.createOptionsDialog();
    }

    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // functions on the popup menu...
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //

    private boolean abort_scan = false;

    // search all of the annotations for all of the current spots and 
    //  place any spot containing the string into a new cluster
    //

    public void createClusterFromAnnotation(String ann)
    {
	if((ann == null) || (ann.equals("")))
	    return;
	new AnnotationSearcher(ann).start();
    }

    public class AnnotationSearcher extends Thread
    {
	private String ann;

	public AnnotationSearcher(String ann_)
	{
	    ann = ann_;
	}
	public void run()
	{
	    // strip any leading or trailing spaces...
	    //
	    /*
	    Vector elems = new Vector();
	    ExprData edata = mview.getExprData();
	    
	    ProgressOMeter pm = new ProgressOMeter("Scanning annotations",2);
	    pm.setCancelAction(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			System.out.println("cancelled!");
			abort_scan = true;
		    }
		});
	    
	    abort_scan = false;
	    
	    pm.startIt();
	    
	    ExprData.Cluster new_cl = edata.new Cluster();
	    
	    String ann_lc = ann.toLowerCase();
	    
	    for(int s=0; s < edata.getNumSpots(); s++)
	    {
		if(abort_scan)
		    break;
		
		pm.setMessage(1, s + " of " + edata.getNumSpots());
		
		String str = al.loadAnnotation(AnnotationViewer.this, s);
		if(str != null)
		{
		    String str_lc = str.toLowerCase();
		    if(str_lc.indexOf(ann_lc) >= 0)
			elems.addElement(new Integer(s));
		}
	    }
	    
	    pm.stopIt();
	    
	    if(!abort_scan)
	    {
		if(elems.size() > 1)
		{
		    new_cl.setName(ann);
		    new_cl.setElements(ExprData.SpotIndex, elems);
		    edata.addCluster(new_cl);
		    //mview.informationMessage("Cluster '" + ann + "' with " + elems.size() + " created");
		    //mview.addMessageToLog("'" + ann + "' found " + elems.size() + " times");
		    mview.informationMessage("'" + ann + "' found " + elems.size() + " times");
		}
		else
		{
		    mview.informationMessage("This is the only occurrence of '" + ann + "'");
		}
	    }
	    else
		mview.informationMessage("Search cancelled");
	    */
	    mview.informationMessage("Not implemented");
	}
    }

    public void createFindTextDialog()
    {
	
    }
    
    private void createLoadURLDialog()
    {

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  dragon drop
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    //
    // ======================================================
    //  DropTargetListener interface
    // ======================================================
    //

    public void dragEnter (DropTargetDragEvent event) 
    {
	event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    public void dragExit (DropTargetEvent event) 
    {
    }
    
    public void dragOver (DropTargetDragEvent event) 
    {
	event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public void drop (DropTargetDropEvent event) 
    {
	System.out.println( "trying to drop....");
	
	try 
	{
	    Transferable transferable = event.getTransferable();

	    DragAndDropEntity dnde = null;
	    String text = null;
		
	    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
	    {
		
		dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);
		
		event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		
		System.out.println( "'" + dnde.getEntityType() + "' dropped");
		System.out.println( "name is '" + dnde.toString() + "'");
		
		try
		{
		    String sname = dnde.getSpotName(mview.getExprData());
		    setSpot(sname);
		}
		catch(DragAndDropEntity.WrongEntityException wee)
		{
		}

		event.getDropTargetContext().dropComplete(true);
		return;
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

	event.rejectDrop();
    }

    public void dropActionChanged ( DropTargetDragEvent event ) 
    {
    }
    
    //
    // ======================================================
    //  DragGestureListener interface
    // ======================================================
    //

    public void dragGestureRecognized( DragGestureEvent event) 
    {
	System.out.println( " dragGesturedRecognized() start....");
	
	//Object selected = getSelectedValue();
	//if (selected != null)
	{
	    //System.out.println( " drag start....");

	    //DragAndDropEntity dnde = (DragAndDropEntity) dnde_v.elementAt(getSelectedIndex());
	    
	    //dragSource.startDrag (event, DragSource.DefaultMoveDrop, dnde, this);
	} 
    }
    
    //
    // ======================================================
    //  DragSourceListener interface
    // ======================================================
    //

    public void dragDropEnd (DragSourceDropEvent event) 
    {   
    }
    
    public void dragEnter (DragSourceDragEvent event) 
    {
    }
    
    public void dragExit (DragSourceEvent event) 
    {
    }
    
    public void dragOver (DragSourceDragEvent event) 
    {
    }
    
    public void dropActionChanged ( DragSourceDragEvent event) 
    {
    }
    

    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  droppable JEditorPane
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private class DropHTMLPane extends JEditorPane implements DropTargetListener
    {

	public void dragEnter (DropTargetDragEvent event) 
	{
	    event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
	}
	
	public void dragExit (DropTargetEvent event) 
	{
	}
	
	public void dragOver (DropTargetDragEvent event) 
	{
	    event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
	}
	
	public void drop (DropTargetDropEvent event) 
	{
	    // System.out.println( "trying to drop....");
	    
	    try 
	    {
		Transferable transferable = event.getTransferable();
		
		DragAndDropEntity dnde = null;
		String text = null;
		
		if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
		{
		    
		    dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);
		    
		    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		    
		    //System.out.println( "'" + dnde.getEntityType() + "' dropped");
		    //System.out.println( "name is '" + dnde.toString() + "'");
		    
		    try
		    {
			String sname = dnde.getSpotName(mview.getExprData());
			setSpot(sname);
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		    
		    event.getDropTargetContext().dropComplete(true);
		    return;
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
	    
	    event.rejectDrop();
	}
	
	public void dropActionChanged ( DropTargetDragEvent event ) 
	{
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  state
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    DropTarget dropTarget = null;
    DragSource dragSource = null;

    private boolean locked = false;

    private ImageIcon locked_image = null;
    private ImageIcon unlocked_image = null;
    
    private JButton lock_jb;

    private JMenuItem save_mi, search_text_mi, create_cluster_mi, reload_mi, back_mi;

    private maxdView         mview;
    private AnnotationLoader al;
    private DropHTMLPane[]   html_panel;
    private JScrollPane[]    view_scroller;
    private JLabel[]         panel_title;

    //private JLabel           name_label;
    private JLabel           status_label; 

}
