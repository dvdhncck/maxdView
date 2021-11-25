/*
 * QCChart.java
 *
 * Created on 01 June 2004, 16:55
 *by Helen Hulme
 */
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;
/**
 *This is the Plugin / User input interface
 *for the Quality Control Chart
 *developed by Yonxiang Fang.
 *The user chooses data which corresponds to
 *the (normalised) log ratio of several replicate slides.
 *Via this interfact, the user also specifies replicate info.
 *The plugin then calculates quality control info and
 *displays this info.
 * @author  hhulme
 */
public class QCChart implements Plugin, ExprData.ExprDataObserver {
    
    /**Default significance level for control lines*/
    public static double DEFAULT_SIG_LEVEL = 0.01;
    
    public static double DEFAULT_CONFIDENCE_LEVEL = 0.2;
    
    private static int SIG_MESSAGE_MAIN = 0;
    private static int SIG_MESSAGE_MU = 1;
    private static int SIG_MESSAGE_SIG = 2;
    
    /**Plugin's reference to parent maxdview
     */
    private maxdView mview;
    /**Plugin's reference to data of parent maxdview
     */
    private ExprData edata;
    /**refernce to the path of the colt jar (developed by cern)
     *which is used for some of the stats calculations
     */
    private String colt_jar_file_path = null;
    
    //static fields
    /**
     *Title of the frame of the plugin
     */
    private static String FRAME_TITLE = "Quality Control Chart Calculator";
    
    
    //Fields for the Input GUI
    
    /**
     *main frame of the input gui.
     *Contains all other frames
     */
    private JFrame m_frame;
    
    /**
     *tabbed panel which the user navigates through to choose all the options
     */
    private JTabbedPane m_tabbed;
    
    //buttons for navigating between tabs
    /**
     *"next" button for navigating between tabs
     */
    private JButton m_nextButton;
    
    /**
     *"back" button for navigating between tabs
     */
    private JButton m_backButton;
    
    /**
     *"help" button for invoking mview help
     */
    private JButton m_helpButton;
    /**
     *"Go" button for initiating the calculation and display
     */
    private JButton m_goButton;
    
    private JButton m_closeButton;
    
    /**
     *A quickset window used for setting options of
     *multiple measurements at once.
     *Invoked by a quickset button on tab 3 option pane
     */
    private QCQuicksetWindow m_quicksetWindow;
    
    /**
     *List of measurements, which the user can select measurements from.
     *This list will be displayed in the first tab of the frame.
     */
    DragAndDropList m_measList;
    
    /**
     *Contains a list of the names of replicate groups.
     *These are displayed in the second tab for editing purposes.
     *In the third tab, where measurements are assigned to
     *replicate groups, replicate groups will be
     *chosen from this list
     */
    DefaultListModel m_repSets;
    
    /**
     *List of groups, which the user can add to or delete from.
     *This list will be displayed in the second tab, and
     *forms a view on m_repSets
     */
    DragAndDropList m_groupList;
    
    /**
     *Options panel with one line of options per measurement selected in tab 1.
     *Choosable options are replicate groups, as specified in tab 2
     *This panel will be displayed in the third panel
     */
    JPanel m_measOptionsPanel;
    
    
    /**
     *Contains info (dye flip, replicate group, data) about each measurement which the
     *user has already selected. Also contains refs to the corresponding GUI check box and combo box.
     *These are displayed in the m_measOptionsPanel.
     */
    QCMeasurementStruct[] m_measurementStructs;
    
    /**
     *median radioButton
     */
    private JRadioButton m_medianButton;
    private JRadioButton m_meanButton;
    
    /**
     *filter checkbox. When selected, the calculation
     *will be carried out excluding any spots which have been filtered out in the
     *main maxd window.
     */
    private JCheckBox m_applyFilterCheckbox;
    
    /**text field where the user can specify the significance level
     *for which the LCL, CL and UCL will be calculated
     */
    private JTextField m_sig_main_textfield;
    /**text field where the user can specify the mu significance level
     *with respect to which the confidence interval of the CL will be calculated
     */
    private JTextField m_sig_mu_textfield;
    /**text field where the user can specify the sigma significance level
     *with respect to which the confidence intervals of the UCL and LCL will be calculated
     */
    private JTextField m_sig_sigma_textfield;
    
    
    //fields for remembering within session or between sessions
    //via maxdview properties
    
    /**
     *In the Groups panel, the user can choose to save
     *a list of replicate group names.
     *The user can also reimport names from such a file.
     *The m_groupFilePath is the default or most recently used
     *directory for keeping group these files in
     */
    private File m_groupFilePath;
    
    /**
     *These are hashmaps for keeping a
     *memory of what the user has already specified
     *as the Group and orientation (forward reverse)
     *of a measurement.
     *When the list of measurements GUI is updated
     *e.g. due to reselection in the 1st pane,
     *these are remembered so they can be reassigned.
     *This is not the place to keep them for querying:
     *one should query the m_measurementStructs directly to get that info
     */
    private HashMap m_measGroupHash;
    
