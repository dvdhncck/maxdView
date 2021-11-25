/*
 * TTest.java
 *
 * Created on 18 July 2003, 09:47
 */
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
/**
 * This class is a plugin which allows the user to select
 *two sets of measurements and perform a student t test on them.
 *
 * @author  helen
 */
/*
 *The GUI stuff is kept here.
 *The actual calculation is done by StudentTTestCalculator
 */

public class TTest implements Plugin, ExprData.ExprDataObserver {
    
    private static String FRAME_TITLE = "TTest";
    private static double INIIAL_SPLIT_PANEL_DIVIDER_LOC = 0.5;
    
    private maxdView mview;
    private ExprData edata;

    private String colt_jar_file_path = null;

    //GUI objects
    private DragAndDropList m_measListA;
    private DragAndDropList m_measListB;
    
    private DragAndDropList m_selectedListA;
    private DragAndDropList m_selectedListB;
    
   // private static String[] m_outputMeasurements = {"t value", "p value", "mean A", "mean B", "mean diff" "var A", "var B", "degrees of freedom"};
   // private static String[] m_defaultNames = {"tvalue", "pvalue", "meanA", "meanB", "meanDiff", "varA", "varB", "degFre"};
    
    private static String DEFAULT_MEASNAME_T = "tvalue";
    private static String DEFAULT_MEASNAME_P2 = "p2value";
    private static String DEFAULT_MEASNAME_P1 = "p1value";
    private static String DEFAULT_MEASNAME_MEANA = "Amean";
    private static String DEFAULT_MEASNAME_MEANB = "Bmean";
    private static String DEFAULT_MEASNAME_MEANDIFF = "mndiff";
    private static String DEFAULT_MEASNAME_varA = "Avar";
    private static String DEFAULT_MEASNAME_varB = "Bvar";
    private static String DEFAULT_MEASNAME_DEGFREE = "df";
    private static Dimension DEFAULT_TXT_DIM = new Dimension(80,20);
    
    private static int DEFAULT_NUM_COLS = 30;
    
    //private JComboBox m_ttestTypeCombo;
    // private JTextPane m_explainArea;
    
    private JCheckBox m_tValueCheckBox;
    private JCheckBox m_pValueTwoCheckBox;
    private JCheckBox m_pValueOneCheckBox;
    private JCheckBox m_meanACheckBox;
    private JCheckBox m_meanBCheckBox;
    private JCheckBox m_meanDifCheckBox;
    private JCheckBox m_varACheckBox;
    private JCheckBox m_varBCheckBox;
    private JCheckBox m_degreesFreedomCheckBox;
    
    private JTextField m_tValueTextField;
    private JTextField m_pValueTwoTextField;
    private JTextField m_pValueOneTextField;
    private JTextField m_meanATextField;
    private JTextField m_meanBTextField;
    private JTextField m_meanDifTextField;
    private JTextField m_varATextField;
    private JTextField m_varBTextField;
    private JTextField m_degreesFreedomTextField;
    
    private JCheckBox m_applyFilterCheckbox;
    private JCheckBox m_reportStats;

    private ButtonGroup m_actionOnNANradio;
    private JRadioButton m_radioNAN;
    private JRadioButton m_radioDoBest;
    
    private JFrame m_frame;
    private JSplitPane m_splitPane;
    private JSplitPane m_splitPaneAnother;
    private JTabbedPane m_tabbed;
    
    
    private JButton m_ttestButton;
    
    private JButton m_nextButton;
    private JButton m_backButton;
    private JButton m_closeButton;
    private JButton m_helpButton;
    

    private TTestType selectedTest;

    /** Creates a new instance of StudentTTest */
    public TTest(maxdView mview_) {
        mview = mview_;
        edata = mview_.getExprData();
    }
    
    
    //BEGIN methods from Plugin
    public PluginCommand[] getPluginCommands() {
        return null;
    }
    
    public PluginInfo getPluginInfo() {
        PluginInfo pinf = new PluginInfo("TTest", 
					 "transform", 
					 "Student's T-Test", 
					 "Performs Student's T-Test<BR>" + 
					 "Plugin written by hhulme of Manchester Bioinformatics 2003<BR>" + 
					 "(Requires the 'colt' package from CERN)",
					 1, 1, 0);
        return pinf;
        
    }
    
    public void runCommand(String str, String[] str1, CommandSignal commandSignal) {
    }
    
