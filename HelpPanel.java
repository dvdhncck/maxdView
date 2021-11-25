import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.io.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.text.html.*;
import java.util.Vector;
import java.util.Enumeration;  
import java.util.Hashtable;

import java.util.zip.*;

public class HelpPanel extends JFrame 
{
    public HelpPanel(maxdView mview_, String topic)
    {
	
	super("maxdView Help");

	mview = mview_;
	
	outer = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	outer.setLayout(gridbag);

	addKeyListener(new CustomKeyListener());

	//outer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	getContentPane().add(outer);

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

	    
	    { 
		JButton button = new JButton(" Find ");
		button.setToolTipText("Search all help text");
		button.setFont(mview.getSmallFont());
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    displayFindDialog();
			}
		    });
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(button, c);
		wrapper.add(button);
	    }

	    { 
		ImageIcon ii = new ImageIcon( mview.getImageDirectory() + File.separator + "moveleft.gif" );

		back_button = new JButton( ii );

		back_button.setToolTipText("Go back to previous page");
		back_button.setFont(mview.getSmallFont());
		back_button.setEnabled(false);
		back_button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    goBackward();
			}
		    });
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.gridx = 2;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.VERTICAL;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(back_button, c);
		wrapper.add(back_button);
	    }

	    {
		history_jcb = new JComboBox();
		history_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(!history_grabbing_disabled)
			    {
				gotoPageFromHistory( history_jcb.getSelectedIndex() );
			    }
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.weightx = 4.0;
		// c.fill = GridBagConstraints.HORIZONTAL;
		wrapbag.setConstraints(history_jcb, c);
		wrapper.add(history_jcb);
	    }

	    { 
		ImageIcon ii = new ImageIcon( mview.getImageDirectory() + File.separator + "moveright.gif" );

		fore_button = new JButton(ii);

		fore_button.setToolTipText("Go forward to next page");
		fore_button.setFont(mview.getSmallFont());
		fore_button.setEnabled(false);
		fore_button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    goForward();
			}
		    });
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		c.weightx = 1.0;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.VERTICAL;
		c.anchor = GridBagConstraints.WEST;
		wrapbag.setConstraints(fore_button, c);
		wrapper.add(fore_button);
	    }

	    { 
		JButton button = new JButton(" Close ");
		button.setToolTipText("Close this window");
		button.setFont(mview.getSmallFont());
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    closeWindow();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		wrapbag.setConstraints(button, c);
		wrapper.add(button);
	    }
	    
	    {
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 10.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(wrapper, c);
		outer.add(wrapper);
	    }
	}

	main_editor_pane = new CustomEditorPane();

	main_editor_pane.addHyperlinkListener(new CustomLinkListener());
	main_editor_pane.setEditable(false);

	main_editor_scroll_pane = new JScrollPane(main_editor_pane);
        
	int w = mview.getIntProperty("HelpPanel.width",  800);
	int h = mview.getIntProperty("HelpPanel.height", 450);
	int spl = mview.getIntProperty("HelpPanel.split_pane_width", 200);

	index_tree = new JTree();
	populateIndexTree(index_tree);

	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	renderer.setOpenIcon(null);
	renderer.setClosedIcon(null);
	index_tree.setCellRenderer(renderer);

	index_tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged(TreeSelectionEvent e) 
		{
		    IndexTreeNode node = (IndexTreeNode) index_tree.getLastSelectedPathComponent();
		    
		    if(node == null) 
			return;
		    
		    // System.out.println("-> goto '" + node.url + "'");

		    if(!node.url.startsWith("http:"))
		    {
			if(!node.url.startsWith("file:"))
			    gotoTopic( "file:" + mview.getHelpDirectory() + node.url);
			else
			    gotoTopic( node.url );
		    }
		    else
			gotoTopic( node.url );
		}
	    });

	JScrollPane index_jsp = new JScrollPane(index_tree);

	split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	
	// split_pane.setLeftComponent(index_editor_scroll_pane);

	split_pane.setLeftComponent(index_jsp);

	split_pane.setRightComponent(main_editor_scroll_pane);
	split_pane.setOneTouchExpandable(true);
	split_pane.setDividerLocation(spl);

	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 10.0;
	    c.weighty = 9.0;
	    
	    gridbag.setConstraints(split_pane, c);
	    outer.add(split_pane);
	}
	
	outer.setPreferredSize(new Dimension(w, h));

        setText("maxdView Help Browser v1.0");

	addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    closeWindow();
		}
	    });

	mview.decorateFrame(this);

	pack();
	setVisible(true);

	if(topic != null)
	    gotoTopic(topic);
	
	loadHistoryDataFromProps();
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
	    if(e.isShiftDown() || e.isControlDown() || e.isAltDown())
	    {
		switch(e.getKeyCode())
		{
		case KeyEvent.VK_LEFT:  // up-arrow
		    goBackward();
		    break;
		case KeyEvent.VK_RIGHT: 
		    goForward();
		    break;
		case KeyEvent.VK_HOME:
		    mview.getHelpTopic("Top");
		    break;
		}
	    }
	}
    }


    private void closeWindow()
    {
	mview.helpPanelHasClosed();
	
	mview.putIntProperty("HelpPanel.width", 
			     outer.getWidth());
	mview.putIntProperty("HelpPanel.height", 
			     outer.getHeight());
	mview.putIntProperty("HelpPanel.split_pane_width", split_pane.getDividerLocation());
	
	saveHistoryDataInProps();

	setVisible(false);
    }

    private void changeFont(int delta)
    {
	font_size += delta;

	Font f = new Font(mview.getDataPlot().getFont().getName(), Font.PLAIN, mview.getDataPlot().getFont().getSize());

	main_editor_pane.setFont(f);

	// System.out.println("font size = " + mview.getDataPlot().getFont().getSize());
    }

    public void gotoTopic(String topic)
    {
	gotoTopic(topic, null, null);
    }

    public void gotoTopic(String topic, String[] target, int[] sel_words)
    {
	if(topic == null)
	{
	    setText("gotoTopic(): topic was null");
	}
	else
	{
	    //System.out.println("goto topic: " + topic);

	    URL topic_url = null;
	    
	    try 
	    {
		topic_url = new URL(topic);
	    }
	    catch (Exception e) 
	    {
		setText("gotoTopic(): Couldn't create URL object for " + topic + "\n  " + e);
	    }
	    
	    if(topic_url == null)
	    {
		setText("gotoTopic(): null URL object for: " + topic + "\n");
	    }
	    else
	    {
		try 
		{
		    //System.err.println("goto topic: " + topic);
		    gotoURL(topic_url, 0, true, target, sel_words);
		}
		catch (Exception e) 
		{
		    setText("gotoTopic(): Couldn't open URL: " + topic + "\n  " + e);
		}
	    }
	}
    }

    public void gotoURL(URL topic_url, int y_pos, boolean add_to_history, String[] target, int[] sel_words)
    {
	if(topic_url == null)
	{
	    setText("gotoURL(): URL was null");
	}
	else
	{	
	    try 
	    {
		//System.out.println("goto URL: " + topic_url.toString());

		//System.out.println("there are now " + history.size() + " URLs in history list");

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    
		try
		{
		    displayURL(topic_url, main_editor_pane, y_pos, target, sel_words);
		    
		    waitForDocumentToLoad();
		
		    if(add_to_history)
		    {
			saveURLInHistory(topic_url);
		    }
		}
		catch( java.io.FileNotFoundException fnfe )
		{
		    // new in 1.0.5:
		    //
		    // perhaps the file couldn't be found because the refering hyperlink
		    // was in the user's 'private' help pages (e.g. a link from a locally
		    // installed plugin) which wanted to refer to something in the shared
		    // documentation directory.
		    //
		    // because it is impossible for the 'private' document to know the correct
		    // path to one of the shared documents, it will have used a relative path
		    // based on the assumption that the 'private' document is actually in the
		    // 'shared' docuemtn hierarchy (i.e. the way things used to be set up before
		    // the separation into shared and private).
		    //
		    // if we encounter a FileNotFound, we can try to reconstruct the url in terms
		    // of the real path to the shared directory, and try finding it again. If it is 
		    // still not found, then it's actually a 'proper' broken link.
		    //
		    
		    if( topic_url.getProtocol().equals( "file" ) )
		    {
			String local_file_path = topic_url.getFile();

			
			System.out.println( "HelpPanel: trying to convert suspected local 'private' into corresponding 'shared' path..." );
			System.out.println( "  private path=" + local_file_path );
			
			String prefix_to_remove= null;
			File test = new File( local_file_path );

			if( test.isAbsolute() )
			{
			    prefix_to_remove = mview.getUserSpecificDirectory();
			}
			else
			{
			    prefix_to_remove = ".." + File.separatorChar + ".." + File.separatorChar +  ".." ;
			}


			//
			// slight problem here with acursed file separator chars being system dependant
			//
			// try to convert both the path from the URL and the possible prefix into the same form
			//
			local_file_path = new File( local_file_path ).getPath();
			prefix_to_remove = new File( prefix_to_remove ).getPath();

			
			System.out.println( "  private path (post normalisation)=" + local_file_path );
			System.out.println( "  prefix path (post normalisation)=" + prefix_to_remove );


			if( prefix_to_remove != null )
			{
			    System.out.println( "  the possible prefix for removal is '" + prefix_to_remove + "'..." );
			    
			    if( local_file_path.startsWith( prefix_to_remove ) )
			    {
				System.out.println( "  which does exist..." );
				
				try
				{
				    String revised_path = mview.getTopDirectory() + File.separatorChar + local_file_path.substring( prefix_to_remove.length() );


				    // replace any "#ref" that might have been part of the URL
				    //
				    String local_file_path_ref = topic_url.getRef();
				    if(( local_file_path_ref != null ) && ( local_file_path_ref.length() > 0 ) )
					revised_path += ( "#" + local_file_path_ref ) ;
				    

				    System.out.println( "  this path has been adapted to be:"  + revised_path );

				    URL revised_url = ( new File( revised_path ) ).toURL();

				    System.out.println( "  trying again..." );

				    displayURL( revised_url, main_editor_pane, y_pos, target, sel_words );
				    
				    waitForDocumentToLoad();
				    
				    if(add_to_history)
				    {
					saveURLInHistory( topic_url );  // or should this be 'revised_url' ?
				    }
				}
				catch( java.io.FileNotFoundException fnfe_again )
				{
				    // right, it really couldn't be found then...
				    
				    setText("<FONT COLOR=\"#ff4444\">File not found.</FONT>\n<FONT COLOR=\"#4444ff\">(" + 
					    topic_url.toString() + ")</FONT>\n"); 
				}
			    }
			}
		    }

	
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

		/*
		if(sel_words != null)
		    selectText( sel_words );
		*/
		
	    } 
	    catch (Exception e) 
	    {
		setText("gotoURL(): Couldn't open help URL: " + topic_url.toString() + "\n  " + e);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    }
	}
    }

   public class CustomEditorPane extends JEditorPane
    {
	public void paintComponent(Graphics g)
	{
	    ((Graphics2D)g).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
					      RenderingHints.VALUE_ANTIALIAS_ON );

	    super.paintComponent( g );
	}
	
	public void scrollTo( String ref )
	{
	    // System.out.println("scrolling to '" + ref + "'");

	    scrollToReference( ref ); 
	}
    }

    public class CustomLinkListener implements HyperlinkListener 
    {
	
	public void hyperlinkUpdate(HyperlinkEvent e) 
	{
	    //System.out.println( "hyperlink me up to " + e.getURL() );

	    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) 
	    {
		JEditorPane pane = (JEditorPane) e.getSource();
		if (e instanceof HTMLFrameHyperlinkEvent) 
		{
		    HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
		    HTMLDocument doc = (HTMLDocument)pane.getDocument();
		    doc.processHTMLFrameHyperlinkEvent(evt);

		    //System.out.println("framed hyperlink me up to " + e.getURL() + " (HTMLFrameHyperlinkEvent)");

		    saveURLInHistory( e.getURL() );

		    index_tree.clearSelection();
		} 
		else 
		{
		    try 
		    {
			URL url = fixURL( e.getURL() );
			
			//System.out.println("hyperlink me up to " + url);

			if(url != null)
			    gotoURL(url,0,true,null,null);
			else
			    mview.alertMessage("Empty link");
			
			index_tree.clearSelection();
			// pane.setPage(e.getURL());
		    }
		    catch (Throwable t) 
		    {
			t.printStackTrace();
			
		    }
		}
	    }
	}
    }

    public void setText(String str)
    {
	main_editor_pane.setContentType("text/html");

	main_editor_pane.setText("<HTML><BODY>" + str + "</BODY></HTML>");

	//main_editor_pane.setContentType("text/html");

	main_editor_pane.scrollRectToVisible(new Rectangle(20,20));
    }

    private void displayJavaCode( URL src )
    {
	try
	{
	    BufferedReader in = new BufferedReader(new InputStreamReader(src.openStream()));
	    
	    String input_line;
	    
	    StringBuffer sbuf = new StringBuffer();
	    sbuf.append("<HTML><HEAD><TITLE>");
	    sbuf.append( src.toString() );
	    sbuf.append("</TITLE></HEAD><BODY><PRE>");
	    while ((input_line = in.readLine()) != null)
	    {
		sbuf.append(input_line + "\n");
	    }
	    in.close();
	    sbuf.append("</PRE></BODY></HTML>");
	    main_editor_pane.setText( sbuf.toString() );
	    main_editor_pane.scrollRectToVisible(new Rectangle(20,20));
	}
	catch(java.net.MalformedURLException murle)
	{
	}
	catch(java.io.IOException ioe)
	{
	}
    }

    private void displayURL(URL url, JEditorPane main_editor_pane, int y_pos, String[] target, int[] sel_words) throws java.io.FileNotFoundException
    {
        try 
	{
	    if(url.toString().endsWith(".java"))
	    {
		displayJavaCode( url );
	    }
	    else
	    {
		main_editor_pane.setPage( url );
	    }
	    
	    // System.out.println("display page @ 0," + y_pos);

	    if(sel_words != null)
	    {
		// selectText( target, sel_words );
		highlightText( target, sel_words );
	    }
	    else
	    {
		Highlighter hl = main_editor_pane.getHighlighter();
		hl.removeAllHighlights();
	    }

	    // changeFont( 0 );

	    // scroll to the top of the page, unless the URL contains
	    // a #label part meaning that it is a sub link...

	    //main_editor_pane.scrollRectToVisible(new Rectangle(20,20));
	    
	    //System.out.println("displayURL(): showing " + url.toString());
        } 
	catch(java.io.IOException ioe)
	{
	    if( ioe instanceof java.io.FileNotFoundException )
		throw (java.io.FileNotFoundException) ioe;
	    
	    System.out.println( "Unable to load the URL " + url + "\n" + ioe);

	    //System.out.println( "possible missing file '" + url.getFile() + "' ?" );
	}
	catch(java.lang.NullPointerException npe)
	{
	    System.out.println( "NullPointerException absorbed" );
	}
	catch(java.util.EmptyStackException ese)
	{
	    System.out.println( "EmptyStackException absorbed" );
	}
	//
        //catch (Exception e) 
	//{
	//setText("displayURL(): unhandled exception whilst loading\nfrom URL " + url.toString() + "\n  " + e);
	// }
	
	
    }

    // ======================================================================================
    // ======================================================================================

    private int[] highlightText( String[] target, int[] word_counts )
    {
	Vector results = new Vector();

	waitForDocumentToLoad();
	
        //main_editor_pane.setEditable(true);
	final Document doc = main_editor_pane.getDocument();

	try
	{
	    final String text    = doc.getText( 0, doc.getLength() );
	    final String text_lc = text.toLowerCase();
	    
	    Highlighter hl = main_editor_pane.getHighlighter();
	    hl.removeAllHighlights();
	    
	    DefaultHighlighter.DefaultHighlightPainter hlp = new DefaultHighlighter.DefaultHighlightPainter(Color.pink);
	    
	    for(int t=0; t < target.length; t++)
		highlightText( results, hl, hlp, text_lc, target[t].toLowerCase(), word_counts[t] );
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	}

	int[] results_a = new int[results.size()];
	for(int r=0; r< results.size(); r++)
	    results_a[r] = ((Integer)results.elementAt(r)).intValue(); 

	return results_a;
    }  

    // dont call this directly, use the wrapper above
    private void highlightText( Vector results,
				Highlighter hl, DefaultHighlighter.DefaultHighlightPainter hlp, 
				String text_lc, String target, int count )
    {
	try
	{
	    final int tl = target.length();

	    int pos = 0;
	    boolean done = false;
	    int cnt = 0;

	    while(!done)
	    {
		pos = text_lc.indexOf( target, pos+1 );
		if(pos >= 0)
		{
		    // System.out.println("highlightText() [2] : instance of " + target + " at " + pos);
		    
		    // if(cnt == count)   // (only highlight the instance of the word that matched)

		    hl.addHighlight( pos, pos+tl , hlp );

		    cnt++;

		    results.addElement(new Integer(pos));
		}
		else
		{
		    done = true;
		}
	    }
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	}
    }

    /*
    private void OLD_highlightText( String target )
    {
	try
	{
	    Document doc = main_editor_pane.getDocument();
	    
	    final String text    = doc.getText( 0, doc.getLength() );
	    String text_lc = text.toLowerCase();
	    
	    final int tl = target.length();

	    SimpleAttributeSet text_style  = new SimpleAttributeSet();
	    StyleConstants.setFontFamily(text_style, "Courier");
	    StyleConstants.setFontSize(text_style, 12);
	    StyleConstants.setForeground(text_style, new Color(255-160,0,255-160));

	    int pos = 0;
	    boolean done = false;
	    while(!done)
	    {
		pos = text_lc.indexOf( target, pos+1 );
		if(pos >= 0)
		{
		    System.out.println("highlightText(): instance of " + target + " at " + pos);
		    
		    //String new_str = "<FONT COLOR=\"RED\">" + text.substring(pos, (pos+tl+1)) + "<\\FONT>";
		    // String new_str = text.substring(pos, (pos+tl+1)).toUpperCase();
		    String new_str = "[[[" + text.substring(pos, (pos+tl)) + "]]]";

		    text_lc = text_lc.substring(0, pos-1) + text.substring(pos, (pos+tl)) + text_lc.substring(pos);
		    
		    main_editor_pane.setCaretPosition( pos );
		    main_editor_pane.moveCaretPosition( pos+tl );
		    main_editor_pane.replaceSelection( new_str );

		    //pos += 25;
		    pos += 6;

		}
		else
		{
		    done = true;
		}
	    }
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	}
    }
    */



    // ======================================================================================
    // ======================================================================================

    /*
    private void selectText( String[] text )
    {
	selectText( text[0] );
    }
    */
    private int[] findTargetText( String[] target, String text, int pos )
    {
	int[] result = new int[2];
	result[0] = -1;

	for(int t = 0; t < target.length; t++)
	{
	    int td = text.indexOf( target[t], pos );
	    if(td >= 0)
	    {
		if(result[0] == -1)
		{
		    result[0] = td;
		    result[1] = t;
		}
		else
		{
		    if(td < result[0])
		    {
			result[0] = td;
			result[1] = t;
		    }
		}
	    }
	}
	return result;
    }

    private void selectText( String[] target, int[] sel_words )
    {
	highlightText( target, sel_words  );

	/*
	try
	{
	    // need to wait until the document is fully loaded....

	    // (guess when this is by watching the doc length and
	    // waiting until it doesn't change for a bit)

	    waitForDocumentToLoad();
	    
	    Document doc = main_editor_pane.getDocument();
	    
	    System.out.println("selectText() : doc len=" + doc.getLength());

	    System.out.println("selectText() : from " + target[0] + " to " + target[ sel_words.length - 1 ]);

	    String text_lc = doc.getText( 0, doc.getLength() ).toLowerCase();

	    // find the character position of the first word
	    
	    int start_p = findCharIndexOfWord( text_lc, sel_words[0] );
	    int end_p   = (sel_words.length == 1) ? start_p : findCharIndexOfWord( text_lc, sel_words[ sel_words.length - 1] );
	    
	    // got a result!
	    if((start_p >= 0) && (end_p >= 0))
	    {
		main_editor_pane.setCaretPosition( start_p );
		main_editor_pane.moveCaretPosition( end_p );
		main_editor_pane.grabFocus();
		System.out.println("selectText() : highlighting!");
	    }
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	}
	*/

    }

    private void waitForDocumentToLoad()
    {
	Document doc = main_editor_pane.getDocument();
	
	boolean has_changed = true;
	int last_len = doc.getLength();
	
	while( has_changed )
	{
	    try
	    {
		Thread.sleep(500);
	    }
	    catch(InterruptedException ie)
	    {
	    }
	    has_changed = (doc.getLength() != last_len);
	    last_len = doc.getLength();
	}
    }

    // find the character position of the Nth word
    private int findCharIndexOfWord( String str, int word_t )
    {
	final int string_l = str.length();
	Vector result = new Vector();
	
	int word_c = 0;
	boolean in_word = false;
	int p = 0;
	int last_start = 0;
	String word_s = null;

	while((p < string_l) && (word_c < word_t))
	{
	    char c = str.charAt(p);
	    if(!Character.isLetterOrDigit(c))
	    {
		if(in_word)
		{
		    word_s = ( str.substring( last_start, p ).trim() );
		    if(++word_c >= word_t)
			break;
		    in_word = false;
		}
	    }
	    else
	    {
		if(!in_word)
		{
		    in_word = true;
		    last_start = p;
		}
	    }
	    p++;
	}
	if((word_c < word_t) && (in_word))
	{
	    if(last_start < p)
	    {
		String wtmp = str.substring( last_start, p ).trim();
		if(wtmp.length() > 0)
		{
		    word_s = wtmp;
		    word_c++;
		}

	    }
	}

	
	if(word_c == word_t)
	{
	    // System.out.println("findCharIndexOfWord(): word index " + word_t + " is at char " + last_start);

	    return last_start;
	}
	else 
	{
	    // System.out.println("findCharIndexOfWord(): word index " + word_t + " not found");

	    return -1;
	}
    }

    //
    // NOT USED!
    //
    private void selectText( String[] target )
    {
	try
	{
	    // need to wait until the document is fully loaded....

	    // (guess when this is by watching the doc length and
	    // waiting until it doesn't change for a bit)

	    Document doc = main_editor_pane.getDocument();
	    
	    boolean has_changed = true;
	    int last_len = doc.getLength();

	    while( has_changed )
	    {
		try
		{
		    Thread.sleep(500);
		}
		catch(InterruptedException ie)
		{
		}
		has_changed = (doc.getLength() != last_len);
		last_len = doc.getLength();
	    }
	    
	    // System.out.println("selectText() : doc len=" + doc.getLength());

	    String text_lc = doc.getText( 0, doc.getLength() ).toLowerCase();
	    
	    String[] target_lc = new String[ target.length ];
	    for(int t=0; t < target.length; t++)
		target_lc[t] = target[t].toLowerCase();

	    int cpos = main_editor_pane.getCaretPosition();
	    if(cpos < 0)
		cpos = 0;

	    int[] res = findTargetText( target_lc, text_lc, cpos);
	    
	    if((res[0] < 0) && (cpos > 0))
	    {
		// fallen off the end? trying restarting 
		res = findTargetText( target_lc, text_lc, 0);
	    }

	    // System.out.println("selectText() : index=" + res[0]);

	    // got a result!
	    if(res[0] >= 0)
	    {
		main_editor_pane.setCaretPosition( res[0] );
		main_editor_pane.moveCaretPosition( res[0] + target[ res[1] ].length() );
		main_editor_pane.grabFocus();
		// System.out.println("selectText() : highlighting!");
	    }
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	}


    }
    
    // ======================================================================================
    // ======================================================================================

    private class VisitedPage
    {
	URL url;
	String title;
	int y_pos;
    }

    private Vector page_history_v = new Vector();
    private boolean history_grabbing_disabled = false;

    private void saveURLInHistory(URL url)
    {
	JViewport vp = main_editor_scroll_pane.getViewport();
	Point pt = vp.getViewPosition();
	
	savePageInHistory( null, url, pt.y );
    }
    
    private void goBackward()
    {
	int pos = history_jcb.getSelectedIndex();
	if((pos >= 0) && ((pos+1) < history_jcb.getItemCount()))
	    gotoPageFromHistory( pos + 1 );
    }
    private void goForward()
    {
	int pos = history_jcb.getSelectedIndex();
	if(pos > 0)
	    gotoPageFromHistory( pos - 1 );
    }
    
    public void savePageInHistory( String title, URL url, int y_pos )
    {
	//System.out.println("saving " + url.toString() + " (y=" + y_pos + ") in history");

	VisitedPage vp = new VisitedPage();
        vp.title = (title == null) ? extractTitleFromPane() : title;
	if(vp.title == null)
	    vp.title = extractTitleFromURL( url );
	vp.url = url;
	vp.y_pos = y_pos;
	
	// is the page already in the history?
	int id = findPageInHistory(vp);
	if(id == -1)
	{
	    // page not in history, save it at head of list

	    history_grabbing_disabled = true;
	    
	    history_jcb.insertItemAt( vp.title, 0 );
	    history_jcb.setSelectedIndex(0);
	    
	    page_history_v.insertElementAt(vp, 0);
	    
	    history_grabbing_disabled = false;
	    
	    back_button.setEnabled( true );
	    fore_button.setEnabled( false );
	}
	else
	{
	    // page in history, update jcb

	    //System.out.println("   page already in history, not saved");

	    history_grabbing_disabled = true;
	    history_jcb.setSelectedIndex(id);

	    back_button.setEnabled( id < page_history_v.size() );
	    fore_button.setEnabled( id > 0 );
	    history_grabbing_disabled = false;

	}
    }

    private int findPageInHistory( VisitedPage vp )
    {
	for(int f=0; f < page_history_v.size(); f++)
	{
	    VisitedPage vp_f = (VisitedPage) page_history_v.elementAt( f );

	    boolean match = true;

	    if(vp.title != null)
	    {
		if(vp_f.title != null)
		{
		    if(!vp.title.equals(vp_f.title))
			match = false;
		}
		else
		   match = false;
	    }
	    else
	    {
		if(vp_f.title != null)
		    match = false;
	    }
	    
	    if(!vp.url.equals(vp_f.url))
		match = false;

	    if(match)
		return f;
	}
	return -1;
    }

    public void gotoPageFromHistory( int steps_back )
    {
	// System.out.println("going to page at " + steps_back + " steps back");

	history_grabbing_disabled = true;
	
	VisitedPage vp = (VisitedPage) page_history_v.elementAt( steps_back );
	
	//System.out.println("going to page " + vp.url.toString() + " at " + 
	//		   steps_back + " steps back (y=" + vp.y_pos + ")");
	
	gotoURL( vp.url, vp.y_pos, false, null, null );

	history_jcb.setSelectedIndex( steps_back );

	history_grabbing_disabled = false;

	fore_button.setEnabled( steps_back > 0 );
	back_button.setEnabled( steps_back < page_history_v.size() );
    }

    final int max_history_entries = 10;

    public void saveHistoryDataInProps()
    {
	int max = max_history_entries;
	if(max > page_history_v.size())
	    max = page_history_v.size();

	mview.putIntProperty( "HelpPanel.n_history_entries", max );

	for(int i=0; i < max; i++)
	{
	    VisitedPage vp = (VisitedPage) page_history_v.elementAt( i );
	    mview.putProperty( ("HelpPanel.history" + i +" .url"), vp.url.toString() );
	}
    }
    
    public void loadHistoryDataFromProps()
    {
	int nh = mview.getIntProperty( "HelpPanel.n_history_entries", 0 );

	history_grabbing_disabled = true;

	for(int i=0; i < nh; i++)
	{
	    String val =  mview.getProperty( "HelpPanel.history" + i +" .url", null);
	    
	    if(val != null)
	    {
		VisitedPage vp = new VisitedPage();
		try
		{
		    vp.url = new URL(val);

		    vp.title = null;
		    vp.y_pos = 0;
		    
		    page_history_v.addElement( vp );
		    
		    history_jcb.addItem( extractTitleFromURL( vp.url ) );
		    history_jcb.setSelectedIndex(0);
		}
		catch(java.net.MalformedURLException mure)
		{
		    System.out.println("HelpPanel.loadHistoryDataFromProps(): odd, MalformedURLException");
		}
	    }
	}
	
	history_grabbing_disabled = false;
	
    }
    
    
    // ======================================================================================

    private String extractTitleFromURL(URL url)
    {
	if(url == null)
	    return "?";
	String url_str = url.toString();
	if(url_str.startsWith("file:"))
	    url_str = url_str.substring(5);
	if(url_str.startsWith(mview.getTopDirectory()))
	    url_str = url_str.substring( mview.getTopDirectory().length() );
	return url_str;
	
    }

    private String extractTitleFromPane()
    {
	
	Document doc = main_editor_pane.getDocument();
	return (String) doc.getProperty( doc.TitleProperty );

	//return (doc == null) ? null : doc.TitleProperty;  // doesnt work (always returns "title")!

	

    }

    // ======================================================================================
    // ======================================================================================

    class IndexTreeNode extends DefaultMutableTreeNode
    {
	public IndexTreeNode( String title_, String url_ )
	{
	    title = title_;
	    url = url_;
	}
	public String toString() { return title; }

	public String title;
	public String url;	
    }

    public void populateIndexTree(JTree tree)
    {

	final String shared_help_path = mview.getHelpDirectory();
	final String users_help_path  = mview.getUserSpecificDirectory() + File.separatorChar + "docs" + File.separatorChar;

	final String shared_help_url = "file:" + mview.getHelpDirectory();
	final String users_help_url  = "file:" + mview.getUserSpecificDirectory() + File.separatorChar + "docs" + File.separatorChar;



	// System.out.println( "populateIndexTree() ... start" );

	IndexTreeNode root = new IndexTreeNode("Top", "Top.html");
	IndexTreeNode node = null;
	IndexTreeNode subnode = null;
	IndexTreeNode subsubnode = null;
	DefaultTreeModel model =  new DefaultTreeModel( root );
	
	node = new IndexTreeNode("Overview", "Overview.html");
	root.add(node);

	// ======================

	node = new IndexTreeNode("Concepts", "Concepts.html");
      	root.add(node);
	
	subnode = new IndexTreeNode("Spots and Names", "Concepts.html#spot");
	node.add(subnode);

	subnode = new IndexTreeNode("Measurements", "Concepts.html#meas");
	node.add(subnode);

	subnode = new IndexTreeNode("Special numerical values", "Concepts.html#nans");
	node.add(subnode);

	subnode = new IndexTreeNode("Filters", "Concepts.html#filt");
	node.add(subnode);

	subnode = new IndexTreeNode("Clusters", "Concepts.html#clust");
	node.add(subnode);

	subnode = new IndexTreeNode("Annotation", "Concepts.html#anno");
	node.add(subnode);

	subnode = new IndexTreeNode("Events", "Concepts.html#event");
	node.add(subnode);

	subnode = new IndexTreeNode("Plugins", "Concepts.html#plug");
	node.add(subnode);

	subnode = new IndexTreeNode("Colourisers", "Concepts.html#col");
	node.add(subnode);

	subnode = new IndexTreeNode("Data merging", "Concepts.html#merge");
	node.add(subnode);

	subnode = new IndexTreeNode("Custom menu", "Concepts.html#cust");
	node.add(subnode);

	subnode = new IndexTreeNode("Drag and Drop", "Concepts.html#drag");
	node.add(subnode);

	subnode = new IndexTreeNode("Selection", "Concepts.html#sel");
	node.add(subnode);

	// ======================

	node = new IndexTreeNode("Data Model", "DataModel.html");
	root.add(node);

	// ======================

	node = new IndexTreeNode("Tutorials", "Tutorial.html");
	root.add(node);

	subnode = new IndexTreeNode("User level", "Tutorial.html");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Getting started", "Started.html");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Working with Clusters", "ClusterTutorial.html");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Commands and Hotkeys", "CommandTutorial.html");
	subnode.add(subsubnode);

	subnode = new IndexTreeNode("Programmer level", "Tutorial.html");
	node.add(subnode);

	subsubnode = new IndexTreeNode("The Cluster APIs", "ClusterAPITutorial.html");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Writing a Plugin", "PluginTutorial.html" );
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Working with Plugin Commands", "RunningCommands.html" );
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("The RMI Interface", "RMIInterface.html" );
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Wrapping maxdView with another application", "WrappingTutorial.html");
	subnode.add(subsubnode);


	// ======================

	node = new IndexTreeNode("Menu Commands", "file:" + users_help_path + "Commands.html");
	root.add(node);

	subnode = new IndexTreeNode("Popup Menu", "Popup.html");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Name or Name Attribute", "Popup.html#name");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Measurement Name", "Popup.html#meas");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Cluster", "Popup.html#clust");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Spot", "Popup.html#spot");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Custom Menu", "CustomMenu.html");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Selection Menu", "Popup.html#select");
	subnode.add(subsubnode);

	// -------------------

	subnode = new IndexTreeNode("System Menu", "file:" + users_help_path + "Commands.html#System");
	node.add(subnode);
	
	subsubnode = new IndexTreeNode("Plugin Manager", "PluginManager.html");
	subnode.add(subsubnode);

	subnode = new IndexTreeNode("Display Menu", "Commands.html#Display");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Find", "ViewerFind.html");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Layout", "ViewerLayout.html");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Colours", "ViewerColours.html");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("New view", "NewView.html");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Print", "ViewerPrint.html");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Apply filter", "ApplyFilter.html");
	subnode.add(subsubnode);



	PluginManager pman = mview.getPluginManager();

	DefaultMutableTreeNode menus = pman.getMenuRoot();

	for(int m=0; m < menus.getChildCount(); m++)
	{
	    DefaultMutableTreeNode menu_node = (DefaultMutableTreeNode) menus.getChildAt(m);
	    String menu_name = (String) menu_node.getUserObject();
	    
	    subnode = new IndexTreeNode(menu_name + " Menu", "file:" + users_help_path + "Commands.html#" + menu_name);
	    node.add(subnode);
	
	    for(int c=0; c < menu_node.getChildCount(); c++)
	    {
		// generate the name of the documentation file

		DefaultMutableTreeNode child_node = (DefaultMutableTreeNode) menu_node.getChildAt( c );
	    
		String pname = (String) child_node.getUserObject();

		PluginInfo pinf = mview.getPluginInfoFromName( pname );

		if( pinf != null )
		{
		    //String fname = mview.getPluginFullName( pname );
		    
		    //String doc_file = top_dir + File.separator + fname + ".html";
		    
		    //String doc_file = "file:" + ".." + File.separator + fname;
		    String doc_file = "file:" + pinf.root_path + File.separator + pinf.class_name + ".html";
		    
		    //if(doc_file.toLowerCase().endsWith(".class"))
		    //    doc_file = doc_file.substring(0, doc_file.length()-6); // remove the .class suffix
		    
		    //doc_file += ".html";
		    
		    //System.out.println( pinf.name + " -> " + doc_file );
		    
		    IndexTreeNode pnode = new IndexTreeNode( pinf.name, doc_file );
		    
		    subnode.add( pnode );
		}

	    }
	}


	// ======================

	node = new IndexTreeNode("File Formats", "FileFormats.html");
	root.add(node);

	// ======================

	node = new IndexTreeNode("Plugin Commands", "file:" + users_help_path + "PluginCommandsList.html");
	root.add(node);


	node = new IndexTreeNode("Method Reference", "MethodRef.html");
	root.add(node);

	subnode = new IndexTreeNode("ExprData", "MethodRef.html#exprdata");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Data Access", "MethodRef.html#exprdata_access");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Spots and Indexing", "MethodRef.html#exprdata_spots");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Sorting", "MethodRef.html#exprdata_sort");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Selection", "MethodRef.html#exprdata_sel");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Names Attributes", "MethodRef.html#exprdata_tags");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Measurements", "MethodRef.html#exprdata_meas");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Clusters", "MethodRef.html#exprdata_clust");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Filter (general)", "MethodRef.html#exprdata_filt");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Filter (interface)", "MethodRef.html#exprdata_filtint");
	subnode.add(subsubnode);	
	subsubnode = new IndexTreeNode("Receiving and generating Events", "MethodRef.html#exprdata_events");
	subnode.add(subsubnode);

	// ---------------------

	subnode = new IndexTreeNode("Measurement", "MethodRef.html#measurement");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Constructors", "MethodRef.html#meas_constr");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Accessors", "MethodRef.html#meas_access");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Attributes", "MethodRef.html#meas_attrib");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("Spot Attributes", "MethodRef.html#meas_sa");
	subnode.add(subsubnode);

	// ---------------------

	subnode = new IndexTreeNode("Cluster", "MethodRef.html#cluster");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Constructors", "MethodRef.html#clust_constr");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Cluster Methods", "MethodRef.html#clust_methods");
	subnode.add(subsubnode);
	subsubnode = new IndexTreeNode("Cluster Iterator Methods", "MethodRef.html#clust_iter_methods");
	subnode.add(subsubnode);

	// ---------------------

	subnode = new IndexTreeNode("maxdView", "MethodRef.html#maxdView");
	node.add(subnode);

	subsubnode = new IndexTreeNode("Displaying Information", "MethodRef.html#mview_output");
	subnode.add(subsubnode);

	subsubnode = new IndexTreeNode("User input", "MethodRef.html#mview_input");
	subnode.add(subsubnode);
	
	subsubnode = new IndexTreeNode("Application Properties", "MethodRef.html#mview_appprops");
	subnode.add(subsubnode);

	// ---------------------

	subnode = new IndexTreeNode("DataTags", "MethodRef.html#datatags");
	node.add(subnode);

	subnode = new IndexTreeNode("TagAttrs", "MethodRef.html#tagattrs");
	node.add(subnode);

	subnode = new IndexTreeNode("DataPlot", "MethodRef.html#dataplot");
	node.add(subnode);

	subnode = new IndexTreeNode("AnnotationLoader", "MethodRef.html#annlo");
	node.add(subnode);

	subnode = new IndexTreeNode("Event", "MethodRef.html#event");
	node.add(subnode);

	subnode = new IndexTreeNode("Plugin", "MethodRef.html#plugin");
	node.add(subnode);

	subnode = new IndexTreeNode("Filter", "MethodRef.html#filter");
	node.add(subnode);

	// ======================

	node = new IndexTreeNode("Programmer's Guide", "ProgGuide.html");
	root.add(node);

	subnode = new IndexTreeNode("Using the ExprData and maxdView APIs", "ProgGuide.html#apis");
	node.add(subnode);

	subnode = new IndexTreeNode("Writing Plugins", "ProgGuide.html#plugins");
	node.add(subnode);

	subnode = new IndexTreeNode("Inter-Plugin Communication", "ProgGuide.html#ipc");
	node.add(subnode);

	subnode = new IndexTreeNode("The Event Model", "ProgGuide.html#events");
	node.add(subnode);

	// ======================

	node = new IndexTreeNode("Glossary", "Glossary.html");
	root.add(node);

	node = new IndexTreeNode("About", "file:" + users_help_path + "About.html");
	root.add(node);

	node = new IndexTreeNode("News", "WhatsNew.html");
	root.add(node);

	tree.setModel(model);
	tree.putClientProperty("JTree.lineStyle", "Angled");

    }

    // ======================================================================================
    // ======================================================================================

    class HelpIndex
    {
	Vector index_data;
	
	String[] filenames;
	String[] docnames;
    }

    private HelpIndex help_index = null;

    // returns a Vector full of String's, one for each line in the file
    //
    private HelpIndex loadIndex(String fname)
    {
 	HelpIndex hi = new HelpIndex();

	File file = new File(fname); //fc.getSelectedFile();
	
	try
	{
	    FileInputStream fis = new FileInputStream(file);
	    GZIPInputStream gis = new GZIPInputStream(fis);
	    InputStreamReader isr = new InputStreamReader(gis);
	    BufferedReader br = new BufferedReader(isr);

	    // how may file names?
	    int n_filenames = 0;
	    String str = br.readLine();
	    try
	    {
		n_filenames = (new Integer(str)).intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
	    }
	    
	    //System.out.println("there are " + n_filenames + " filenames");
	    
	    // read the file names
	    hi.filenames = new String[n_filenames];
	    for(int nf=0; nf < n_filenames; nf++)
	    {
		str = br.readLine();
		hi.filenames[nf] = str;
	    }
	    
	    hi.docnames = new String[n_filenames];
	    for(int nf=0; nf < n_filenames; nf++)
	    {
		str = br.readLine();
		hi.docnames[nf] = str;
	    }
	    
	    // now read the index data lines
	    hi.index_data = new Vector();
	    
	    str = br.readLine();
	    while(str != null)
	    {
		String tstr = str.trim();
		
		if(tstr.length() > 0)
		    hi.index_data.addElement(tstr);
		
		str = br.readLine();
	    }

	    // System.out.println("there are " + hi.index_data.size() + " index lines");
	}
	catch(java.io.IOException ioe)
	{
	    mview.alertMessage("Unable to read name.\nError: " + ioe);
	}
	return hi;
    }

    // ======================================================================================

    private void displayFindDialog()
    {
	
	final JFrame frame = new JFrame("Help: Find");
	final JPanel panel = new JPanel();
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    closeMatchDisplay();
		}
	    });

	panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	frame.getContentPane().add(panel, BorderLayout.CENTER);

	//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

	JPanel control_panel = new JPanel();
	control_panel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	GridBagLayout control_gridbag = new GridBagLayout();
	control_panel.setLayout(control_gridbag);

	JLabel label = new JLabel("Find ");
	c = new GridBagConstraints();
	control_gridbag.setConstraints(label, c);
	control_panel.add(label);

	final JTextField jtf = new JTextField(32);
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridwidth = 2;
	control_gridbag.setConstraints(jtf, c);
	control_panel.add(jtf);

	JButton jb = new JButton("in this document");
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    doFindInThis( jtf.getText() );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	control_gridbag.setConstraints(jb, c);
	control_panel.add(jb);

	jb = new JButton("in all documents");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    doFindInAll( jtf.getText() );
		}
	    });
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 1.0;
	c.gridx = 2;
	c.gridy = 1;
	control_gridbag.setConstraints(jb, c);
	control_panel.add(jb);

	panel.add(control_panel);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	gridbag.setConstraints(control_panel, c);


	//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
	
	details_table = new JTable();
	details_table.setColumnSelectionAllowed(false);

        details_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	details_table.addMouseListener(new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    // System.out.println("click!");

		    final int id = details_table.getSelectedRow();
		    
		    if(current_table_data != null)
		    {
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if(current_match_mode == 2)
			    selectMatch( current_find_target, ((Object[][]) current_table_data)[ id ] );
			if(current_match_mode == 1)
			    selectMatch( ((int[]) current_table_data)[ id ] );
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		    }
		}
	    });
	//details_table.setPreferredSize(new Dimension( 300, 250 ));
	JScrollPane jsp = new JScrollPane(details_table);
	jsp.setPreferredSize(new Dimension( 300, 250 ));

	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.gridy = 1;
	c.weightx = 2.0;
	// c.weighty = 4.0;
	gridbag.setConstraints(jsp, c);
	
	panel.add(jsp);

	//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

	JPanel buttons_panel = new JPanel();
	buttons_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	GridBagLayout inner_gridbag = new GridBagLayout();
	buttons_panel.setLayout(inner_gridbag);
	
	/*  // help on help?
	{   
	    final JButton jb = new JButton("Help");
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getHelpTopic("HelpPanel", "#attributes");
		    }
		});
		
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    gridbag.setConstraints(jb, c);
	}
	*/

	jb = new JButton("Close");
	buttons_panel.add(jb);
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    closeMatchDisplay();
		    frame.setVisible(false);
		}
	    });
	
	c = new GridBagConstraints();
	c.gridx = 0;
	gridbag.setConstraints(jb, c);
	
	panel.add(buttons_panel);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	gridbag.setConstraints(buttons_panel, c);

	//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

	frame.pack();
	mview.locateWindowAtCenter(frame);
	frame.setVisible(true);

    }
  
    private String[] current_find_target = null; // used in doFind() and displayMatches()
    private Object current_table_data = null;
    private int current_match_mode = 0;

    // ======================================================================================

    private void doFindInThis( String srch )
    {
	String[] srch_a = tokenise(srch);
	int[] dummy = new int[srch_a.length];
	int[] matches = highlightText( srch_a, dummy );
	displayMatches( matches );
    }

    // ======================================================================================

    private void doFindInAll( String srch )
    {
	String[] srch_a = tokenise(srch);
	
	// System.out.println("doFind: " + srch_a.length + " words");
	
	if(help_index == null)
	    help_index = loadIndex( mview.getHelpDirectory() + "index.dat.gz" );
	
	if((help_index == null) || (help_index.index_data == null) || (help_index.index_data.size() == 0))
	{
	    mview.alertMessage("Unable to load index");
	    help_index = null;
	    return;
	}

	Vector matches_a[] = new Vector[ srch_a.length ];
	
	for(int a=0; a < srch_a.length; a++)
	{
	    Vector matches = new Vector();
	    
	    for(int m=0; m < help_index.index_data.size(); m++)
	    {
		if( ((String)help_index.index_data.elementAt(m)).startsWith( srch_a[a] ) )
		{
		    matches.addElement( help_index.index_data.elementAt(m) );
		}
	    }
	    
	    // System.out.println("doFind: word " + a + "=" + srch_a[a] +" matches=" + matches.size() );
	    
	    /*
	      if( matches.size() > 0)
	      {
	      displayMatches( scoreMatches( help_index, matches ) );
	      }
	      else
	      {
	      mview.alertMessage( "No matches" );
	      }
	    */
	    matches_a[a] = matches ;
	    
	    // mview.infoMessage(index_data.size() + " lines in index, " + 
	    //	      matches.size() + " matches with '" + srch + "'");
	}
	
	displayMatches( srch_a, scoreMatches( help_index, matches_a ) );
	    
    }

    private String[] tokenise( String str )
    {
	final int string_l = str.length();
	Vector result = new Vector();
	
	boolean in_word = false;
	int p = 0;
	int last_start = 0;

	while(p < string_l)
	{
	    char c = str.charAt(p);
	    if(Character.isWhitespace(c))
	    {
		if(in_word)
		{
		    result.addElement ( str.substring( last_start, p ).trim().toLowerCase() );
		    in_word = false;
		}
	    }
	    else
	    {
		if(!in_word)
		{
		    in_word = true;
		    last_start = p;
		}
	    }
	    p++;
	}
	if(in_word)
	{
	    if(last_start < p)
		result.addElement ( str.substring( last_start, p ).trim().toLowerCase() );
	}

	return (String[]) result.toArray(new String[0]);
    }

    // ======================================================================================

    private void selectMatch( int char_pos )
    {
	main_editor_pane.setCaretPosition( char_pos );
    }

    private void selectMatch( String[] target, Object[] sel_match )
    {
	{
	    // match in all files

	    int[] count_a = (int[]) sel_match[4];
	    
	    // note: filenames in the index file are relative to the
	    //       top-level directory, not the docs directory


	    /*
	      if(count_a != null)
	      {
	      System.out.print("selectMatch(): best match word counts are ");
	      for(int s=0; s < count_a.length; s++)
	      System.out.print(" "+ count_a[s]);
	      System.out.println();
	      }
	    */
	    gotoTopic( "file:" + mview.getTopDirectory() + File.separatorChar + sel_match[1],  target, count_a );
	}
	
    }

  
    // --------------------------------------------------------------------------------------

    private Object[][]  scoreMatches( HelpIndex hi, Vector matches_a[] )
    {
	return scoreMatches_2(hi, matches_a);
    }

    // generates score based on how many of the search words occur in each document
    // (multiple words)

    /*
    private Object[][]  scoreMatches_1( HelpIndex hi, Vector matches_a[] )
    {
	final int n_files = hi.filenames.length;
	int[] master_file_count = new int[ n_files ];
	int[] master_match_count = new int[ n_files ];

	for(int w=0; w < matches_a.length; w++)         // for each search word
	{
	    int[] local_file_count = new int[ n_files ];
	    
	    for(int d=0; d < matches_a[w].size(); d++)  // for each matching word in each file
	    {
		String line = (String) matches_a[w].elementAt(d);
		
		// System.out.println("scoring: " + line);
		
		int first_spc = line.indexOf(' ');
		String word = line.substring(0, first_spc);
		
		first_spc++;
		int second_spc = line.indexOf(' ', first_spc);
		Integer filecode = Integer.valueOf( line.substring(first_spc, second_spc) );
		int filecode_i = filecode.intValue();
		
		second_spc++;
		int third_spc = line.indexOf(' ', second_spc);
		// String count = line.substring(second_spc, third_spc);
		Integer count = Integer.valueOf( line.substring(second_spc, third_spc) );
		int count_i = count.intValue();
		
		// System.out.println("  word=" + word + " fc=" + filecode + " c=" + count_i);

		// increment the file count for this word
		
		local_file_count[ filecode_i ] ++;
		master_match_count[ filecode_i ] +=  count_i;
	    }
	    
	    // update master file counts
	    for(int f=0; f< n_files; f++)
	    {
		if(local_file_count[f] > 0)
		    master_file_count[f]++;
	    }
	}
	
	// now we know how many files each search-word occurs in
	
	// generate a score for each file (i.e. the amount of search-words it contains)

	Vector data_v = new Vector();
	for(int f=0; f< n_files; f++)
	{
	    if(master_file_count[f] > 0)
	    {
		Object[] dataline = new Object[3];
		dataline[0] = hi.filenames[f];
		dataline[1] = new Integer(master_file_count[f]);
		dataline[2] = new Integer(master_match_count[f]);
		
		data_v.addElement( dataline );
	    }
	}
	
	Object[][] result = (Object[][]) data_v.toArray(new Object[0][]);
	java.util.Arrays.sort( result, new MatchComparator() );
	return result;
    }
    */

    // (new version: uses word distances)
    //
    private Object[][]  scoreMatches_2( HelpIndex hi, Vector matches_a[] )
    {
	//
	// for each file:
	//   find the closest distance between all search words
        //


	final int n_files        = hi.filenames.length;
	final int n_search_words = matches_a.length;

	int[]     master_file_count = new int[ n_files ];
	int[][][] master_posn_a = new int[ n_search_words ][][];     // search_words x files x positions
	int[]     total_word_freq   = new int[ n_files ];
	    
	for(int sw=0; sw < n_search_words; sw++)         // for each search word
	{
	    // int[]    local_file_count  = new int[ n_files ];
	    String[] local_file_posns  = new String[ n_files ];
            for(int f=0; f < n_files; f++)
		local_file_posns[f] = "";

	    // for this search word, how many hits in which files?

	    for(int mw=0; mw < matches_a[sw].size(); mw++)  // for each words that matches search word a
	    {
		String line = (String) matches_a[sw].elementAt(mw);
		
		// System.out.println("scoring: " + line);
		
		int first_spc = line.indexOf(' ');
		String word = line.substring(0, first_spc);
		
		first_spc++;
		int second_spc = line.indexOf(' ', first_spc);
		Integer filecode = Integer.valueOf( line.substring(first_spc, second_spc) );
		int filecode_i = filecode.intValue();
		
		second_spc++;
		int third_spc = line.indexOf(' ', second_spc);
		// String count = line.substring(second_spc, third_spc);
		Integer count = Integer.valueOf( line.substring(second_spc, third_spc) );
		int count_i = count.intValue();
		
		// accumulate all positions for this search-word on a per-file basis
		//
		local_file_posns[ filecode_i ] += ( line.substring( third_spc ) );

		total_word_freq[ filecode_i ] += count_i;

		// System.out.println("  word=" + word + " fc=" + filecode + " c=" + count_i);

		// increment the file count for this word
		// local_file_count[ filecode_i ] ++;
	    }
	    
	    // update master file positions
	    int[][] file_posn_a = new int[ n_files ][];
	    for(int f=0; f < n_files; f++)
	    {
		file_posn_a[ f ] = extractPositions( local_file_posns[f] );
	    }
	    master_posn_a[ sw ] = file_posn_a;
	}

	// for each file, find the best sequence of search words (i.e. the one
	// with the shortest distance)
	
	Vector data_v = new Vector();
	for(int f=0; f< n_files; f++)
	{
	   int[] best_seq = findBestSequence( hi, n_files, n_search_words, f, master_posn_a );
	   int score = (best_seq == null) ? -1 : scoreSequence( best_seq );
	   if(score >= 0)
	   {
	       Object[] dataline = new Object[5];
	       dataline[0] = hi.docnames[f];
	       dataline[1] = hi.filenames[f];
	       dataline[2] = new Integer(score);
	       dataline[3] = new Integer( total_word_freq[f] );
	       dataline[4] = convertSequenceToCount( best_seq, n_files, n_search_words, f, master_posn_a);
	       data_v.addElement( dataline );
	   }
	}

	Object[][] result = (Object[][]) data_v.toArray(new Object[0][]);
	java.util.Arrays.sort( result, new MatchComparator() );
	return result;

    }

    private int[] extractPositions( String str )
    {
	String[] str_a = tokenise( str );
	int[] str_i = new int[ str_a.length ];

	for(int i=0; i < str_a.length; i++)
	{
	    try
	    {
		str_i[i] = (Integer.valueOf( str_a[i] )).intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		str_i[i] = -1;
	    }
	}

	//if(str_i.length > 0)
	//    System.out.println("extractPositions(): " + str_i.length + " ints found in '" + str + "'");

	return str_i;
    }

    // 'seq' must be sorted
    //
    private int scoreSequence( int[] seq )
    {
	int[] seq_s = (int[]) seq.clone();

	java.util.Arrays.sort( seq_s );

	int dist = 0;
	for(int sw=1; sw < seq_s.length; sw++)
	{
	    dist += (seq_s[ sw ] - seq_s[ sw-1 ]);
	}
	return dist;
    }

    //  master_posn_a is 3dim array of search_words x files x positions
    //
    private int[] findBestSequence( final HelpIndex hi, 
				    final int n_files, final int n_search_words, 
				    int file_id, int[][][] master_posn_a )
    {
	// explicitly check all permutations....

	int n_perms = master_posn_a[ 0 ][ file_id ].length;

	int[] stride = new int[n_search_words];

	for(int sw=1; sw < n_search_words; sw++)
	{
	    n_perms *= master_posn_a[ sw ][ file_id ].length;
	}
	
	//if(n_perms > 0)
	//    System.out.println("findBestSequence(): file " + file_id + " has " + n_perms + " perms");

	if(n_perms == 0)
	    return null;

	// need stride info for updating positions later
	for(int sw=0; sw < n_search_words; sw++)
	{
	    stride[ sw ] = master_posn_a[ sw ][ file_id ].length;
	}
	
	int best = Integer.MAX_VALUE;
	int[] best_a = null;

	int[] pos = new int[n_search_words];
	int[] seq = new int[n_search_words];

	for(int p=0; p < n_perms; p++)
	{
	    // generate sequence of word positions

	    for(int sw=0; sw < n_search_words; sw++)
	    {
		seq[ sw ] = master_posn_a[ sw ][ file_id ][ pos[sw] ];
	    }

	    // work out distance
	    
	    int dist = scoreSequence( seq );

	    // store score
	    if(dist < best)
	    {
		best_a = (int[]) seq.clone();
		best = dist;
	    }
	    
	    // update positions
	    boolean cascade = true;
	    int swi = n_search_words - 1;
	    while((cascade) && (swi >= 0))
	    {
		if( (++pos[swi]) == stride[swi] )
		{
		    pos[swi] = 0;
		    swi--;
		    cascade = true;
		}
		else
		{
		    cascade = false;
		}
	    }
	}

	/*
	if(best_a != null)
	{
	    System.out.println("findBestSequence(): file=" + hi.filenames[file_id] + " best seq=");
	    for(int s=0; s < best_a.length; s++)
		System.out.print(" "+ best_a[s]);
	    System.out.println();
	}
	else
	{
	    System.out.println("findBestSequence(): file=" + hi.filenames[file_id] + "  no seq found");
	}
	*/

	return best_a;
    }

    // converts an array of word positions to an array of counts
    //
    // i.e. converts from a 'best seq' (i.e. word positions 34, 67, 99)
    // to the number of times the search word has been seen by the
    // time it occurs in the 'best seq'
    //
    private int[] convertSequenceToCount( int[] seq, 
					  final int n_files, final int n_search_words, 
					  int file_id, int[][][] master_posn_a )
    {
	int[] count =  new int[n_search_words];

	for(int sw=0; sw < n_search_words; sw++)
	{
	    // which occurence corresponds to the sequence position ?
	    for(int m=0; m < master_posn_a[ sw ][ file_id ].length; m++)
	    {
		{
		    if( master_posn_a[ sw ][ file_id ][ m ] == seq[ sw ] )
		    {
			count[ sw ] = m;
		    }
		}
	    }
	}
	return count;
    }

    // --------------------------------------------------------------------------------------

    private class MatchComparator implements java.util.Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Object[] d1 = (Object[]) o1;
	    Object[] d2 = (Object[]) o2;

	    Integer i1 = (Integer) d1[2];
	    Integer i2 = (Integer) d2[2];
	    
	    //return i1.compareTo(i2);

	    if(i1.equals(i2))
	    {
		Integer is1 = (Integer) d1[3];
		Integer is2 = (Integer) d2[3];
		
		return is2.compareTo(is1);
	    }
	    else
	    {
		return i1.compareTo(i2);
	    }
	    

	}
	public boolean equals(Object o) { return false; }
     }
   
    class NonEditableTableModel extends DefaultTableModel
    {
	public NonEditableTableModel(Object[][] data, Object[] colnames) 
	{
	    super(data,colnames);
	}

	public boolean isCellEditable(int row, int column) 
	{
	    return false;
	}
    }

    private void displayMatches( final String[] target,  final Object[][] matches )
    {
	current_table_data = matches;
	current_find_target = target;
	current_match_mode = 2;

	String title = "Find: " + (matches.length > 1 ? (matches.length + " Matches") : "1 Match") + " for \"";
	for(int t=0;t < target.length; t++)
	    title += (target[t] + (((t+1)==target.length) ? "\"" : " "));

	if(matches.length == 0)
	{
	    current_table_data = null;
	    details_table.setModel(new NonEditableTableModel(new Object[0][], null));
	    details_table.removeEditor();
	    String wdesc = (target.length == 1) ? "this word" : "these words";
	    mview.alertMessage("No documents contain " + wdesc);
	    
	    return;
	}


	String[] colnames = { "Document", "File", "Best Dist", "Matches"};

	details_table.setModel(new NonEditableTableModel( matches, colnames ));
	
	TableColumnModel tcm = details_table.getColumnModel();

	TableColumn column = tcm.getColumn(0);
	column.setPreferredWidth(250);
	column = tcm.getColumn(1);
	column.setPreferredWidth(250);
	column = tcm.getColumn(2);
	column.setPreferredWidth(60);
	column = tcm.getColumn(3);
	column.setPreferredWidth(60);

	details_table.setColumnModel(tcm);
	details_table.validate();
    }

    private void closeMatchDisplay() 
    {
	
    }

    // used when finding in this document...
    private void displayMatches( final int[] matches )
    {
	try
	{
	    current_table_data = matches;
	    current_match_mode = 1;
	    
	    Object[][] data = new Object[ matches.length ][];
	    
	    final Document doc = main_editor_pane.getDocument();
	    final String text  = doc.getText( 0, doc.getLength() );
	    
	    final int nm = matches.length;

	    for(int d=0; d <  nm; d++)
	    {
		int start = matches[d] - 20;
		if(start < 0)
		    start = 0;
		int end = start + 50;
		if(end > text.length())
		    end = text.length();
		data[d] = new String[1];
		data[d][0] = text.substring( start, end );
	    }
	    
	    String[] colnames = new String[1];
	    colnames[0] = (nm == 1) ?  "One Match"  : ( nm + " Matches" );
	    
	    details_table.setModel(new DefaultTableModel( data, colnames ));

	    
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	    current_table_data = null;
	    details_table.setModel(new DefaultTableModel(new Object[0][], null));
	}
    }

    // ======================================================================================

    // for some reason, JDK 1.2.2 appears to add a prefix to internal links
    // (i.e. xxx.html#label) 
    //
    // the prefix (possibly the urlbase?) doesn't appear in 1.3.0 
    //
    private URL fixURL( URL in )
    {
	if( in == null )
	    return null;

	String fname = in.toString();
	
	// does 'docs' occur more than once?
	
        final String srch = String.valueOf(File.separator) + "docs";

	int i1 = fname.indexOf( srch );
	int i2 = fname.indexOf( srch, i1+1);

	if(i2 >= 0)
	{
	    //System.out.println("fixURL() multiple \"" + srch + "\" in url");
	 
	    fname = fname.substring(0,i2) + fname.substring((i2+srch.length()));
	}

	try
	{
	    return new URL( fname );
	}
	catch( java.net.MalformedURLException murle )
	{
	    return in;
	}
    }
    

    // ======================================================================================


    private JComboBox history_jcb;
    private JPanel outer;
    private JSplitPane split_pane;
    private JScrollPane main_editor_scroll_pane;
    // private JScrollPane index_editor_scroll_pane;
    private JButton back_button, fore_button;
    private JEditorPane main_editor_pane;
    // private JEditorPane index_editor_pane;
    private maxdView mview;
    private JTree index_tree;
    private JTable details_table;

    /*
    private Vector history = new Vector();
    private Vector pos_history = new Vector();
    private int history_pos;
    */

    private int font_size = 12;
}

    