    private HashMap m_measOrientHash;
    
    
    
    
    /** Creates a new instance of QCChart */
    public QCChart(maxdView mview_)  {
        //set up plugin's references to the parent maxdview and data
        mview = mview_;
        edata = mview_.getExprData();
        
        
        //set up hashs for short term memory of which measurements
        //have been assigned to which groups,
        //and whether they are forward or reverse
        m_measGroupHash = new HashMap();
        m_measOrientHash = new HashMap();
        
        //set up the storage for the names of the groups and fill them with
        //anything from before
        m_repSets = new DefaultListModel();
        
        boolean prevGroupsSet = mview.getBooleanProperty("QCChart.prevGroupsWereSet",  false);
        if (prevGroupsSet) {
            int numGroups = mview.getIntProperty("QCChart.numGroups",0);
            for (int i = 0 ; i < numGroups ; i++) {
                m_repSets.addElement(mview.getProperty("QCChart.group"+i,""));
            }
        } else {
            resetGroup();
        }
        
        
    }
    
    //BEGIN PLUGIN METHODS
    //does nothing at present.
    //?TO Do add commands for calculating the values and ? writing results to a file.
    public PluginCommand[] getPluginCommands() {
        return null;
    }
    
    public PluginInfo getPluginInfo() {
        PluginInfo pinf = new PluginInfo("QCChart",
        "viewer",
        "Quality Control Chart",
        "Reports Quality Control info as developed by Yongxiang Fang<BR>" +
        "Plugin written by hhulme Manchester Bioinformatics 2004<BR>" +
        "Requires the Colt plugin from CERN", 
        1, 1, 0);
        return pinf;
    }
    
    // Does nothing ?TO Do add commands for calculating the values and ? writing results to a file.
    public void runCommand(String str, String[] str1, CommandSignal commandSignal) {
    }
    
    
    /**
     *starts the plugin
     */
    public void startPlugin() {
        
        //following is to set up colt jar.
        //lets hope its there

        maxdView.CustomClassLoader ccl = (maxdView.CustomClassLoader) getClass().getClassLoader();
        
        String default_jar_file_location = 
	    mview.getTopDirectory() + 
	    java.io.File.separatorChar + 
	    "external" +
	    java.io.File.separatorChar + 
	    "colt" +
	    java.io.File.separatorChar + 
	    java.io.File.separatorChar + "colt.jar";

        colt_jar_file_path = mview.getProperty("QCChart.colt_jar_path", default_jar_file_location );

        mview.setMessage("Initialising Colt classes");
        Thread.yield();
        
        boolean found = false;
        while(!found) {
            if( colt_jar_file_path != null ) {
                try {
                    ccl.addPath( colt_jar_file_path );
                }
                catch( java.net.MalformedURLException murle ) {
                    String msg =
                    "Unable to load the Colt JAR file from '" + colt_jar_file_path + "'\n" +
                    "\nPress \"Find\" to specify an alternate location for the file,\n" +
                    "  or\nPress \"Cancel\" to stop the plugin.\n" +
                    "\n(see the help page for more information)\n";
                    
                    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
                        return;
                    
                    try {
                        colt_jar_file_path = mview.getFile("Location of 'colt.jar'", colt_jar_file_path );
                    }
                    catch( UserInputCancelled uic ) {
                        return;
                    }
                }
            }
            
            Class wc = ccl.findClass("cern.jet.stat.Gamma");
            found = (wc != null);
            if(!found) {
                try {
                    String msg = "Unable to find the Colt JAR file\n";
                    msg += (colt_jar_file_path == null)  ? "\n" : ("in '" + colt_jar_file_path + "'\n");
                    msg += "\nPress \"Find\" to specify the location of the file,\n" +
                    "  or\nPress \"Cancel\" to stop the plugin.\n" +
                    "\n(see the help page for more information)\n";
                    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
                        return;
                    colt_jar_file_path = mview.getFile("Location of 'colt.jar'", colt_jar_file_path);
                    
                }
                catch(UserInputCancelled uic) {
                    // don't start the plugin
                    return;
                }
            }
            else {
                mview.putProperty("QCChart.colt_jar_path", colt_jar_file_path);
            }
        }
        
        // preload the required classes
        
        Class probablilityQC = ccl.findClass("cern.jet.stat.Probability");
        Class gammaQC = ccl.findClass("cern.jet.stat.Gamma");
        //end of gubbins to get colt
        
        //set up the GUI
        addComponents();
        //add a data observer, so that measurement list is updated when measurements change
        edata.addObserver(this);
        //align everything and make visible
        m_frame.pack();
        m_frame.setVisible(true);
    }
    
    /**
     *ends the plugin
     */
    public void stopPlugin() {
        cleanup();
    }
    //END PLUGIN METHODS
    
    //BEGIN EXPRDATAOBSERVER METHODS
    
    public void clusterUpdate(ExprData.ClusterUpdateEvent cue) {
    }
    