    public void startPlugin() 
    {
	maxdView.CustomClassLoader ccl = (maxdView.CustomClassLoader) getClass().getClassLoader();
	
        String default_jar_file_location = 
	    mview.getTopDirectory() + 
	    java.io.File.separatorChar + 
	    "external" +
	    java.io.File.separatorChar + 
	    "colt" +
	    java.io.File.separatorChar + 
	    java.io.File.separatorChar + "colt.jar";


//	System.out.println("default_jar_file_location = " + default_jar_file_location );

	colt_jar_file_path = mview.getProperty("TTest.colt_jar_path", default_jar_file_location );
	
	
	mview.setMessage("Initialising Colt classes");
	Thread.yield();

	boolean found = false;
	while(!found)
	{
	    if( colt_jar_file_path != null )
	    {
		try
		{
		    ccl.addPath( colt_jar_file_path );
		}
		catch( java.net.MalformedURLException murle )
		{
		    String msg = 
			"Unable to load the Colt JAR file from '" + colt_jar_file_path + "'\n" +
			"\nPress \"Find\" to specify an alternate location for the file,\n" + 
			"  or\nPress \"Cancel\" to stop the plugin.\n" +
			"\n(see the help page for more information)\n";

		    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
			return;
		    
		    try
		    {
			colt_jar_file_path = mview.getFile("Location of 'colt.jar'", colt_jar_file_path );
		    }
		    catch( UserInputCancelled uic )
		    {
			return;
		    }
		}
	    }


	    //mview.showClassSearchLocations();

	    Class wc = ccl.findClass("cern.jet.stat.Gamma");
	    
	    found = (wc != null);
	    
	    if(!found)
	    {
		try
		{
		    String msg = "Unable to find the Colt JAR file\n";
		    msg += (colt_jar_file_path == null)  ? "\n" : ("in '" + colt_jar_file_path + "'\n");
		    msg += "\nPress \"Find\" to specify the location of the file,\n" + 
		    "  or\nPress \"Cancel\" to stop the plugin.\n" +
		    "\n(see the help page for more information)\n";
		    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
			return;
		    
		    colt_jar_file_path = mview.getFile("Location of 'colt.jar'", colt_jar_file_path);
		    
		}
		catch(UserInputCancelled uic)
		{
		    // don't start the plugin
		    return;
		}
	    }
	    else
	    {
		mview.putProperty("TTest.colt_jar_path", colt_jar_file_path);
	    }
	}

	// preload the required classes
	Class gamma = ccl.findClass("cern.jet.stat.Gamma");
	Class beta  = ccl.findClass("cern.jet.stat.Beta");


        addComponents();
        edata.addObserver(this);
        m_frame.pack();
        m_frame.setVisible(true);
        m_splitPane.setDividerLocation(INIIAL_SPLIT_PANEL_DIVIDER_LOC);
        //m_splitPaneAnother.setDividerLocation(INIIAL_SPLIT_PANEL_DIVIDER_LOC);
        
    }
    
    public void stopPlugin() {
        cleanup();
    }
    //END methods from Plugin

    //Begin GUI stuff
    private void addComponents() {
        JPanel outer_panel;
  
	GridBagConstraints constraints;

	GridBagLayout outer_gridbag;
        GridBagConstraints outer_constraints;
        
        DragAndDropPanel meas_pick_panel;
        GridBagLayout meas_pick_gridbag;
  
      
        
        JScrollPane scrollListA;
        JScrollPane scrollListB;
        
        JPanel button_panel;
        GridBagLayout button_gridbag;
        
       
       
        JScrollPane scrollListAA;
        JScrollPane scrollListBB;
        
        JPanel ttest_button_panel;
        GridBagLayout ttest_button_gridbag;
        

        
        
        
        //the main frame
        m_frame = new JFrame(this.FRAME_TITLE);
        mview.decorateFrame(m_frame);
        m_frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
        //an outer panel
        outer_panel = new JPanel();
        outer_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        outer_gridbag = new GridBagLayout();
        outer_panel.setLayout(outer_gridbag);
        
        outer_panel.setMinimumSize(new Dimension(640, 580));
        outer_panel.setPreferredSize(new Dimension(640, 580));
        
        //tabbed panel
        m_tabbed = new JTabbedPane();
        m_tabbed.setEnabled(false);
        
        outer_constraints = new GridBagConstraints();
        outer_constraints.gridx = 0;
        outer_constraints.gridy = 0;
        outer_constraints.weightx = 10.0;
        outer_constraints.weighty = 9.0;
        outer_constraints.fill = GridBagConstraints.BOTH;
        outer_gridbag.setConstraints(m_tabbed, outer_constraints);
        outer_panel.add(m_tabbed);


 
	// -------------------------------------------------------------------------------
        // meaurement picking  tab
	// -------------------------------------------------------------------------------


	{
	    meas_pick_panel = new DragAndDropPanel();
	    meas_pick_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    meas_pick_gridbag = new GridBagLayout();
	    meas_pick_panel.setLayout(meas_pick_gridbag);
	    
	    
	    
	    m_measListA = new DragAndDropList();
	    scrollListA = new JScrollPane(m_measListA);
	    m_measListA.setModel(new MeasListModel());
	    m_measListA.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    
	    m_measListB = new DragAndDropList();
	    scrollListB = new JScrollPane(m_measListB);
	    m_measListB.setModel(new MeasListModel());
	    m_measListB.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    
	    m_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollListA, scrollListB);
	    
	    
	    GridBagConstraints meas_pick_constraints = new GridBagConstraints();

	    meas_pick_constraints.weightx = 10.0;
	    meas_pick_constraints.weighty = 10.0;
	    meas_pick_constraints.fill = GridBagConstraints.BOTH;

	    meas_pick_gridbag.setConstraints(m_splitPane, meas_pick_constraints);

	    meas_pick_panel.add(m_splitPane);

	    m_tabbed.add(" Pick Measurements ", meas_pick_panel);
        }
       

	// -------------------------------------------------------------------------------
        // options tab
	// -------------------------------------------------------------------------------


