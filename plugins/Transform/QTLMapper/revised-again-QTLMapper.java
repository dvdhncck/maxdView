/*
 * QTLMapper.java
 * A plugin for marking which spots are for genes within a specified QTL
 * Created on 01 March 2005, 12:48
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;

/**
 * A plugin for marking which spots are for genes within a specified QTL
 * @author Catriona Rennie
 */
public class QTLMapper implements Plugin {
    
    /**
     * Parent maxdView
     */
    private maxdView mView;
    private ExprData exprData;
    
    /**
     * JFrame to hold GUI components
     */
    private JFrame frame;
    private JTabbedPane tabPane;
    private JPanel connectPanel;
    private JPanel describePanel;
    private JPanel selectPanel;

    private String dbHost;
    private String dbPort;
    private String dbListName = "mysql";
    private String dbName;
    private String dbUser;
    private String dbPass;
    private Vector dbNames;
    private Vector speciesNames;
    
    private String qtlName;
    private HashMap speciesHash;
    private String species;
    private String chromosome;
    private String start;
    private String end;
    private String startMarker;
    private String endMarker;
    private String startType;
    private String endType;
    private final String MARKER_TYPE = "Marker";
    private final String POSITION_TYPE = "Position (in bp)";
    private String[] posTypes = {MARKER_TYPE, POSITION_TYPE};

    private Vector qtlProbes;
    
    private boolean loadedJars;
    
    private Class mysqlDriverClass;
    
    /** Creates a new instance of QTLMapper */
    public QTLMapper(maxdView mView) {
        this.mView = mView;
        exprData = mView.getExprData();
    }

    /** Not implemented */
    public void runCommand(String str, String[] str1, CommandSignal commandSignal) {
    }

    /** Not implemented */
    public PluginCommand[] getPluginCommands() {
          return null;
    }

    /**
     * Returns a set of information about the plugin
     * @return PluginInfo containing information about the plugin
     */
    public PluginInfo getPluginInfo() {
        return new PluginInfo("QTL Mapper",
                "transform",
                "Label spots to indicate inclusion in a QTL",
                "Labels spots to indicate which represent genes within a QTL, using EnsEMBL genome information. Only works for Affymetrix arrays<BR>Plugin written by Catriona Rennie, 2005<BR>", 1, 0, 0);
    }

    /**
     * Handles any clearing up after the plugin has finished
     */
    public void stopPlugin() {
        cleanUp();
    }