    public void dataUpdate(ExprData.DataUpdateEvent due) {
        
    }
    
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue) {
    }
    
    /**
     *If measurements in maxdview change in any way,
     *make a new model for looking at those measurements
     */
    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue) {
        m_measList.setModel(new QCChart.MeasListModel());
    }
    
    //END EXPRDATAOBSERVERMETHODS
    
    
    //START GUI methods
    /**
     *Builds the QC Chart input user interface
     *which is a frame with 4 tabs.
     *Tab 1 is for choosing the measurements
     *Tab 2 is for specifying the names etc of the replicate groups.
     *Tab 3 is for pairing these up, i.e. assigning each chosen measurement to a particular replicate group
     *and specifying if it is forward or reverse.
     *Tab 4 is for specifying the significance levels in order to calculate both the
     *Control lines (Upper, Center and Lower) and the confidence intervals of those control lines
     */
    private void addComponents() {
        
        //outer panel
        JPanel outer_panel;
        GridBagConstraints outer_constraints;
        
        //button panel for navigating between tabs
        JPanel button_panel;
        GridBagConstraints button_constraints;
        
        //tab 1 for picking measurements
        DragAndDropPanel meas_pick_panel;
        GridBagConstraints meas_pick_constraints;
        
        //scroll to surround the list of measurements for picking
        JScrollPane scrollList;
        
        //tab 2 for managing group names
        DragAndDropPanel group_panel;
        GridBagLayout group_gridbag;
        GridBagConstraints group_constraints;
        JScrollPane groupScrollList;
        
        //buttons for managing the names of groups of replicates in tab 2, and panel to keep them in
        JButton add_group_button;
        JButton rename_group_button;
        JButton delete_group_button;
        JButton import_groupnames_button;
        JButton save_groupnames_button;
        JButton reset_groupnames_button;
        
        JPanel group_buttons_panel;
        
        //tab 3 for specifying options about measurements
        JPanel optionsPanel;
        GridBagConstraints options_Constrants;
        JPanel optionsButtonPanel;
        JButton quickSetButton;
        
        //scroll to surround list of measurments that have been picked and their
        //options in tab 3
        JScrollPane measOpScroll;
        
        //tab 4 for choosing the significance levels
        JPanel sigPanel;
        GridBagConstraints sig_constraints;
        JTextArea sig_main_text;
        JTextArea sig_mu_text;
        JTextArea sig_sigma_text;
        
        //main frame
        m_frame = new JFrame(this.FRAME_TITLE);
        mview.decorateFrame(m_frame);
        m_frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
        //outer frame,which will be added to the main frame
        //an outer panel
        outer_panel = new JPanel();
        outer_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        outer_panel.setLayout(new GridBagLayout());
        
        outer_panel.setMinimumSize(new Dimension(640, 580));
        outer_panel.setPreferredSize(new Dimension(640, 580));
        
        //tabbed panel, which will be added to the outer frame
        //tabbed panel
        m_tabbed = new JTabbedPane();
        m_tabbed.setEnabled(false);
        
        outer_constraints = new GridBagConstraints();
        outer_constraints.gridx = 0;
        outer_constraints.gridy = 0;
        outer_constraints.weightx = 10.0;
        outer_constraints.weighty = 9.0;
        outer_constraints.fill = GridBagConstraints.BOTH;
        outer_panel.add(m_tabbed, outer_constraints);
        
        //measurement picking panel for tab 1
        {
            //set up the GUI
            meas_pick_panel = new DragAndDropPanel();
            meas_pick_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
           
            meas_pick_panel.setLayout(new GridBagLayout());
            
            //get the list to look at the measurements from maxd
            m_measList = new DragAndDropList();
            scrollList = new JScrollPane(m_measList);
            m_measList.setModel(new QCChart.MeasListModel());
            m_measList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            
            //layout stuff
            int line = 0;
            meas_pick_constraints = new GridBagConstraints();
            meas_pick_constraints.gridx = 0;
            meas_pick_constraints.gridy = line;
            meas_pick_constraints.gridwidth = 2;
            meas_pick_constraints.weightx = 8.0;
            meas_pick_constraints.weighty = 8.0;
            meas_pick_constraints.fill = GridBagConstraints.BOTH;
            meas_pick_panel.add(scrollList, meas_pick_constraints);
            
            //add the measurements stuff to be tab 1 of the tabbed pane
            m_tabbed.add(" Measurements ", meas_pick_panel);
        }
        
        //groups control panel which is in tab 2
        {
            //GUI stuff and layout
            group_panel = new DragAndDropPanel();
            group_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            group_gridbag = new GridBagLayout();
            group_panel.setLayout(group_gridbag);
            group_constraints = new GridBagConstraints();
            
            //set up the list of groups to look at m_repSets via a GroupModel
            m_groupList = new DragAndDropList();
            m_groupList.setModel(new GroupModel());
            m_groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            groupScrollList = new JScrollPane(m_groupList);
            groupScrollList.setMinimumSize(new Dimension(300,300));
            groupScrollList.setSize(300,300);
            groupScrollList.setPreferredSize(new Dimension(300,300));
            
            //Make the buttons and a button panel for manipulating group names in tab 2
            group_buttons_panel = new JPanel();
            add_group_button = new JButton("Add");
            add_group_button.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    rememberMeasPanelOptions();
                    addGroup();
                    remindMeasPanelOptions();
                }
            });
            rename_group_button = new JButton("Rename");
            rename_group_button.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    rememberMeasPanelOptions();
                    renameGroup();
                    remindMeasPanelOptions();
                }
            });
            delete_group_button = new JButton("Delete");
            delete_group_button.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    rememberMeasPanelOptions();
                    deleteGroup();
                    remindMeasPanelOptions();
                }
            });
            import_groupnames_button = new JButton("Import");
            import_groupnames_button.setToolTipText("Import a list of group names from a file");
            import_groupnames_button.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    rememberMeasPanelOptions();
                    importGroup();
                    remindMeasPanelOptions();
                }
            });
            save_groupnames_button = new JButton("Save");
            save_groupnames_button.setToolTipText("Save the current list of group names to a file");
            save_groupnames_button.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveGroup();
                }
            });
            reset_groupnames_button = new JButton("Reset Groups to \"A...J\"");
            reset_groupnames_button.setToolTipText("reset the group names");
            reset_groupnames_button.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    rememberMeasPanelOptions();
                    resetGroup();
                    remindMeasPanelOptions();
                }
            });
            
            //add the buttons to a button panel
            group_buttons_panel.add(add_group_button);
            group_buttons_panel.add(delete_group_button);
            group_buttons_panel.add(rename_group_button);
            group_buttons_panel.add(save_groupnames_button);
            group_buttons_panel.add(import_groupnames_button);
            group_buttons_panel.add(reset_groupnames_button);
            
            //layout stuff for within tab 2
            
            group_constraints.gridx = 0;
            group_constraints.gridy = 0;
            group_constraints.gridwidth = 2;
            group_constraints.gridheight = 8;
            group_constraints.weightx = 8.0;
            group_constraints.weighty = 8.0;
            group_constraints.fill = GridBagConstraints.BOTH;
            group_constraints.anchor = GridBagConstraints.CENTER;
            groupScrollList.setSize(100,100);
            group_gridbag.setConstraints(groupScrollList, group_constraints);
            group_panel.add(groupScrollList);
            
            //add the button panel to the panel
            
            group_constraints.gridy = 9;
            group_constraints.weighty = 1.0;
            group_constraints.gridheight = 1;
            group_constraints.fill = GridBagConstraints.BOTH;
            group_gridbag.setConstraints(group_buttons_panel, group_constraints);
            group_panel.add(group_buttons_panel);
            
            
            //add the group panel as the next tab i.e. tab 2
            m_tabbed.add(" Groups ", group_panel);
        }
        
        //options panel - this is tab 3 for assigning each chosen measurement
        //to one of the replicate groups and picking forward or reverse
        {
            //GUI and layout
            //options panel is the outer pane containg the
            //m_measOptionsPanel and the panel of buttons
            optionsPanel = new JPanel();
 
            optionsPanel.setLayout(new GridBagLayout());
            options_Constrants = new GridBagConstraints();
            
            //This contains the Gui from each measurement struct
            m_measOptionsPanel = new JPanel();
            m_measOptionsPanel.setLayout(new GridBagLayout());
            measOpScroll = new JScrollPane(m_measOptionsPanel);
            
            //this panel contains the buttons - quickset
            optionsButtonPanel = new JPanel();
            quickSetButton = new JButton("Quick Set");
            
            //build a quickset window, which is initially hidden.
            m_quicksetWindow = new QCQuicksetWindow(this);
            
            //assign action to the quikset button
            quickSetButton.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    m_quicksetWindow.show();
                }
            });
            
           
            
            optionsButtonPanel.add(quickSetButton);
            
            //layout stuff
            options_Constrants.gridx = 0;
            options_Constrants.gridy = 1;
            options_Constrants.weightx = 1;
            options_Constrants.weighty = 10;
            options_Constrants.gridwidth = 2;
            options_Constrants.fill = GridBagConstraints.BOTH;
            //options_gridbag.setConstraints(measOpScroll, options_Constrants);
            optionsPanel.add(measOpScroll,options_Constrants);
            options_Constrants.gridy = 0;
            options_Constrants.weighty = 1;
            optionsPanel.add(optionsButtonPanel, options_Constrants);
            
            // this adds the appropriate measurement structs
            //including combo box and radio buttons, to the panel
            updateMeasurementsOptionPanel();
            remindMeasPanelOptions();
            
            /*add a listener so that when the measurements change
             *A)the panel will get updated
             *B) the quickset window will display the new selected measurements
             */
            m_measList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    //some of the measurements may have had options set.
                    //remember them so that they don't get cleared and so
                    //the user doesn't have to type them again
                    rememberMeasPanelOptions();
                    //update the measurements panel -
                    //the remembered versions will be set if they exist
                    // from within updateMeasurementsOptionPanel()
                    updateMeasurementsOptionPanel();
                    remindMeasPanelOptions();
                    if (m_quicksetWindow!=null) {
                        //update the model for the quickset window
                        m_quicksetWindow.resetModel(new SelectedMeasModel());
                    }
                }
            });
            
           
            //add the options panel to tab 3 of the tabbed pane
            m_tabbed.add(" Measurements to Groups ", optionsPanel);
        }
        
        //tab 4 where the user specified the required significance levels
        {
            sigPanel = new JPanel(new GridBagLayout());
            m_sig_main_textfield = new JTextField(padDoubleString(DEFAULT_SIG_LEVEL, 8));
            m_sig_mu_textfield = new JTextField(padDoubleString(DEFAULT_CONFIDENCE_LEVEL, 8));
            m_sig_sigma_textfield = new JTextField(padDoubleString(DEFAULT_CONFIDENCE_LEVEL, 8));
            
            sig_main_text = new JTextArea(getSigMessage(SIG_MESSAGE_MAIN), 8, 30);
            sig_main_text.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            sig_main_text.setBackground(sigPanel.getBackground());
            
            sig_mu_text = new JTextArea(getSigMessage(SIG_MESSAGE_MU), 8, 30);
            sig_mu_text.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            sig_mu_text.setBackground(sigPanel.getBackground());
            
            sig_sigma_text = new JTextArea(getSigMessage(SIG_MESSAGE_SIG), 8, 30);
            sig_sigma_text.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            sig_sigma_text.setBackground(sigPanel.getBackground());
            
              //checkbox where the user can specify whether filter should be applied
            m_applyFilterCheckbox = new JCheckBox("Apply Filter");
            //option remembered by mview.
            m_applyFilterCheckbox.setSelected(mview.getBooleanProperty("QCChart.applyFilter", false));
            
            m_medianButton = new JRadioButton("Use Median (Reccomended)", true);
            m_meanButton = new JRadioButton("Mean", false);
            
            ButtonGroup meanMedianButtonGroup = new ButtonGroup();
            meanMedianButtonGroup.add(m_medianButton);
            meanMedianButtonGroup.add(m_medianButton);
            
            JPanel meanMedianButtonPanel = new JPanel();
            meanMedianButtonPanel.add(m_medianButton);
            meanMedianButtonPanel.add(m_meanButton);
            
            JTextArea mean_median_text = new JTextArea("Choose whether to use the mean or median\nin the calculation.\n(Median is recommended, for robustness)\n", 8, 30);
            mean_median_text.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            mean_median_text.setBackground(sigPanel.getBackground());
            
            
            
            sig_constraints = new GridBagConstraints();
            sig_constraints.anchor = GridBagConstraints.NORTHWEST;
            sig_constraints.gridx = 0;
            sig_constraints.gridy = 0;
            
            sigPanel.add(m_sig_main_textfield, sig_constraints);
            sig_constraints.gridy++;
            sigPanel.add(m_sig_mu_textfield, sig_constraints);
            sig_constraints.gridy++;
            sigPanel.add(m_sig_sigma_textfield, sig_constraints);
            sig_constraints.gridy++;
            sigPanel.add(meanMedianButtonPanel, sig_constraints);
            sig_constraints.gridy++;
            sigPanel.add(m_applyFilterCheckbox, sig_constraints);
            
            sig_constraints.gridx = 1;
            sig_constraints.gridy=0;
            sigPanel.add(sig_main_text, sig_constraints);
            sig_constraints.gridy++;
            sigPanel.add(sig_mu_text, sig_constraints);
            sig_constraints.gridy++;
            sigPanel.add(sig_sigma_text, sig_constraints);
            sig_constraints.gridy++;
            //sigPanel.add(mean_median_text, sig_constraints);
            
            

            
            
            
            
            
            
            
            
            m_tabbed.add(" Sig Levels ", sigPanel);
        }
        
        
        //buttons for navigating between tabs
        m_nextButton = new JButton("Next");
        m_backButton = new JButton("Back");
        m_goButton = new JButton("Go!");
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
        
        m_goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doStuff();
            }
        });
        
        m_helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mview.getPluginHelpTopic("QCChart", "QCChart");
            }
        });
        
        m_closeButton = new JButton("Close");
	m_closeButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    cleanup();
		    m_frame.setVisible( false );
		}
	    });
        
        //stuff for controlling the buttons
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
        //kickstart the tab stuff:
        if ( m_tabbed.getSelectedIndex() == m_tabbed.getTabCount()-1 ) {
            m_nextButton.setEnabled(false);
        }
        if ( m_tabbed.getSelectedIndex() == 0) {
            m_backButton.setEnabled(false);
        }
        
        //add the buttons to the button panel
        button_panel = new JPanel();
        
        button_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        button_panel.setLayout(new GridBagLayout());
        
        button_constraints = new GridBagConstraints();
        button_constraints.gridx = 0;
        button_constraints.gridy = 0;
        button_constraints.weightx = 1.0;
        button_constraints.weighty = 1.0;
        
 
        button_panel.add(m_backButton, button_constraints);
        button_constraints.gridx++;
  //      button_gridbag.setConstraints(m_nextButton, button_constraints);
        button_panel.add(m_nextButton, button_constraints);
        button_constraints.gridx++;
     //   button_gridbag.setConstraints(m_goButton, button_constraints);
        button_panel.add(m_goButton, button_constraints);
        button_constraints.gridx++;