        {
            JPanel options_panel = new JPanel();
            options_panel.setBorder(BorderFactory.createEmptyBorder(15,15,5,15));
            GridBagLayout options_gridbag = new GridBagLayout();
            options_panel.setLayout(options_gridbag);
            
	    // user_info_panel = new JPanel();
            // user_info_gridbag = new GridBagLayout();
            // user_info_constraints = new GridBagConstraints();
            
        /*  
	    info_text_area = new JTextPane();
            info_text_area.setBorder(BorderFactory.createEmptyBorder());
            info_text_area.setEditable(false);
            info_text_area.setMinimumSize(new Dimension(600, 100));
            info_text_area.setPreferredSize(new Dimension(600, 100));
            info_text_area.setAutoscrolls(true);
            info_text_area.setFont(mview.getSmallFont());
            info_text_area.setText(getInfoText());
            info_text_area.setBackground(user_info_panel.getBackground());
            
            info_scrollpane = new JScrollPane(info_text_area);
            
            user_info_constraints.gridx = 0;
            user_info_constraints.gridy = 0;
            user_info_constraints.weightx = 1.0;
            user_info_constraints.weighty = 1.0;
            user_info_constraints.fill= GridBagConstraints.BOTH;
            
            user_info_gridbag.setConstraints(info_scrollpane, user_info_constraints);
            user_info_panel.add(info_scrollpane);
         */
            
            //options_gridbag.setConstraints(user_info_panel, constraints);
            //options_panel.add(user_info_panel);
            
            
            /*
            m_selectedListA = new DragAndDropList();
            m_selectedListA.setEnabled(false);
            m_selectedListA.setListData(m_measListA.getSelectedValues());
            
            m_selectedListB = new DragAndDropList();
            m_selectedListB.setEnabled(false);
            m_selectedListB.setListData(m_measListB.getSelectedValues());
            
            
            
            scrollListAA = new JScrollPane(m_selectedListA);
            scrollListBB = new JScrollPane(m_selectedListB);
            
            m_splitPaneAnother = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollListAA, scrollListBB);
            
            constraints = new GridBagConstraints();
            constraints.gridy = line++;
            constraints.weightx = 1.0;
            constraints.weighty = 6.0;
            constraints.gridwidth = 2;
	    //constraints.anchor = GridBagConstraints.CENTER;
            constraints.fill = GridBagConstraints.BOTH;
            
            options_gridbag.setConstraints(m_splitPaneAnother, constraints);
            options_panel.add(m_splitPaneAnother);
	    */

            
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

            JPanel measNamesPanel = createMeasNamesPanel();
            
            //measNamesPanel.setBackground(Color.pink);
            
            constraints = new GridBagConstraints();
	    constraints.gridx = 0;
	    constraints.gridy = 0;
            constraints.weightx = 10.0;
	    constraints.weighty = 1.0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
           
            options_gridbag.setConstraints(measNamesPanel, constraints);
            options_panel.add(measNamesPanel);
            

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	    addFiller( options_panel, options_gridbag, 1, 0, 32 );

	    
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


            JPanel radioPanel = createRadioPanel();
            
            
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
	    constraints.gridy = 2;
            constraints.weightx = 10.0;
	    constraints.weighty = 1.0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;

            options_gridbag.setConstraints(radioPanel, constraints);
            options_panel.add(radioPanel);


            initialiseListListeners();

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	    
	    addFiller( options_panel, options_gridbag, 3, 0, 32 );

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	    
            JPanel otherPanel = createOtherOptionsPanel();
            
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
	    constraints.gridy = 4;
            constraints.weightx = 10.0;
	    constraints.weighty = 1.0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;

            options_gridbag.setConstraints(otherPanel, constraints);
            options_panel.add(otherPanel);

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


	    m_tabbed.add(" Choose Options ", options_panel);
 
        }


	// -------------------------------------------------------------------------------
        // execution tab
	// -------------------------------------------------------------------------------


	{
        
            JPanel exec_panel = new JPanel();
            exec_panel.setBorder(BorderFactory.createEmptyBorder(5,15,5,15));
            GridBagLayout exec_gridbag = new GridBagLayout();
            exec_panel.setLayout(exec_gridbag);
 

	    JPanel comboPanel = createTypeComboPanel();
            constraints = new GridBagConstraints();
	    constraints.gridy = 0;
	    constraints.weighty = 8.0;
	    constraints.anchor = GridBagConstraints.NORTHWEST;
	    //constraints.fill = GridBagConstraints.BOTH;
	    exec_gridbag.setConstraints(comboPanel, constraints);
            exec_panel.add(comboPanel);

	    addFiller( exec_panel, exec_gridbag, 1, 0, 48 );

          
            createTtestButton();
	    constraints = new GridBagConstraints();
	    constraints.gridy = 2;
	    constraints.weighty = 1.0;
	    constraints.anchor = GridBagConstraints.SOUTH;
            exec_gridbag.setConstraints(m_ttestButton, constraints);
            exec_panel.add(m_ttestButton);

   
	    m_tabbed.add(" Perform T-Test ", exec_panel);

            updateTbuttonEnabledness();            
 
	}

 	// -------------------------------------------------------------------------------
        // buttons
	// -------------------------------------------------------------------------------

        {
            button_panel = new JPanel();
            button_gridbag = new GridBagLayout();
            button_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
            button_panel.setLayout(button_gridbag);
            
            createButtons();
            initialiseTabTracker();
            
            GridBagConstraints button_constraints = new GridBagConstraints();
            button_constraints.gridx = 0;
            button_constraints.gridy = 0;
            button_constraints.weightx = 1.0;
            button_constraints.weighty = 1.0;
            
            button_gridbag.setConstraints(m_backButton, button_constraints);
            button_panel.add(m_backButton, button_constraints);
            button_constraints.gridx++;
            button_gridbag.setConstraints(m_nextButton, button_constraints);
            button_panel.add(m_nextButton, button_constraints);
            button_constraints.gridx++;
            button_gridbag.setConstraints(m_closeButton, button_constraints);
            button_panel.add(m_closeButton, button_constraints);
            button_constraints.gridx++;
            button_gridbag.setConstraints(m_helpButton, button_constraints);
            button_panel.add(m_helpButton, button_constraints);
            
            

            outer_constraints.gridy++;
            outer_constraints.weighty=1.0;
            outer_gridbag.setConstraints(button_panel, outer_constraints);
            outer_panel.add(button_panel);
            
        }
        
        
        
        m_frame.getContentPane().add(outer_panel);
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