    /**
     * Handles initialisation etc when the plugin starts
     */
    public void startPlugin() {
        System.out.println("In startPlugin");
        
        // Initialise frame for displaying GUI components
        frame = new JFrame("QTL Mapper");
        mView.decorateFrame(frame);
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    cleanUp();
                }
        });
        
        System.out.println("Ready to load external JARs");
        loadedJars = loadExternalClasses();
        System.out.println("Finished 1st loading attempt");
        
        // Load classes from external sources
        if (loadedJars) {
            init();
        } else {
            // Give the user a chance to review help documentation or have another try
            System.out.println("External JARs not loaded");
            // Show help message or just quit?
            final JDialog extDialog = new JDialog(frame, "External JARs not loaded", false);
            JLabel msgLabel = new JLabel("External JAR files not loaded. These are required for running QTLMapper");
            JButton loadButton = new JButton("Try again");
            loadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    loadedJars = loadExternalClasses();
                    extDialog.setVisible(false);
                    extDialog.dispose();
                    if (loadedJars) {
                        init();
                    } else {
                        cleanUp();
                    }
                }
            });
            JButton quitButton = new JButton("Exit");
            quitButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    extDialog.setVisible(false);
                    extDialog.dispose();
                    cleanUp();
                }
            });
            JButton helpButton = new JButton("Help");
            helpButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#external");
                }
            });
            JPanel buttonPanel = new JPanel();
            
            extDialog.getContentPane().setLayout(new BorderLayout());
            extDialog.getContentPane().add(msgLabel, BorderLayout.CENTER);
            buttonPanel.setLayout(new FlowLayout());
            buttonPanel.add(loadButton);
            buttonPanel.add(quitButton);
            buttonPanel.add(helpButton);
            extDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            extDialog.pack();
            extDialog.setVisible(true);
        }
    }
    
    public void init() {
        
        // Retrieve maxd properties
        qtlName = mView.getProperty("qtlmapper.qtlname", null);
        species = mView.getProperty("qtlmapper.species", null);
        chromosome = mView.getProperty("qtlmapper.chr", null);
        start = mView.getProperty("qtlmapper.start", null);
        end = mView.getProperty("qtlmapper.end", null);
        startMarker = mView.getProperty("qtlmapper.startmark", null);
        endMarker = mView.getProperty("qtlmapper.endmark", null);
        startType = mView.getProperty("qtlmapper.starttype", null);
        endType = mView.getProperty("qtlmapper.endtype", null);
        dbHost = mView.getProperty("qtlmapper.dbhost", null);
        dbPort = mView.getProperty("qtlmapper.dbport", null);
        dbName = mView.getProperty("qtlmapper.dbname", null);
        dbUser = mView.getProperty("qtlmapper.dbuser", null);
        dbPass = mView.getProperty("qtlmapper.dbpass", null);
        
        System.out.println("Retrieved properties");
        
        // Create JTabbedPane
        tabPane = new JTabbedPane();
        
        // Create tabs
        createConnectTab();
        createDescribeTab();
        createSelectTab();
        
        tabPane.setEnabledAt(tabPane.indexOfComponent(connectPanel), true);
        tabPane.setEnabledAt(tabPane.indexOfComponent(describePanel), false);
        tabPane.setEnabledAt(tabPane.indexOfComponent(selectPanel), false);

        // Add tabbed pane to frame
        frame.getContentPane().add(tabPane);

        frame.pack();
        frame.setVisible(true);
    }
    
    private void createConnectTab() {
        connectPanel = new JPanel();

	JLabel mysqlJarLabel = new JLabel("'MySQL' Driver");
        final JTextField mysqlJarField = new JTextField(20);
	mysqlJarField.setEditable( false );
        if (dbHost != null) {
            mysqlJarField.setText( mView.getProperty("qtlmapper.mysqlfilepath", "[undefined]" ) );
        }


	JLabel ensjJarLabel = new JLabel("'ensj' Library");
        final JTextField ensjJarField = new JTextField(20);
	ensjJarField.setEditable( false );
        if (dbHost != null) {
            ensjJarField.setText( mView.getProperty("qtlmapper.ensjfilepath", "[undefined]" ) );
        }

	JButton mysqlJarChanger = new JButton("Select new driver");
	mysqlJarChanger.setMargin( new Insets( 1,1,1,1 ) );
	mysqlJarChanger.setFont( mView.getSmallFont() );
	mysqlJarChanger.addActionListener( new ActionListener() 
	    {
		public void actionPerformed(ActionEvent evt) 
		{
		    try 
		    {
                        String mysqlFilePath = mView.getProperty( "qtlmapper.mysqlfilepath", mysqlJarField.getText() );
			mysqlFilePath = mView.getFile( "Location of the JAR file for the MySQL JDBC driver '", mysqlFilePath );
			mView.putProperty( "qtlmapper.mysqlfilepath", mysqlFilePath );
			mysqlJarField.setText( mysqlFilePath );
                    } 
		    catch(UserInputCancelled uic) 
		    {
                    }
		}
	    });

	
	JButton ensjJarChanger = new JButton("Select new driver");
	ensjJarChanger.setMargin( new Insets( 1,1,1,1 ) );
	ensjJarChanger.setFont( mView.getSmallFont() );
	ensjJarChanger.addActionListener( new ActionListener() 
	    {
		public void actionPerformed(ActionEvent evt) 
		{
		    try 
		    {
                        String ensjFilePath = mView.getProperty( "qtlmapper.filepath", ensjJarField.getText() );
			ensjFilePath = mView.getFile( "Location of the JAR file for the 'Ensj' library", ensjFilePath );
			mView.putProperty( "qtlmapper.mysqlfilepath", ensjFilePath );
			mysqlJarField.setText( ensjFilePath );
                    } 
		    catch(UserInputCancelled uic) 
		    {
                    }
		}
	    });


        JLabel hostLabel = new JLabel("Host");
        final JTextField hostField = new JTextField(20);
        if (dbHost != null) {
            hostField.setText(dbHost);
        }
        hostField.setToolTipText("EnsEMBL database host");
        JLabel portLabel = new JLabel("Port");
        final JTextField portField = new JTextField(4);
        if (dbPort != null) {
            portField.setText(dbPort);
        }
        portField.setToolTipText("EnsEMBL database port number");
        JLabel userLabel = new JLabel("User");
        final JTextField userField = new JTextField(20);
        if (dbUser != null) {
            userField.setText(dbUser);
        }
        userField.setToolTipText("EnsEMBL database user");
        JLabel passLabel = new JLabel("Password");
        final JTextField passField = new JPasswordField(20);
        if (dbPass != null) {
            passField.setText(dbPass);
        }
        passField.setToolTipText("EnsEMBL database password (Leave blank if connecting as 'anonymous')");
        
        JPanel buttonPanel = new JPanel();
        JButton backButton = new JButton("Back");
        backButton.setEnabled(false);
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Get data from form fields
                dbHost = hostField.getText();
                dbPort = portField.getText();
                dbUser = userField.getText();
                dbPass = passField.getText();
                String err = retrieveDBList();
                if (err != null) {
                    mView.alertMessage(err);
                } else {
                    loadDescribeTab();
                    int describeIndex = tabPane.indexOfComponent(describePanel);
                    tabPane.setEnabledAt(describeIndex, true);
                    tabPane.setSelectedIndex(describeIndex);
                    tabPane.setEnabledAt(tabPane.indexOfComponent(connectPanel), false);
            
                }
            }
        });
        nextButton.setToolTipText("Go to next step");
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Clear all form fields
                hostField.setText(null);
                portField.setText (null);
                userField.setText(null);
                passField.setText(null);
            }
        });
        clearButton.setToolTipText("Clear all form fields");
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Reset form fields to initial value
                if (dbHost != null) {
                    hostField.setText(dbHost);
                }
                if (dbPort != null) {
                    portField.setText(dbPort);
                }
                if (dbUser != null) {
                    userField.setText(dbUser);
                }
                if (dbPass != null) {
                    passField.setText(dbPass);
                }
            }
        });
        resetButton.setToolTipText("Reset all form fields to initial values");
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cleanUp();
            }
        });
        exitButton.setToolTipText("Exit plugin");
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#ensembl");
            }
        });
        helpButton.setToolTipText("View help information");

        // Lay out form components
        connectPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        connectPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

	int line = 0;
	
	gbc.fill = GridBagConstraints.VERTICAL;

	gbc.gridy = line++;

        gbc.gridx = 0;
	gbc.anchor = GridBagConstraints.EAST;
	connectPanel.add(mysqlJarLabel, gbc);
	gbc.gridx = 1;
	connectPanel.add(Box.createRigidArea(new Dimension(10, 10)), gbc);
	gbc.gridx = 2;
	gbc.anchor = GridBagConstraints.WEST;
        connectPanel.add(mysqlJarField, gbc);
	gbc.gridx = 3;
	connectPanel.add(Box.createRigidArea(new Dimension(5, 5)), gbc);
	gbc.gridx = 4;
        connectPanel.add(mysqlJarChanger, gbc);

	gbc.gridx = 4;
	gbc.gridy = line++;
	connectPanel.add(Box.createRigidArea(new Dimension(5, 5)), gbc);

	gbc.gridy = line++;

        gbc.gridx = 0;
	gbc.anchor = GridBagConstraints.EAST;
	connectPanel.add(ensjJarLabel, gbc);
	gbc.gridx = 2;
	gbc.anchor = GridBagConstraints.WEST;
        connectPanel.add(ensjJarField, gbc);
	gbc.gridx = 4;
        connectPanel.add(ensjJarChanger, gbc);

	gbc.gridx = 0;
	gbc.gridy = line++;
	connectPanel.add(Box.createRigidArea(new Dimension(5, 25)), gbc);

	gbc.gridy = line++;

        gbc.gridx = 0;
	gbc.anchor = GridBagConstraints.EAST;
	connectPanel.add(hostLabel, gbc);
	gbc.gridx = 2;
	gbc.anchor = GridBagConstraints.WEST;
        connectPanel.add(hostField, gbc);

	gbc.gridx = 0;
	gbc.gridy = line++;
	connectPanel.add(Box.createRigidArea(new Dimension(5, 5)), gbc);

	gbc.gridy = line++;

        gbc.gridx = 0;
 	gbc.anchor = GridBagConstraints.EAST;
	connectPanel.add(portLabel, gbc);
	gbc.gridx = 2;
	gbc.anchor = GridBagConstraints.WEST;
	connectPanel.add(portField, gbc);

	gbc.gridx = 0;
	gbc.gridy = line++;
	connectPanel.add(Box.createRigidArea(new Dimension(5, 5)), gbc);

	gbc.gridy = line++;

	gbc.gridx = 0;
	gbc.anchor = GridBagConstraints.EAST;
	connectPanel.add(userLabel, gbc);
	gbc.gridx = 2;
 	gbc.anchor = GridBagConstraints.WEST;
	connectPanel.add(userField, gbc);
	
	gbc.gridx = 0;
	gbc.gridy = line++;
	connectPanel.add(Box.createRigidArea(new Dimension(5, 5)), gbc);

	gbc.gridy = line++;
	
        gbc.gridx = 0;
 	gbc.anchor = GridBagConstraints.EAST;
	connectPanel.add(passLabel, gbc);
	gbc.gridx = 2;
	gbc.anchor = GridBagConstraints.WEST;
	connectPanel.add(passField, gbc);

	gbc.gridy = line++;
	connectPanel.add(Box.createRigidArea(new Dimension(10, 10)), gbc);

      
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(clearButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(helpButton);

        gbc.gridx = 0;
	gbc.gridy = line++;
        gbc.gridwidth = 5;
        gbc.anchor = GridBagConstraints.SOUTH;
	gbc.fill = GridBagConstraints.NONE;
	gbc.weighty = 1.0;
	connectPanel.add(buttonPanel, gbc);

        // Ensure tabbed pane will be sized large enough to fit the other panels!
        connectPanel.setPreferredSize(new Dimension(500, 300));

        tabPane.addTab("EnsEMBL parameters", connectPanel);
    }
    
    private void createDescribeTab() {
        describePanel = new JPanel();
        tabPane.addTab("Describe QTL", describePanel);
    }
    
    private void createSelectTab() {
        selectPanel = new JPanel();
        tabPane.addTab("Select database", selectPanel);
    }
    
    private void loadDescribeTab() {
        
        // If the tab is already loaded, remove previous components
        if (describePanel.getComponentCount() > 0) {
            describePanel.removeAll();
        }
        
	describePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
 
        // Create form for describing QTL
        JLabel nameLabel = new JLabel("QTL name");
        final JTextField nameField = new JTextField(20);
        if (qtlName != null) {
            nameField.setText(qtlName);
        }
        nameField.setToolTipText("Name of the QTL (used as title for new column showing which spots represent genes in QTL)");
        JLabel speciesLabel = new JLabel("Species");
        final JComboBox speciesCombo = new JComboBox(speciesNames);
        if (species != null) {
            speciesCombo.setSelectedItem(species);
        }
        speciesCombo.setToolTipText("Select species for QTL");
        JLabel chrLabel = new JLabel("Chromosome");
        final JTextField chrField = new JTextField(2);
        if (chromosome != null) {
            chrField.setText(chromosome);
        }
        chrField.setToolTipText("Chromosome for the QTL");
        JLabel startLabel = new JLabel("Start");
        final JTextField startField = new JTextField(20);
        final JComboBox startCombo = new JComboBox(posTypes);
        if (startType != null) {
            startCombo.setSelectedItem(startType);
            if (startType.equals(MARKER_TYPE) && (startMarker != null)) {
                startField.setText(startMarker);
            } else if (startType.equals(POSITION_TYPE) && (start != null)) {
                startField.setText(start);
            }
        }
        startField.setToolTipText("Marker at start of QTL or start of QTL in base pairs");
        startCombo.setToolTipText("Select an option to indicate whether you have given start as a marker name or in base pairs");
        JLabel endLabel = new JLabel("End");
        final JTextField endField = new JTextField(20);
        final JComboBox endCombo = new JComboBox(posTypes);
        if (endType != null) {
            endCombo.setSelectedItem(endType);
            if (endType.equals(MARKER_TYPE) && (endMarker != null)) {
                endField.setText(endMarker);
            } else if (endType.equals(POSITION_TYPE) && (end != null)) {
                endField.setText(end);
            }
        }
        endField.setToolTipText("Marker at end of QTL or end of QTL in base pairs");
        endCombo.setToolTipText("Select an option to indicate whether you have given end as a marker name or in base pairs");
        JPanel buttonPanel = new JPanel();
        //JButton selectButton = new JButton("Select database");
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                qtlName = nameField.getText();
                species = (String)speciesCombo.getSelectedItem();
                chromosome = chrField.getText();
                startType = (String)startCombo.getSelectedItem();
                if (startType.equals(MARKER_TYPE)) {
                    startMarker = startField.getText();
                } else if (startType.equals(POSITION_TYPE)) {
                    start = startField.getText();
                }
                endType = (String)endCombo.getSelectedItem();
                if (endType.equals(MARKER_TYPE)) {
                    endMarker = endField.getText();
                } else if (endType.equals(POSITION_TYPE)) {
                    end = endField.getText();
                }
                loadSelectTab();
                int selectIndex = tabPane.indexOfComponent(selectPanel);
                tabPane.setEnabledAt(selectIndex, true);
                tabPane.setSelectedIndex(selectIndex);
                tabPane.setEnabledAt(tabPane.indexOfComponent(describePanel), false);
            }
        });
        nextButton.setToolTipText("Go to next step");
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Clear contents of all fields
                nameField.setText(null);
                speciesCombo.setSelectedItem(null);
                chrField.setText(null);
                startField.setText(null);
                startCombo.setSelectedItem(null);
                endField.setText(null);
                endCombo.setSelectedItem(null);                
            }
        });
        clearButton.setToolTipText("Clear all form fields");
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Reset all fields to initial values
                if (qtlName != null) {
                    nameField.setText(qtlName);
                }
                if (species != null) {
                    speciesCombo.setSelectedItem(species);
                }
                if (chromosome != null) {
                    chrField.setText(chromosome);
                }
                if (startType != null) {
                    startCombo.setSelectedItem(startType);
                    if (startType.equals(MARKER_TYPE) && (startMarker != null)) {
                        startField.setText(startMarker);
                    } else if (startType.equals(POSITION_TYPE) && (start != null)) {
                        startField.setText(start);
                    }
                }
                if (endType != null) {
                    endCombo.setSelectedItem(endType);
                    if (endType.equals(MARKER_TYPE) && (endMarker != null)) {
                            endField.setText(endMarker);
                    } else if (endType.equals(POSITION_TYPE) && (end != null)) {
                        endField.setText(end);
                    }
                }
            }
        });
        resetButton.setToolTipText("Reset all form fields to their initial values");
        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // NEED TO CHANGE
                // Go back to the EnsEMBL parameters tab
                int connectIndex = tabPane.indexOfComponent(connectPanel);
                tabPane.setEnabledAt(connectIndex, true);
                tabPane.setSelectedIndex(connectIndex);
                tabPane.setEnabledAt(tabPane.indexOfComponent(describePanel), false);
            }
        });
        backButton.setToolTipText("Return to previous step");
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cleanUp();
            }
        });
        exitButton.setToolTipText("Exit plugin");
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#describe");
            }
        });
        helpButton.setToolTipText("View help information");

        // Lay out form components
        GridBagConstraints gbc = new GridBagConstraints();
        describePanel.setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 7;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        describePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 1;
        describePanel.add(nameLabel, gbc);
        gbc.gridx = 2;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 3;
        describePanel.add(nameField, gbc);
        gbc.gridx = 6;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 7;
        describePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 1;
        describePanel.add(speciesLabel, gbc);
        gbc.gridx = 2;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 3;
        describePanel.add(speciesCombo, gbc);
        gbc.gridx = 6;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 7;
        describePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 1;
        describePanel.add(chrLabel, gbc);
        gbc.gridx = 2;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 3;
        describePanel.add(chrField, gbc);
        gbc.gridx = 6;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 7;
        describePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 1;
        describePanel.add(startLabel, gbc);
        gbc.gridx = 2;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 3;
        describePanel.add(startField, gbc);
        gbc.gridx = 4;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 5;
        describePanel.add(startCombo, gbc);
        gbc.gridx = 6;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 7;
        describePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
        gbc.gridy = 9;
        gbc.gridwidth = 1;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 1;
        describePanel.add(endLabel, gbc);
        gbc.gridx = 2;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 3;
        describePanel.add(endField, gbc);
        gbc.gridx = 4;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 5;
        describePanel.add(endCombo, gbc);
        gbc.gridx = 6;
        describePanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 7;
        describePanel.add(Box.createRigidArea(new Dimension(0, 20)), gbc);
        
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(clearButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(helpButton);

        
        gbc.gridy = 11;
        gbc.gridwidth = 7;
	gbc.anchor = GridBagConstraints.SOUTH;
	gbc.weighty = 1.0;
        describePanel.add(buttonPanel, gbc);

        //gbc.gridy = 12;
        //describePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

    }
    
    private void loadSelectTab() {
        
        if (selectPanel.getComponentCount() > 0) {
            selectPanel.removeAll();
        }
        
        JLabel nameLabel = new JLabel("Database:");
        Vector specDBNames = new Vector();
        for (int i = 0; i < dbNames.size(); i++) {
            String name = (String)dbNames.elementAt(i);
            if (name.indexOf(species) >= 0) {
                specDBNames.addElement(name);
            }
        }
        final JComboBox nameCombo = new JComboBox(specDBNames);
        if (dbName == null) {
            // default is the most recent database i.e. last in list
            int nItems = nameCombo.getItemCount();
            nameCombo.setSelectedIndex(nItems);
        } else {
            nameCombo.setSelectedItem(dbName);
        }
        nameCombo.setToolTipText("Select the EnsEMBL database to use");
        JPanel buttonPanel = new JPanel();
        JButton finishButton = new JButton("Next");
        finishButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dbName = (String)nameCombo.getSelectedItem();
                // Label spots to show presence within QTL
                if (findQTLProbes()) {
                    labelSpots();
                    cleanUp();
                }
            }
        });
        finishButton.setToolTipText("Finish (label spots to indicate inclusion in the QTL)");
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Clear form fields
                nameCombo.setSelectedItem(null);
            }
        });
        clearButton.setToolTipText("Clear all form fields");
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                // Reset form fields
                if (dbName != null) {
                    nameCombo.setSelectedItem(dbName);
                }
            }
        });
        resetButton.setToolTipText("Reset form fields to their previous values");
        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int describeIndex = tabPane.indexOfComponent(describePanel);
                tabPane.setEnabledAt(describeIndex, true);
                tabPane.setSelectedIndex(describeIndex);
                tabPane.setEnabledAt(tabPane.indexOfComponent(selectPanel), false);
            }
        });
        backButton.setToolTipText("Return to previous step");
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cleanUp();
            }
        });
        exitButton.setToolTipText("Exit plugin");
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#selectdb");
            }
        });
        helpButton.setToolTipText("View help information");
        
        // Lay out components in form
        selectPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
        selectPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        selectPanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 1;
        selectPanel.add(nameLabel, gbc);
        gbc.gridx = 2;
        selectPanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        selectPanel.add(nameCombo, gbc);
        gbc.gridx = 4;
        selectPanel.add(Box.createRigidArea(new Dimension(10, 0)), gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 5;
        selectPanel.add(Box.createRigidArea(new Dimension(0, 20)), gbc);
        
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(backButton);
        buttonPanel.add(finishButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(clearButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(helpButton);

        
        gbc.gridy = 3;
	gbc.anchor = GridBagConstraints.SOUTH;
	gbc.weighty = 0.1;
        gbc.gridwidth = 5;
        selectPanel.add(buttonPanel, gbc);
        gbc.gridy = 4;
        selectPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);
    }

    private String retrieveDBList() {
        
        // Connect to EnsEMBL
        try {
            String dbURL = "jdbc:mysql://"+dbHost+"/"+dbListName+"?user="+dbUser;
            if (dbPass != null) {
                dbURL = dbURL+"&password="+dbPass;
            }
            System.out.println("Database URL: "+dbURL);
            //Connection con = java.sql.DriverManager.getConnection(dbURL);
            java.sql.Driver mysqlDriver = (java.sql.Driver)mysqlDriverClass.newInstance();
            Connection con = mysqlDriver.connect(dbURL, null);
            System.out.println("Got connection to database");
            // Retrieve list of databases and store in array
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            speciesNames = new Vector();
            dbNames = new Vector();
            while (rs.next()) {
                String ensDB = rs.getString("Database");
                if (ensDB.indexOf("core") >= 0) {
                    dbNames.addElement(ensDB);
                    // Trim species name from start and add to species list
                    int index = ensDB.indexOf("_core");
                    String spec = ensDB.substring(0,index);
                    if (speciesNames.indexOf(spec) < 0) {
                        speciesNames.addElement(spec);
                    }
                }
            }
            return null;
        } catch (InstantiationException iex) {
            return "Cannot instantiate MySQL database driver: "+iex;
        } catch (IllegalAccessException iax) {
            return "Illegal access to MySQL database driver: "+iax;
        } catch (SQLException ex) {
            return "Error in querying EnsEMBL for database list: "+ex;
        }
    }
        
    private boolean findQTLProbes() {
        qtlProbes = new Vector();
        try {
            // Get EnsEMBL database driver
            int portInt = (new Integer(dbPort)).intValue();
            org.ensembl.driver.CoreDriver coreDriver = org.ensembl.driver.CoreDriverFactory.createCoreDriver(dbHost, portInt, dbName, dbUser, dbPass);
            // If start or end are given as markers get position in bp
            if (startType.equals(MARKER_TYPE)) {
                String msg = "";
                boolean gotLocation = false;
                LOC: while (!gotLocation) {
                    org.ensembl.driver.MarkerAdaptor ma = coreDriver.getMarkerAdaptor();
                    org.ensembl.datamodel.Marker mk = ma.fetchBySynonym(startMarker);
                    if (mk == null) {
                        msg = "Marker "+startMarker+" is not present in EnsEMBL database "+dbName;
                        break LOC;
                    }
                    java.util.List mfList = mk.getMarkerFeatures();
                    Iterator iter = mfList.iterator();
                    while (iter.hasNext()) {
                        org.ensembl.datamodel.MarkerFeature mf = (org.ensembl.datamodel.MarkerFeature)iter.next();
                        org.ensembl.datamodel.Location loc = mf.getLocation();
                        org.ensembl.datamodel.CoordinateSystem cs = loc.getCoordinateSystem();
                        if (!cs.getName().equals("chromosome")) {
                            continue;
                        }
                        if (!loc.getSeqRegionName().equals(chromosome)) {
                            continue;
                        }
                        start = Integer.toString(loc.getStart());
                        gotLocation = true;
                        break;
                    }
                    if (!gotLocation) {
                        msg = "Marker "+startMarker+" is not mapped to chromosome "+chromosome+" in EnsEMBL database "+dbName;
                        break LOC;
                    }
                }
                if (!gotLocation) {
                    msg = msg + "\nPlease enter a start position in bp";
                    start = (String)JOptionPane.showInputDialog(frame, msg, "Cannot obtain marker position", JOptionPane.WARNING_MESSAGE, null, null, null);
                    if (start == null || start == "") {
                        return false;
                    }
                }
            }
            if (endType.equals(MARKER_TYPE)) {
                String msg = "";
                boolean gotLocation = false;
                LOC: while (!gotLocation) {
                    org.ensembl.driver.MarkerAdaptor ma = coreDriver.getMarkerAdaptor();
                    org.ensembl.datamodel.Marker mk = ma.fetchBySynonym(endMarker);
                    if (mk == null) {
                        msg = "Marker "+endMarker+" is not present in EnsEMBL database "+dbName;
                        break LOC;
                    }
                    java.util.List mfList = mk.getMarkerFeatures();
                    Iterator iter = mfList.iterator();
                    while (iter.hasNext()) {
                        org.ensembl.datamodel.MarkerFeature mf = (org.ensembl.datamodel.MarkerFeature)iter.next();
                        org.ensembl.datamodel.Location loc = mf.getLocation();
                        org.ensembl.datamodel.CoordinateSystem cs = loc.getCoordinateSystem();
                        if (!cs.getName().equals("chromosome")) {
                            continue;
                        }
                        if (!loc.getSeqRegionName().equals(chromosome)) {
                            continue;
                        }
                        end = Integer.toString(loc.getEnd());
                        gotLocation = true;
                        break;
                    }
                    if (!gotLocation) {
                        msg = "Marker "+endMarker+" is not mapped to chromosome "+chromosome+" in EnsEMBL database "+dbName;
                        break LOC;
                    }
                }
                if (!gotLocation) {
                    msg = msg + "\nPlease enter an end position in bp";
                    end = (String)JOptionPane.showInputDialog(frame, msg, "Cannot obtain marker position", JOptionPane.WARNING_MESSAGE, null, null, null);
                    if (end == null || end == "") {
                        return false;
                    }
                }
            }

            // Find all genes in QTL region
            // Make a fake location to represent the QTL and fetch by this region
            org.ensembl.datamodel.CoordinateSystem qtlCS = new org.ensembl.datamodel.CoordinateSystem("chromosome");
            int startInt = (new Integer(start)).intValue();
            int endInt = (new Integer(end)).intValue();
            if (startInt > endInt) {
                int temp = startInt;
                startInt = endInt;
                endInt = temp;
            }
            System.out.println("Finding probe list for QTL on chromosome "+chromosome+", "+start+" to "+end);
            org.ensembl.datamodel.Location qtlLoc = new org.ensembl.datamodel.Location(qtlCS, chromosome, startInt, endInt);
            org.ensembl.driver.GeneAdaptor ga = coreDriver.getGeneAdaptor();
            java.util.List geneList = ga.fetch(qtlLoc);
            Iterator iter = geneList.iterator();
            while (iter.hasNext()) {
                // Get all the external refs for each gene in QTL region
                org.ensembl.datamodel.Gene gene = (org.ensembl.datamodel.Gene)iter.next();
                java.util.List refList = gene.getExternalRefs();
                Iterator refIter = refList.iterator();
                while (refIter.hasNext()) {
                    org.ensembl.datamodel.ExternalRef ref = (org.ensembl.datamodel.ExternalRef)refIter.next();
                    String refID = ref.getDisplayID();
                    String source = ref.getExternalDatabase().getName();
                    // Then check through synonyms for ones that contain "AFFY"
                    if (source.indexOf("AFFY") >= 0) {
                        System.out.println("Found QTL probe "+refID);
                        qtlProbes.addElement(refID);
                    }
                }
            }
        } catch (Exception aex) {
            mView.alertMessage("Error in retrieving EnsEMBL data: "+aex);
            return false;
        }
        return true;
    }

    private void labelSpots() {
        int numSpots = exprData.getNumSpots();
        double[] qtlFlags = new double[numSpots];
        // For each spot
        for (int i = 0; i < numSpots; i++) {
            // Get probe name
            String probeName = exprData.getProbeNameAtIndex(i);
            // Check if name is in QTL list
            if (qtlProbes.indexOf(probeName) >= 0) {
                qtlFlags[i] = 1.0d;
            } else {
                qtlFlags[i] = 0.0d;
            }
        }
        
        // Create new measurement
        exprData.addMeasurement(exprData.new Measurement(qtlName, ExprData.UnknownDataType, qtlFlags));
        mView.infoMessage("QTLMapper has finished labelling spots");
    }
    
    /**
     * Load classes from external Jar files
     */
    private boolean loadExternalClasses() {
        maxdView.CustomClassLoader ensjCL = (maxdView.CustomClassLoader)getClass().getClassLoader();
        String ensjJarLocation = mView.getTopDirectory() + java.io.File.separatorChar +
        "external" + java.io.File.separatorChar +  "ensj" + java.io.File.separatorChar + "ensj-31.1.jar";

        String ensjFilePath = mView.getProperty("qtlmapper.ensjfilepath", ensjJarLocation);
        mView.setMessage("Initialising EnsJ classes");
        Thread.yield();

        boolean found = false;
        while (!found) {
            if (ensjFilePath != null) {
                try {
                    ensjCL.addPath(ensjFilePath);
                } catch(java.net.MalformedURLException murle){
                    String msg = "Unable to load the ensj JAR file from '" + ensjFilePath + "'\n" +
                    "\nPress \"Find\" to specify an alternate location for the file,\n" +
                    "  or\nPress \"Cancel\" to stop the plugin.\n" +
                    "\n(see the help page for more information)\n";

                    if (mView.alertQuestion(msg, "Find", "Cancel") == 1) {
                        return found;
                    }
                    
                    try {
                        ensjFilePath = mView.getFile("Location of 'ensj-31.1.jar'", ensjFilePath);
                    } catch(UserInputCancelled uic) {
                        return found;
                    }
                }
            }

            Class testClass = ensjCL.findClass("org.ensembl.driver.CoreDriver");

            if (testClass != null) {
                found = true;
            }
            
        
            if (!found) {
                try {
                    String msg = "Unable to find the ensj JAR file";
                    if (ensjFilePath == null) {
                        msg += "\n";
                    } else {
                        msg += " in "+ensjFilePath+"\n";
                    }
                    msg += "\nPress \"Find\" to specify the location of the file,\n" +
                    "  or\nPress \"Cancel\" to stop the plugin.\n" +
                    "\n(see the help page for more information)\n";

                    if (mView.alertQuestion(msg, "Find", "Cancel") == 1) {
                        return found;
                    }
                    ensjFilePath = mView.getFile("Location of 'ensj-31.1.jar'", ensjFilePath);
                } catch (UserInputCancelled uic) {
                    return found;
                }

            } else {
                mView.putProperty("qtlmapper.ensjfilepath", ensjFilePath);
            }
        
        }


        maxdView.CustomClassLoader mysqlCL = (maxdView.CustomClassLoader)getClass().getClassLoader();
        String mysqlJarLocation = mView.getTopDirectory() + java.io.File.separatorChar +
        "external" + java.io.File.separatorChar +  "mysql-connector-java" + java.io.File.separatorChar + "mysql-connector-java-3.1.8-bin.jar";

        String mysqlFilePath = mView.getProperty("qtlmapper.mysqlfilepath", mysqlJarLocation);
        mView.setMessage("Initialising MySQL connector for Java classes");
        Thread.yield();

        found = false;
        while (!found) {
            if (mysqlFilePath != null) {
                try {
                    mysqlCL.addPath(mysqlFilePath);
                } catch(java.net.MalformedURLException murle){
                    String msg = "Unable to load the mysql-connector-java JAR file from '" + mysqlFilePath + "'\n" +
                    "\nPress \"Find\" to specify an alternate location for the file,\n" +
                    "  or\nPress \"Cancel\" to stop the plugin.\n" +
                    "\n(see the help page for more information)\n";
    
                    if (mView.alertQuestion(msg, "Find", "Cancel") == 1) {
                        return found;
                    }
                    
                    try {
                        mysqlFilePath = mView.getFile("Location of 'mysql-connector-java-3.1.8-bin.jar'", mysqlFilePath);
                    } catch(UserInputCancelled uic) {
                        return found;
                    }
                }
            }

            mysqlDriverClass = mysqlCL.findClass("com.mysql.jdbc.Driver");

            if (mysqlDriverClass != null) {
                found = true;
            }
            
        
            if (!found) {
                try {
                    String msg = "Unable to find the mysql-connector-java JAR file";
                    if (mysqlFilePath == null) {
                        msg += "\n";
                    } else {
                        msg += " in "+mysqlFilePath+"\n";
                    }
                    msg += "\nPress \"Find\" to specify the location of the file,\n" +
                    "  or\nPress \"Cancel\" to stop the plugin.\n" +
                    "\n(see the help page for more information)\n";

                    if (mView.alertQuestion(msg, "Find", "Cancel") == 1) {
                        return found;
                    }
                    mysqlFilePath = mView.getFile("Location of 'mysql-connector-java-3.1.8-bin.jar'", mysqlFilePath);
                } catch (UserInputCancelled uic) {
                    return found;
                }

            } else {
                
                mView.putProperty("qtlmapper.mysqlfilepath", mysqlFilePath);
            }
        }

        // Preload the required classes
        Class axC = ensjCL.findClass("org.ensembl.driver.AdaptorException");
        Class drC = ensjCL.findClass("org.ensembl.driver.CoreDriver");
        Class dfC = ensjCL.findClass("org.ensembl.driver.CoreDriverFactory");
        Class maC = ensjCL.findClass("org.ensembl.driver.MarkerAdaptor");
        Class mkC = ensjCL.findClass("org.ensembl.datamodel.Marker");
        Class mfC = ensjCL.findClass("org.ensembl.datamodel.MarkerFeature");
        Class locC = ensjCL.findClass("org.ensembl.datamodel.Location");
        Class csC = ensjCL.findClass("org.ensembl.datamodel.CoordinateSystem");
        Class gaC = ensjCL.findClass("org.ensembl.driver.GeneAdaptor");
        Class geC = ensjCL.findClass("org.ensembl.datamodel.Gene");
        Class erC = ensjCL.findClass("org.ensembl.datamodel.ExternalRef");
        

        if (mysqlDriverClass == null) {
            mysqlDriverClass = mysqlCL.findClass("com.mysql.jdbc.Driver");
        }
        
        return true;
    }

    /**
     * Clean up and free up resources on exitting plugin
     */
    private void cleanUp() {
        // Set maxdView properties
        mView.putProperty("qtlmapper.qtlname", qtlName);
        mView.putProperty("qtlmapper.species", species);
        mView.putProperty("qtlmapper.chr", chromosome);
        mView.putProperty("qtlmapper.start", start);
        mView.putProperty("qtlmapper.end", end);
        mView.putProperty("qtlmapper.startmark", startMarker);
        mView.putProperty("qtlmapper.endmark", endMarker);
        mView.putProperty("qtlmapper.starttype", startType);
        mView.putProperty("qtlmapper.endtype", endType);
        mView.putProperty("qtlmapper.dbhost", dbHost);
        mView.putProperty("qtlmapper.dbport", dbPort);
        mView.putProperty("qtlmapper.dbname", dbName);
        mView.putProperty("qtlmapper.dbuser", dbUser);
        mView.putProperty("qtlmapper.dbpass", dbPass);

        frame.setVisible(false);
        frame.dispose();
    }
}