//        button_gridbag.setConstraints(m_helpButton, button_constraints);
        button_panel.add(m_helpButton, button_constraints);
        button_constraints.gridx++;

        button_panel.add(m_closeButton, button_constraints);
        
        //add the button panel to the outer panel
        
        outer_constraints.gridy++;
        outer_constraints.weighty=1.0;
       
        outer_panel.add(button_panel, outer_constraints);
        
        //add the outer panel to the main panel
        
        m_frame.getContentPane().add(outer_panel);
        
    }
    
    /**OK, this is a rubbish place to keep this function.
     *returns a string representation of a double which is 
     *padded to the right amount with spaces, for use in 
     *the text fields for significance levels
     */
    public static String padDoubleString(double db, int n) {
        StringBuffer buf = new StringBuffer(Double.toString(db));
        int len = buf.toString().length();
        for (int i = len ; i < n ; i++) {
            buf.append(" ");
        }
        return buf.toString();
    }
    
    /**
     *@return true if 0<db<1, false otherwise
     */
    public static boolean inUnitInterval(double db) {
        if (db>0 && db<1) {
            return true;
        } 
        return false;
    }
    
    /**
     *get a string explaining what the significance levels are / mean
     */
    public static String getSigMessage(int i) {
        StringBuffer buf=new StringBuffer();
        if (i == SIG_MESSAGE_MAIN) {
            buf.append("Choose significance level.\n");
            buf.append("This will be used to calculate the \n");
            buf.append("Upper and Lower Control Lines.\n");
            buf.append("The significance value must be in the range (0,1)\n");
            buf.append("and should typically be close to zero.\n");
            buf.append("Sensible values would be: e.g. 0.01, 0.05 or 0.0027\n");
            buf.append("Note that 0.0027 corresponds to three x stdev\n ");
        } else if (i == SIG_MESSAGE_MU) {
            buf.append("Choose the significance level for the mean estimate.\n");
            buf.append("This will be used to calculate the Confidence interval\nfor the Central Control Line\n");
            buf.append("This value must be in the range (0,1) and should\ntypically be set at around 0.2\n ");
        } else if (i == SIG_MESSAGE_SIG) {
            buf.append("Choose the significance level for the stdev estimate.\n");
            buf.append("This will be used to calculate the Confidence intervals\nfor the Upper and Lower Control Lines\n");
            buf.append("This value must be in the range (0,1) and should\ntypically be set at around 0.2\n ");
        }
        return buf.toString();
    }
    
    //methods for adding, deleting etc of groups
    /**
     *Adds a new replicate group name
     */
    public void addGroup() {
        String newGroup;
        newGroup = JOptionPane.showInputDialog("New Replicate Set Name");
        
        if (newGroup==null) {
            return;
        }
        newGroup = newGroup.trim();
        if (newGroup.length()>0 && !m_repSets.contains(newGroup)) {
            m_repSets.addElement(newGroup);
            m_groupList.updateUI();
            if (m_quicksetWindow!=null) {
                m_quicksetWindow.updateGroupNames();
            }
        }
    }
    
    /**Deletes the selected group name
     */
    public void deleteGroup() {
        String deletionGroup = (String) m_groupList.getSelectedValue();
        if (deletionGroup == null) {
            return;
        }
        int index = m_repSets.indexOf(deletionGroup);
        m_repSets.remove(index);
        m_groupList.updateUI();
        if (m_quicksetWindow!=null) {
                m_quicksetWindow.updateGroupNames();
            }
    }
    
    /**Resets the list of groups to A through J
     */
    public void resetGroup() {
        m_repSets.removeAllElements();
        m_repSets.addElement("A");
        m_repSets.addElement("B");
        m_repSets.addElement("C");
        m_repSets.addElement("D");
        m_repSets.addElement("E");
        m_repSets.addElement("F");
        m_repSets.addElement("G");
        m_repSets.addElement("H");
        m_repSets.addElement("I");
        m_repSets.addElement("J");
        if (m_groupList!=null) {
            m_groupList.updateUI();
        }
        if (m_quicksetWindow!=null) {
                m_quicksetWindow.updateGroupNames();
            }
    }
    
    public void renameGroup() {
        String groupToRename;
        String newName;
        
        groupToRename = (String) m_groupList.getSelectedValue();
        if (groupToRename == null) {
            return;
        }
        newName = JOptionPane.showInputDialog("New Replicate Set Name");
        
        if (newName == null) {
            return;
        }
        
        newName = newName.trim();
        int index = m_repSets.indexOf(groupToRename);
        if (newName.length()>0 && !m_repSets.contains(newName) && index>-1) {
            
            m_repSets.set(index, newName);
            m_groupList.updateUI();
            if (m_quicksetWindow!=null) {
                m_quicksetWindow.updateGroupNames();
            }
            
        }
        
    }
    
    /**Import a list of replicate names from an EOL
     *sepearted file
     */
    private void importGroup() {
        //directory path to use
        String path = mview.getProperty("QCChart.groupFilePath","");
        JFileChooser chooser;
        File file;
        
        chooser = new JFileChooser();
        if (m_groupFilePath !=null) {
            chooser.setCurrentDirectory(m_groupFilePath);
        } else if (!path.equalsIgnoreCase("")) {
            chooser.setCurrentDirectory(new File(path));
        }
        
        
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                m_repSets.removeAllElements();
                while (true) {
                    String line=reader.readLine();
                    if (line == null) {
                        break;
                    } else {
                        line = line.trim();
                        if (!line.equalsIgnoreCase("")) {
                            m_repSets.addElement(line);
                        }
                    }
                }
                reader.close();
                
            } catch (IOException iox) {
                System.out.println("[QCChart] Problem reading from file");
                iox.printStackTrace();
            }
            m_groupList.updateUI();
            if (m_quicksetWindow!=null) {
                m_quicksetWindow.updateGroupNames();
            }
            m_groupFilePath = chooser.getCurrentDirectory();
        }
    }
    
    /**Writes the group names to a file,
     *as an Ene-Of-Line seperated list
     */
    private void saveGroup() {
        
        String path = mview.getProperty("QCChart.groupFilePath","");
        JFileChooser chooser;
        File file;
        
        chooser = new JFileChooser();
        if (m_groupFilePath !=null) {
            chooser.setCurrentDirectory(m_groupFilePath);
        } else if (!path.equalsIgnoreCase("")) {
            chooser.setCurrentDirectory(new File(path));
        }
        
        
        int returnValue = chooser.showSaveDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            try {
                FileWriter writer = new FileWriter(file);
                int numGroups = m_repSets.getSize();
                for (int i = 0 ; i < numGroups ; i++) {
                    writer.write((String) m_repSets.getElementAt(i));
                    writer.write("\n");
                }
                writer.close();
            } catch (IOException iox) {
                System.out.println("[QCChart] Problem writing to file");
                iox.printStackTrace();
            }
            m_groupFilePath = chooser.getCurrentDirectory();
        }
    }
    
    //methods for going between tabs on the user input GUI
    /**
     *moves to previous tab
     */
    private void gotoPrevTab() {
        int index = m_tabbed.getSelectedIndex();
        if (index>0) {
            m_tabbed.setSelectedIndex(index-1);
        }
    }
    
    /** moves to next tab*/
    private void gotoNextTab() {
        int index = m_tabbed.getSelectedIndex();
        int maxIndex = m_tabbed.getTabCount();
        if (index < maxIndex-1) {
            m_tabbed.setSelectedIndex(index+1);
        }
        
    }
    
    /**cleans up and saves anything that should be remembered
     *as mview properties
     */
    private void cleanup() {
        //remove data observer and set to invisible
        edata.removeObserver(this);
        if (m_frame !=null) {
            m_frame.setVisible(false);
        }
        
        //remember if the filter property
        mview.putBooleanProperty("QCChart.applyFilter", m_applyFilterCheckbox.isSelected());
        
        //remember the names of the replicate groups
        mview.putBooleanProperty("QCChart.prevGroupsWereSet", true);
        int numGroups = m_repSets.getSize();
        mview.putIntProperty("QCChart.numGroups", numGroups);
        for (int i = 0 ; i < numGroups ; i++) {
            mview.putProperty("QCChart.group"+i, (String) m_repSets.getElementAt(i));
        }
        
        //remember any measurement-to-replicate-group assignments and measurement-to-orientation assignments
        for (int i = 0 ; i < m_measurementStructs.length ; i++) {
            String measname = m_measurementStructs[i].getName();
            String groupname = (String) m_measurementStructs[i].getComboBox().getSelectedItem();
            boolean isforward = m_measurementStructs[i].isForward();
            if (groupname!=null) {
                mview.putProperty("QCChart.measurementgroup."+measname, groupname);
            }
            mview.putBooleanProperty("QCChart.measurementfwd."+ measname, isforward);
        }
        //remember the directory where group files are kept
        if (m_groupFilePath!=null) {
            mview.putProperty("QCChart.groupFilePath", m_groupFilePath.getPath());
        }
    }
    
    /**the measurementOptionsPanel is part of the third tab should contain
    //one line per measurement selected in the first tab.
    //For each measurement, the user can select:
    //which replicate set the measurement belongs to
    //and whether the measurement is forward or reverse
    //(I.e. if its a dye flip or not).
    //This method updates the measurementOptionPanel, depending on
    //which measurements are selected in the first panel.
     */
    private void updateMeasurementsOptionPanel() {
        m_measOptionsPanel.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        
        
        Object[] measList = m_measList.getSelectedValues();
        m_measurementStructs = new QCMeasurementStruct[measList.length];
        for (int i = 0 ; i < measList.length ; i++) {
            
            QCMeasurementStruct mstruc = new QCMeasurementStruct((String) measList[i], m_repSets);
            m_measurementStructs[i]  = mstruc;
            constraints.gridy = i;
            constraints.gridx = 0;
            m_measOptionsPanel.add(new JLabel(mstruc.getName()), constraints);
            constraints.gridx = 1;
            m_measOptionsPanel.add(mstruc.getComboBox(), constraints);
            constraints.gridx = 2;
            m_measOptionsPanel.add(mstruc.getButtonPanel(), constraints);
        }
    }
    
    
    /**
     *Restore remembered values
     */
    private void remindMeasPanelOptions() {
           for (int i = 0 ; i < m_measurementStructs.length ; i++) {
            
               QCMeasurementStruct mstruc = m_measurementStructs[i];
            
            /*try to select the right thing in the combo box using the following
             *priority:
             *1 select the group that was most recently selected for that measurement,
             *if that group exists
             *2 select the group that has been stored for that measurement as a mview
             *property, if that group exists
             *select the first thing in the combo box is the model has 1 or more thing in it
             */
            String remValue = (String) m_measGroupHash.get(mstruc.getName());
            if (remValue == null) {
                remValue = mview.getProperty("QCChart.measurementgroup."+mstruc.getName(),  null);
            }
            boolean successfullySet = false;
            if (remValue!=null) {
                try {
                    mstruc.getComboBox().setSelectedItem(remValue);
                    successfullySet = true;
                } catch (Exception ex) {//maybe that group no longer exists
                    mstruc.getComboBox().setSelectedItem(null);
                    successfullySet = false;
                }
            }
            if ((!successfullySet) && m_repSets.size()>0) {
                mstruc.getComboBox().setSelectedIndex(0);
            }
            
            Boolean forValue = (Boolean) m_measOrientHash.get(mstruc.getName());
            if (forValue == null) {
                forValue = new Boolean(mview.getBooleanProperty("QCChart.measurementfwd."+mstruc.getName(), true));
            }
            
            if (forValue.booleanValue()) {
                mstruc.selectForward();
            } else {
                mstruc.selectReverse();
            }
        }
    }
    
    
    /**
     *remembers any meaurement-to-replicate-group assignments which have already been made.
     *The measurement panel may be about to be updated, so these values are remembered so that
     *the user will not have to assign them again from scratch.
     *The "forward / reverse" selections are also remembered
     */
    private void rememberMeasPanelOptions() {
        if (m_measurementStructs!=null) {
            //m_measGroupHash.clear();
            //m_measOrientHash.clear();
            int len = m_measurementStructs.length;
            for (int i = 0 ; i<len ; i++) {
                QCMeasurementStruct measStruct = m_measurementStructs[i];
                m_measGroupHash.put(measStruct.getName(), (String) measStruct.getComboBox().getSelectedItem());
                m_measOrientHash.put(measStruct.getName(), new Boolean(measStruct.isForward()));
            }
        }
    }
    
    
    
    /**
     *Looks what measurements have been selected by the user,
     *Looks what options have been chosem
     *does the calculation and displays the results.
     */
    private void doStuff() {
        
        QCInput[] inputs;
        
        //detect the column names etc and build the input object
        String colName;
        double data[];
        boolean isForward;
        String group;
        
        //check there really are some measurements - if not, warning message and abandon calculation
        
        int len = m_measurementStructs.length;
        
        if (len == 0) {
            JOptionPane.showMessageDialog(m_frame, "First you need to pick some measurements", "No measurements were selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
         //check that OK sig levels have been chosen
        double primary_sigLevel = 0;
        double confidence_mu_siglevel = 0;
        double confidence_sig_siglevel = 0;
        try {
            primary_sigLevel = new Double(m_sig_main_textfield.getText().trim()).doubleValue();
            confidence_mu_siglevel = new Double(m_sig_mu_textfield.getText().trim()).doubleValue();
            confidence_sig_siglevel = new Double(m_sig_sigma_textfield.getText().trim()).doubleValue();
        } catch (Exception ex) {
            //there may be some exception from e.g. string that's not a double
            //don't do anything - rely on fact that the 0 sig levels won't pass the test
        }
        if (!inUnitInterval(primary_sigLevel) || !inUnitInterval(confidence_sig_siglevel) || !inUnitInterval(confidence_mu_siglevel)) {
            JOptionPane.showMessageDialog(m_frame, "All sig values must be in the range (0,1)", "Sig Values incorrectly set", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        
        inputs = new QCInput[len];
        
        for (int i = 0 ; i < len ; i++) {
            colName = m_measurementStructs[i].getName();
            
            
            data = edata.getMeasurementData(colName);
            isForward = m_measurementStructs[i].isForward();
            group = (String) m_measurementStructs[i].getComboBox().getSelectedItem();
            
            inputs[i] = new QCInput(colName, data, isForward, group);
        }
        
        //decide whether to use mean or median
        
        int useMeanOrMedian = QCChartCalculator.USE_MEDIAN_FOR_CALCULATION; //default
        
        if (m_meanButton.isSelected()) {
            useMeanOrMedian = QCChartCalculator.USE_MEAN_FOR_CALCULATION;
            
        }
            
        
        //collect any filter info
        int numSpots = edata.getNumSpots();
        boolean[] filterInfo = new boolean[numSpots];
        if (m_applyFilterCheckbox.isSelected()) {
            for (int i = 0 ; i < numSpots; i++) {
                filterInfo[i] = edata.filter(i);
            }
        } else {
            for (int i = 0 ; i < numSpots ; i++) {
                filterInfo[i] = false;
            }
        }
        
        //Now do the calculation:
        
        QCChartCalculator calc = new QCChartCalculator();
      
        
       
        QCResults results = calc.calculateQCChart(inputs, filterInfo, primary_sigLevel, confidence_mu_siglevel, confidence_sig_siglevel, useMeanOrMedian);
        
        int numBadRows = results.getNumbadRows();
        //System.out.println("num bad rows = " + numBadRows);
        
        String message = "QC Calculations carried out. See table and graph.\n";
        if (numBadRows == 1) {
            message = message + " Note that " + numBadRows + " row contained Inf or NaN and was excluded from the calculation";
        } else if (numBadRows > 1) {
            message = message + " Note that " + numBadRows + " rows contained Inf or NaN and were excluded from the calculation";
        }
        
        mview.infoMessage( message );
        //Now display the results in a new window
        
        QCResultsTableDisplay displayTable = new QCResultsTableDisplay(results, mview);
        displayTable.setVisible(true);
        
        QCResultsGraphDisplay displayPanel = new QCResultsGraphDisplay(results, mview);
        
    }
    
    public ComboListModel getCLMOfRepSets() {
        return new ComboListModel(m_repSets);
    }
    
    protected QCMeasurementStruct[] getMeasurementStructs() {
        return m_measurementStructs;
    }
    
    
    //END GUI methods
    
    /**List model which contains the measurements of maxd*/
    public class MeasListModel extends DefaultListModel {
        public Object getElementAt(int index) {
            return edata.getMeasurementName( edata.getMeasurementAtIndex(index) );
        }
        public int getSize() {
            return edata.getNumMeasurements();
        }
    }
    
    public class GroupModel extends DefaultListModel {
        public Object getElementAt(int index) {
            return m_repSets.get(index);
        }
        public int getSize() {
            return m_repSets.size();
        }
    }
    
    public class SelectedMeasModel extends DefaultListModel {
        public Object getElementAt(int index) {
            return m_measList.getSelectedValues()[index];
        }
        
        public int getSize() {
            return m_measList.getSelectedValues().length;
        }
    }
    

        
        
        
        

}