    private JPanel createTypeComboPanel() 
    {
        JPanel comboPanel = new JPanel();
        comboPanel.setBorder(BorderFactory.createEmptyBorder());
        //comboPanel.setPreferredSize(new Dimension(630,160));
        //comboPanel.setMinimumSize(new Dimension(630,160));
        GridBagLayout gb = new GridBagLayout();
        comboPanel.setLayout( gb );

        final TTestType standardType = new TTestType(StudentTTestCalculator.TTEST_TYPE_STANDARD_VARSAME, 
						     "standard t test", 
						     "T-test in which the distributions are assumed to be normaland variances are assumed to be the same",  
						     "<HTML><P><NOBR>T-test in which <I>distributions</I> are assumed to be normal and </NOBR><BR><NOBR> the <I>variances</I> are assumed to be the same.</NOBR></P><BR><P>The formula is:</P><P><TT>t=(meanA - meanB) / sqrt(alpha * beta)</TT><BR>&nbsp;&nbsp;where<BR><TT>alpha = (n[A] + n[B])/(n[A] * n[B])</TT><BR>&nbsp;&nbsp;and<BR><NOBR><TT>beta = ((n[A]-1)*varA + (n[B]-1)*varB)/(n[A]+n[B]-2)</TT></NOBR></P></HTML>");
	
        final TTestType varianceDiffersType = new TTestType(StudentTTestCalculator.TTEST_TYPE_VARSDIFFER, 
							    "vars t-test", 
							    "T-test in which the distributions are assumed to be normal, variances are not assumed to be the same", 
							    "<HTML><NOBR>T-test in which <I>distributions</I> are assumed to be normal and</NOBR><BR><NOBR>the <I>variances</I> are <B>not</B> assumed to be the same.</NOBR></P><BR><P>The formula is:</P><P><TT>t=(meanA - meanB)/sqrt(varA/n[A] + varB/n[B])</TT></P><BR><P><NOBR>The effective degrees of freedom calculated using variance estimates.</NOBR></P></HTML>");
        
        

	final JLabel explanation_label = new JLabel( standardType.getLongDescription() );
	explanation_label.setBorder( BorderFactory.createEmptyBorder( 0, 16, 0, 0 ) );
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.gridx = 1;
	constraints.gridy = 0;
	constraints.gridheight = 2;
        constraints.weightx = 9.0;
        constraints.weighty = 5.0;
	constraints.anchor = GridBagConstraints.NORTHWEST;
	constraints.fill = GridBagConstraints.HORIZONTAL;
	gb.setConstraints( explanation_label, constraints );
	
	comboPanel.add( explanation_label );
	

	JRadioButton same_jrb = new JRadioButton("Standard T-Test");

	constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = 0;
        constraints.weightx = 1.0;
	// constraints.weighty = 1.0;
	constraints.anchor = GridBagConstraints.NORTHWEST;
	gb.setConstraints( same_jrb, constraints );
	comboPanel.add( same_jrb );
	
	same_jrb.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    //System.out.println("ACTION PERFORMED");
		    selectedTest = standardType;
		    explanation_label.setText( standardType.getLongDescription() );
		    
		    
            }
        });


	JRadioButton vars_jrb = new JRadioButton("Vars T-Test");

	constraints = new GridBagConstraints();
	constraints.gridx = 0;
	constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
	constraints.anchor = GridBagConstraints.NORTHWEST;	
	gb.setConstraints( vars_jrb, constraints );
	comboPanel.add( vars_jrb );
	
	vars_jrb.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    //System.out.println("ACTION PERFORMED");
		    selectedTest = varianceDiffersType;
		    explanation_label.setText( varianceDiffersType.getLongDescription() );
		    
		    
            }
        });


 	

	ButtonGroup bg = new ButtonGroup();
	bg.add( same_jrb );
	bg.add( vars_jrb );
	same_jrb.setSelected( true );
	explanation_label.setText( standardType.getLongDescription() );

	selectedTest = standardType;

        return comboPanel;
    }
    
    
    
    private JPanel createRadioPanel() {
        JPanel radioPanel = new JPanel();


        radioPanel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createLineBorder(Color.black),
								   " Action to take when NaN values are found "  ) );



        GridBagLayout gb = new GridBagLayout();
        radioPanel.setLayout( gb );

     
        boolean useRemainingVals = mview.getBooleanProperty("TTest.useRemainingVals", true);
        m_radioNAN = new JRadioButton("Set t-test results to NaN", !useRemainingVals);
        m_radioDoBest = new JRadioButton("Do calculation using remaining values", useRemainingVals);
        
        
	GridBagConstraints c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
        gb.setConstraints(m_radioNAN, c);
        radioPanel.add(m_radioNAN);
        
        c = new GridBagConstraints();
	c.gridy = 1;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
        gb.setConstraints(m_radioDoBest,c);
        radioPanel.add(m_radioDoBest);
        
        m_actionOnNANradio = new ButtonGroup();
	m_actionOnNANradio.add(m_radioNAN);
	m_actionOnNANradio.add(m_radioDoBest);
	
	m_radioNAN.setSelected( true );

        return radioPanel;
    }
    
    private JPanel createOtherOptionsPanel() {
        JPanel panel = new JPanel();


        panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createLineBorder(Color.black),
								   " Other options "  ) );


        GridBagLayout gb = new GridBagLayout();
        
	panel.setLayout( gb );

	m_applyFilterCheckbox = new JCheckBox("Apply filter");
	m_applyFilterCheckbox.setSelected(mview.getBooleanProperty("TTest.applyFilter", false));
	
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.weightx = 1.0;
	constraints.anchor = GridBagConstraints.WEST;
	gb.setConstraints(m_applyFilterCheckbox, constraints);
	panel.add(m_applyFilterCheckbox);
	
	m_reportStats = new JCheckBox("Report statistics");
	m_reportStats.setSelected(mview.getBooleanProperty("TTest.reportStatistics", false));
	
	constraints = new GridBagConstraints();
	constraints.weightx = 1.0;
	constraints.gridy = 1;
	constraints.anchor = GridBagConstraints.WEST;
	gb.setConstraints(m_reportStats, constraints);
	panel.add(m_reportStats);

	return panel;
    }

    private JPanel createMeasNamesPanel() {
        
        JPanel measNamesPanel = new JPanel();

	measNamesPanel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createLineBorder(Color.black),
								   " Choose which data to create "  ) );

        //measNamesPanel.setBorder(BorderFactory.createEmptyBorder());

        GridBagLayout gb = new GridBagLayout();
        measNamesPanel.setLayout(gb);
        GridBagConstraints c = new GridBagConstraints();
        
        //JTextPane messagePane = new JTextPane();
        //messagePane.setBackground(measNamesPanel.getBackground());
        //messagePane.setFont(mview.getSmallFont());
        //messagePane.setText("Choose which measurements to create");
        //messagePane.setEditable(false);
        
        
        c.gridx = 0;
        c.gridy = 0;
        //c.gridwidth = 2;
        //c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        //gb.setConstraints(messagePane,c);
        //measNamesPanel.add(messagePane);
        
	JLabel title = new JLabel( " Value " );
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(title,c);
        measNamesPanel.add(title);


	title = new JLabel( " Name " );
        c.gridx++;
        c.weightx = 9.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(title,c);
        measNamesPanel.add(title);

        
        //T VALUE
        m_tValueCheckBox = new JCheckBox("t value");
        m_tValueCheckBox.setSelected(mview.getBooleanProperty("TTest.makeTValue",true));
        c.gridx--;
        c.gridy++;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_tValueCheckBox,c);
        measNamesPanel.add(m_tValueCheckBox);
        
        
        m_tValueTextField = new JTextField(DEFAULT_MEASNAME_T, DEFAULT_NUM_COLS);
        m_tValueTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_tValueTextField,c);
        measNamesPanel.add(m_tValueTextField);
        
        addEnableListener( m_tValueCheckBox, m_tValueTextField);
        
        //P2 Value
        m_pValueTwoCheckBox = new JCheckBox("2 tailed p value");
        m_pValueTwoCheckBox.setSelected(mview.getBooleanProperty("TTest.makePValueTwo",true));
        c.gridy++;
        c.gridx--;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_pValueTwoCheckBox,c);
        measNamesPanel.add(m_pValueTwoCheckBox);
        
        
        m_pValueTwoTextField = new JTextField(DEFAULT_MEASNAME_P2, DEFAULT_NUM_COLS);
        m_pValueTwoTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
	gb.setConstraints(m_pValueTwoTextField,c);
        measNamesPanel.add(m_pValueTwoTextField);
        
        addEnableListener( m_pValueTwoCheckBox, m_pValueTwoTextField);
        
        //P2 Value
        m_pValueOneCheckBox = new JCheckBox("1 tailed p value");
        m_pValueOneCheckBox.setSelected(mview.getBooleanProperty("TTest.makePValueOne",false));
        c.gridy++;
        c.gridx--;
        c.weightx = 1.0;
         c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_pValueOneCheckBox,c);
        measNamesPanel.add(m_pValueOneCheckBox);
        
        
        m_pValueOneTextField = new JTextField(DEFAULT_MEASNAME_P1, DEFAULT_NUM_COLS);
        m_pValueOneTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_pValueOneTextField,c);
        measNamesPanel.add(m_pValueOneTextField);
        
        addEnableListener( m_pValueOneCheckBox, m_pValueOneTextField);
        
        
        //MEAN A
        m_meanACheckBox = new JCheckBox("mean A");
        m_meanACheckBox.setSelected(mview.getBooleanProperty("TTest.makeMeanAValue",false));
        c.gridy++;
        c.gridx--;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_meanACheckBox,c);
        measNamesPanel.add(m_meanACheckBox);
        
        m_meanATextField = new JTextField(DEFAULT_MEASNAME_MEANA, DEFAULT_NUM_COLS);
        m_meanATextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_meanATextField,c);
        measNamesPanel.add(m_meanATextField);
        
        addEnableListener( m_meanACheckBox, m_meanATextField);
        
        //MEAN B
        m_meanBCheckBox = new JCheckBox("mean B");
        m_meanBCheckBox.setSelected(mview.getBooleanProperty("TTest.makeMeanBValue",false));
        c.gridy++;
        c.gridx--;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_meanBCheckBox,c);
        measNamesPanel.add(m_meanBCheckBox);
        
        m_meanBTextField = new JTextField(DEFAULT_MEASNAME_MEANB, DEFAULT_NUM_COLS);
        m_meanBTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_meanBTextField,c);
        measNamesPanel.add(m_meanBTextField);
        
        addEnableListener( m_meanBCheckBox, m_meanBTextField);
        
        //MEAN DIFF
        m_meanDifCheckBox = new JCheckBox("mean difference");
        m_meanDifCheckBox.setSelected(mview.getBooleanProperty("TTest.makeMeanDifValue",false));
        c.gridy++;
        c.gridx--;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_meanDifCheckBox,c);
        measNamesPanel.add(m_meanDifCheckBox);
        
        m_meanDifTextField = new JTextField(DEFAULT_MEASNAME_MEANDIFF, DEFAULT_NUM_COLS);
        m_meanDifTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_meanDifTextField,c);
        measNamesPanel.add(m_meanDifTextField);
        
        addEnableListener( m_meanDifCheckBox, m_meanDifTextField);
        
        //VAR A
        m_varACheckBox = new JCheckBox("variance A");
        m_varACheckBox.setSelected(mview.getBooleanProperty("TTest.makevarAValue",false));
        c.gridy++;
        c.gridx--;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_varACheckBox,c);
        measNamesPanel.add(m_varACheckBox);
        
        m_varATextField = new JTextField(DEFAULT_MEASNAME_varA, DEFAULT_NUM_COLS);
        m_varATextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_varATextField,c);
        measNamesPanel.add(m_varATextField);
        
        addEnableListener( m_varACheckBox, m_varATextField);
        
        //VAR B
        m_varBCheckBox = new JCheckBox("variance B");
        m_varBCheckBox.setSelected(mview.getBooleanProperty("TTest.makevarBValue",false));
        c.gridy++;
        c.gridx--;
	c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_varBCheckBox,c);
        measNamesPanel.add(m_varBCheckBox);
        
        m_varBTextField = new JTextField(DEFAULT_MEASNAME_varB, DEFAULT_NUM_COLS);
        m_varBTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_varBTextField,c);
        measNamesPanel.add(m_varBTextField);
        
        addEnableListener( m_varBCheckBox, m_varBTextField);
        
        //DEGFRE
        
        String toolTipDegFree = "For standard t-test, degs freedom is the same for every spot";
        
        m_degreesFreedomCheckBox = new JCheckBox("degrees of freedom");
        m_degreesFreedomCheckBox.setToolTipText(toolTipDegFree);
        m_degreesFreedomCheckBox.setSelected(mview.getBooleanProperty("TTest.makeDFValue",false));
        c.gridy++;
        c.gridx--;
	c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        gb.setConstraints(m_degreesFreedomCheckBox,c);
        measNamesPanel.add(m_degreesFreedomCheckBox);
        
        m_degreesFreedomTextField = new JTextField(DEFAULT_MEASNAME_DEGFREE, DEFAULT_NUM_COLS);
        m_degreesFreedomTextField.setMinimumSize(DEFAULT_TXT_DIM);
        c.gridx++;
        c.weightx = 9.0;
        gb.setConstraints(m_degreesFreedomTextField,c);
        measNamesPanel.add(m_degreesFreedomTextField);
        addEnableListener( m_degreesFreedomCheckBox, m_degreesFreedomTextField);
        
        return measNamesPanel;
    }
    

    private void addEnableListener(final JCheckBox checkBox, final JTextField textField) {
        //init state;
        textField.setEnabled(checkBox.isSelected());
        checkBox.addChangeListener(new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent event) {
                textField.setEnabled(checkBox.isSelected());
            }
        });
    }
    

    
    private void createTtestButton() {
        m_ttestButton = new JButton("Perform T-Test");
        m_ttestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ttestButtonPressed();
            }
        });
    }
    
    private void createButtons() {
            m_nextButton = new JButton("Next");
            m_backButton = new JButton("Back");
            m_closeButton = new JButton("Close");
            m_helpButton = new JButton("Help");
            
            m_nextButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gotoNextTab();
                }
            });
            
            m_backButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gotoPrevTab();
                }
            });
            
            m_closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cleanup();
                }
            });
            
            m_helpButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mview.getPluginHelpTopic("TTest", "TTest");
                }
            });
    }
    
    private void ttestButtonPressed() {
        //collect current states      
        Object[] selectedA =   m_measListA.getSelectedValues();
        Object[] selectedB = m_measListB.getSelectedValues();
        int lenA;
        int lenB;
        int numSpots;
        double[][] dataSetA;
        double[][] dataSetB;
        boolean[] filterInfo;
        double[] tvalues;
        
        boolean doTvalue;
        boolean doP2value;
        boolean doP1value;
        boolean doMeanA;
        boolean doMeanB;
        boolean doVarA;
        boolean doVarB;
        boolean doMeanDif;
        
        boolean doDegFre;
        
        int typeInt;
        int actionOnNull;
        
        String tValueMeasName = DEFAULT_MEASNAME_T;
        String pValueTwoMeasName = DEFAULT_MEASNAME_P2;
        String pValueOneMeasName = DEFAULT_MEASNAME_P1;
        String meanAMeasName = DEFAULT_MEASNAME_MEANA;
        String meanBMeasName = DEFAULT_MEASNAME_MEANB;
        String meanDifMeasName = DEFAULT_MEASNAME_MEANDIFF;
        String varAMeasName = DEFAULT_MEASNAME_varA;
        String varBMeasName = DEFAULT_MEASNAME_varB;
        String degFreeMeasName = DEFAULT_MEASNAME_DEGFREE;
        
        boolean doFiltering;
        
        if (selectedA==null || selectedB == null) {
            //shout
            mview.informationMessage("TTest: you need to select two sets of measurements");
            return;
        }
        
        lenA = selectedA.length;
        lenB = selectedB.length;
        
        if (lenA == 0 || lenB ==0) {
            mview.informationMessage("TTest: you need to select two sets of measurements");
            return;
        }
        if (lenA == 1 && lenB == 1) {
            mview.informationMessage("TTest: Comparing two single measurements gives no variance estimate. Ttest abandonned");
            return;
        }
        if (lenA ==1 || lenB ==1) {
            //WARN
            mview.informationMessage("TTest, warning: one of your sets contains only a single measurement. continuing anyway");
        }
        
        doTvalue = m_tValueCheckBox.isSelected();
        doMeanDif = m_meanDifCheckBox.isSelected();
        doP2value = m_pValueTwoCheckBox.isSelected();
        doP1value = m_pValueOneCheckBox.isSelected();
        doMeanA = m_meanACheckBox.isSelected();
        doMeanB = m_meanBCheckBox.isSelected();
        doVarA = m_varACheckBox.isSelected();
        doVarB = m_varBCheckBox.isSelected();
        doDegFre = m_degreesFreedomCheckBox.isSelected();
        
        if (m_radioNAN.isSelected()) {
            actionOnNull = StudentTTestCalculator.ACTION_ON_NAN_NAN;
        } else {
            actionOnNull = StudentTTestCalculator.ACTION_ON_NAN_USE_OTHERS;
        }
        
        typeInt = selectedTest.getIdentifier();
        
        //
        //validate new measurement names
        
        if (doTvalue) {
            tValueMeasName = m_tValueTextField.getText().trim();
            if (tValueMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new ttest result measurement");
                return;
            }
        }
        
        if (doP2value) {
            pValueTwoMeasName = m_pValueTwoTextField.getText().trim();
            if (pValueTwoMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new P-value result measurement");
                return;
            }
        }
        
        if (doP1value) {
            pValueOneMeasName = m_pValueOneTextField.getText().trim();
            if (pValueOneMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new P-value result measurement");
                return;
            }
        }
        
         if (doMeanA) {
            meanAMeasName = m_meanATextField.getText().trim();
            if (meanAMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new mean A result measurement");
                return;
            }
        }
        
        if (doMeanB) {
            meanBMeasName = m_meanBTextField.getText().trim();
            if (meanBMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new mean B result measurement");
                return;
            }
        }
        
        if (doMeanDif) {
            meanDifMeasName = m_meanDifTextField.getText().trim();
            if (meanDifMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new mean difference result measurement");
                return;
            }
        }
        
        if (doVarA) {
            varAMeasName = m_varATextField.getText().trim();
            if (varAMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new varA result measurement");
                return;
            }
        }
        
         if (doVarB) {
            varBMeasName = m_varBTextField.getText().trim();
            if (varBMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new varB result measurement");
                return;
            }
        }
        
        if (doDegFre) {
            degFreeMeasName = m_degreesFreedomTextField.getText().trim();
            if (degFreeMeasName.length() == 0) {
                mview.informationMessage("Please supply a name for the new degrees of freedom result measurement");
                return;
            }
        }
        
        
        //above method could have taken time, so need to reset, and revalidate measurements
        selectedA =  m_measListA.getSelectedValues();
        selectedB =  m_measListB.getSelectedValues();
        lenA = selectedA.length;
        lenB = selectedB.length;
        
        
        if (lenA == 0 || lenB ==0) {
            // SHOUT
            mview.informationMessage("TTest: you need to select two sets of measurements");
            return;
        }
        if (lenA == 1 && lenB == 1) {
            mview.informationMessage("TTest: Comparing two single measurements gives no variance estimate. Ttest abandonned");
            //SHOUT;
            return;
        }
        if (lenA ==1 || lenB ==1) {
            //WARN
        }
        
        doFiltering = m_applyFilterCheckbox.isSelected();
        //get Data
        dataSetA = new double[lenA][];
        dataSetB = new double[lenB][];
        numSpots = edata.getNumSpots();
        if (doFiltering) {
            filterInfo = new boolean[numSpots];
            for (int i = 0 ; i < numSpots; i++) {
                filterInfo[i] = edata.filter(i);
                
            }
        } else {
            filterInfo = null;
        }
        for (int i = 0 ; i < lenA ; i++) {
            dataSetA[i] = edata.getMeasurementData((String) selectedA[i]);
        }
        for (int i = 0 ; i < lenB ; i++) {
            dataSetB[i] = edata.getMeasurementData((String) selectedB[i]);
        }
        
 
        
    

    
        StudentTTestCalculator calculator = new StudentTTestCalculator();
         boolean success =  calculator.doTTest(dataSetA, dataSetB, filterInfo, typeInt, actionOnNull);
         if (!success) {
             mview.informationMessage("Sorry, something went wrong. T value not calculated");
             return;
         }
    //END doing the ttest calculation
        
        //create new measurements
        
         if (doMeanA) {
             edata.addMeasurement(edata.new Measurement(meanAMeasName, ExprData.UnknownDataType, calculator.getMeansA()));
         }
         if (doMeanB) {
             edata.addMeasurement(edata.new Measurement(meanBMeasName, ExprData.UnknownDataType, calculator.getMeansB()));
         }
         if (doMeanDif) {
             edata.addMeasurement(edata.new Measurement(meanDifMeasName, ExprData.UnknownDataType, calculator.getMeanDiffs()));
         }
         if (doVarA) {
             edata.addMeasurement(edata.new Measurement(varAMeasName, ExprData.UnknownDataType, calculator.getVarA()));
         }
         if (doVarB) {
             edata.addMeasurement(edata.new Measurement(varBMeasName, ExprData.UnknownDataType, calculator.getVarB()));
         }
         
         if (doTvalue) {
             
        edata.addMeasurement(edata.new Measurement(tValueMeasName, ExprData.UnknownDataType, calculator.getTValues()));
         }
         
          if (doP2value) {
              PvalueCalculator pcalculator = new PvalueCalculator();
              double[] pvalues = pcalculator.pvalueTwoTail(calculator.getTValues(), calculator.getDegsFreedom());
             edata.addMeasurement(edata.new Measurement(pValueTwoMeasName, ExprData.UnknownDataType, pvalues));
         }
         
         if (doP1value) {
              PvalueCalculator pcalculator = new PvalueCalculator();
              double[] pvalues = pcalculator.pvalueOneTail(calculator.getTValues(), calculator.getDegsFreedom());
             edata.addMeasurement(edata.new Measurement(pValueOneMeasName, ExprData.UnknownDataType, pvalues));
         }
         
         if (doDegFre) {
             edata.addMeasurement(edata.new Measurement(degFreeMeasName, ExprData.UnknownDataType, calculator.getDegsFreedom()));
         }

	 if( m_reportStats.isSelected() )
	 {
	     //display info
	     StringBuffer infoBuf = new StringBuffer();
	     
	     infoBuf.append("Measurements for set A used were:\n");
	     for (int i = 0 ; i < lenA; i++) {
		 infoBuf.append("\t" + selectedA[i].toString() + "\n");
	     }
	     infoBuf.append("\nMeasurements for set B used were:\n");
	     for (int i = 0 ; i < lenB; i++) {
		 infoBuf.append("\t" + selectedB[i].toString() + "\n");
	     }
	     infoBuf.append("The T Test used was '");
	     infoBuf.append(selectedTest.getName() + "'\n\n");
	     if (typeInt == StudentTTestCalculator.TTEST_TYPE_STANDARD_VARSAME) {
		 infoBuf.append("The number of degrees of freedom is:\n");
		 infoBuf.append("("+lenA+"-1)+("+lenB+"-1)\n");
		 infoBuf.append("=" + (lenA + lenB - 2) +"\n");
	     } else if (typeInt == StudentTTestCalculator.TTEST_TYPE_VARSDIFFER) {
		 infoBuf.append("The effective number of degrees of freedom\n");
		 infoBuf.append("will vary from spot to spot\n");
		 infoBuf.append("and will be less than "+ (lenA + lenB -2) +" in each case.\n");
	     }
	     infoBuf.append("\n");
	     
	     mview.successMessage(infoBuf.toString());
	 }
	 
	 
	 return;
        
    }
    
    private boolean validateCheckboxPair(JCheckBox checkBox, JTextField field) {
        if (checkBox.isSelected()) {
            if (field.getText().trim().length() == 0) {
                return false;
            }
        } 
        return true;
    }
    
    private String getNewMeasName() throws UserInputCancelled {
        String init_name = "ttest_result";
        String name;
        
        name = mview.getString( "Name for new Measurement", init_name);
        while (name.length() == 0) {
            mview.alertMessage("You must specify a unique name");
            name = mview.getString( "Name for new Measurement", init_name);
        }
        return name;
    }
    
    private String getInfoText() {
        StringBuffer sb = new StringBuffer();
        sb.append("Suggested defaults: use standard t-test, 2-tailed p value\n");
        sb.append("Report degs freedom if you wish to interpret t values directly\n");
        return sb.toString();
    }
    
    private void gotoPrevTab() {
        int index = m_tabbed.getSelectedIndex();
        if (index>0) {
            m_tabbed.setSelectedIndex(index-1);
        }
    }
    
    private void gotoNextTab() {
        int index = m_tabbed.getSelectedIndex();
        int maxIndex = m_tabbed.getTabCount();
        if (index < maxIndex-1) {
            m_tabbed.setSelectedIndex(index+1);
        }
    
    }
    
    private void initialiseListListeners() {
        m_measListB.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //m_selectedListB.setListData(m_measListB.getSelectedValues());
                updateTbuttonEnabledness();
            }
        });
        
        m_measListA.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //m_selectedListA.setListData(m_measListA.getSelectedValues());
                updateTbuttonEnabledness();
            }
        });
    }
        
    private void updateTbuttonEnabledness() {
                Object[] selA = m_measListA.getSelectedValues();
                Object[] selB = m_measListB.getSelectedValues();
                if (selA == null || selA.length == 0) {
                    m_ttestButton.setEnabled(false);
                } else if (selB == null || selB.length == 0) {
                    m_ttestButton.setEnabled(false);
                } else {
                    m_ttestButton.setEnabled(true);
                }
            }
        
        
    
    
    private void initialiseTabTracker() {
        m_tabbed.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                int numTabs = m_tabbed.getTabCount();
                int selectedIndex = m_tabbed.getSelectedIndex();
                if (selectedIndex == 0) {
                    m_backButton.setEnabled(false);
                } else {
                    m_backButton.setEnabled(true);
                }
                if (selectedIndex == numTabs-1) {
                    m_nextButton.setEnabled(false);
                } else {
                    m_nextButton.setEnabled(true);
                }
            }
        });
        //kickstart it:
        if ( m_tabbed.getSelectedIndex() == m_tabbed.getTabCount()-1 ) {
            m_nextButton.setEnabled(false);
        }
        if ( m_tabbed.getSelectedIndex() == 0) {
            m_backButton.setEnabled(false);
        }
    }
    
    private void cleanup() 
    {
        mview.putBooleanProperty("TTest.makeTValue", m_tValueCheckBox.isSelected());
        mview.putBooleanProperty("TTest.makePValueTwo", m_pValueTwoCheckBox.isSelected());
        mview.putBooleanProperty("TTest.makePValueOne", m_pValueOneCheckBox.isSelected());
        mview.putBooleanProperty("TTest.makeMeanAValue", m_meanACheckBox.isSelected());
        mview.putBooleanProperty("TTest.makeMeanBValue", m_meanBCheckBox.isSelected());
        mview.putBooleanProperty("TTest.makeMeanDifValue", m_meanDifCheckBox.isSelected());
        mview.putBooleanProperty("TTest.makevarAValue", m_varACheckBox.isSelected());
        mview.putBooleanProperty("TTest.makevarBValue", m_varBCheckBox.isSelected());
        mview.putBooleanProperty("TTest.makeDFValue", m_degreesFreedomCheckBox.isSelected());
        mview.putBooleanProperty("TTest.applyFilter", m_applyFilterCheckbox.isSelected());
        mview.putBooleanProperty("TTest.reportStatistics", m_reportStats.isSelected());
        mview.putBooleanProperty("TTest.useRemainingVals", m_radioDoBest.isSelected());
        edata.removeObserver(this);
        if (m_frame !=null) {
            m_frame.setVisible(false);
        }
        
    }
    //End GUI stuff
    
 
    
 
    
    //BEGIN methods inherited from ExprDataObserver
    public void clusterUpdate(ExprData.ClusterUpdateEvent cue) {
    }
    
    public void dataUpdate(ExprData.DataUpdateEvent due) {
    }
    
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue) {
    }
    
    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue) {
        switch(mue.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    m_measListA.setModel(new MeasListModel());
            m_measListB.setModel(new MeasListModel());
            m_tabbed.setSelectedIndex(0);
	    
	    break;
	}
        
    }
    //END methods from ExprDataObserver
    
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
    
}
