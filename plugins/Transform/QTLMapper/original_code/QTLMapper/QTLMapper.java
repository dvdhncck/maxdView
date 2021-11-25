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
            JButton quitButton = new JButton("Quit");
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
            
            extDialog.setLayout(new BorderLayout());
            extDialog.add(msgLabel, BorderLayout.CENTER);
            buttonPanel.setLayout(new FlowLayout());
            buttonPanel.add(loadButton);
            buttonPanel.add(quitButton);
            buttonPanel.add(helpButton);
            extDialog.add(buttonPanel, BorderLayout.SOUTH);
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

	showDBListDialog();

    }

    private void showDBListDialog() {
        System.out.println("In showDBListDialog");
	// Show a dialog for acquiring a list of databases
	final JDialog listDialog = new JDialog(frame, "Set EnsEMBL connection parameters", false);
	
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
	passField.setToolTipText("EnsEMBL database password\n(Leave blank if connecting as 'anonymous'");
	
        System.out.println("Created DB list dialog text fields");
        
        JPanel buttonPanel = new JPanel();
	JButton qtlButton = new JButton("Describe QTL");
	qtlButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
		// Get data from form fields
		dbHost = hostField.getText();
		dbPort = portField.getText();
		dbUser = userField.getText();
		dbPass = passField.getText();
		// Retrieve database list
		retrieveDBList();
		// Describe QTL
		showQTLInfoDialog();
		listDialog.setVisible(false);
		listDialog.dispose();
	    }
	});
	qtlButton.setToolTipText("Describe QTL parameters");
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
	JButton cancelButton = new JButton("Cancel");
	cancelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
		// Close dialog
		listDialog.setVisible(false);
		listDialog.dispose();
	    }
	});
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#ensembl");
            }
        });
        
        System.out.println("Created DB list dialog buttons");

	// Lay out form components
	listDialog.setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.gridwidth = 1;
	gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
	listDialog.add(hostLabel, gbc);
	gbc.gridx = 1;
	listDialog.add(hostField, gbc);
	gbc.gridx = 0;
	gbc.gridy = 1;
	listDialog.add(portLabel, gbc);
	gbc.gridx = 1;
	listDialog.add(portField, gbc);
	gbc.gridx = 0;
	gbc.gridy = 2;
	listDialog.add(userLabel, gbc);
	gbc.gridx = 1;
	listDialog.add(userField, gbc);
	gbc.gridx = 0;
	gbc.gridy = 3;
	listDialog.add(passLabel, gbc);
	gbc.gridx = 1;
	listDialog.add(passField, gbc);
	
	buttonPanel.setLayout(new FlowLayout());
	buttonPanel.add(qtlButton);
	buttonPanel.add(clearButton);
	buttonPanel.add(resetButton);
	buttonPanel.add(cancelButton);
        buttonPanel.add(helpButton);

	gbc.gridx = 0;
	gbc.gridy = 4;
	gbc.gridwidth = 2;
	listDialog.add(buttonPanel, gbc);

        System.out.println("Finished DB list dialog");
        
	listDialog.pack();
	listDialog.setVisible(true);
    }

    private void retrieveDBList() {
        try {
            // Load database driver
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException cnf) {
            mView.alertMessage("Couldn't find MySQL database driver class. Cannot retrieve database list");
            return;
        }
        // Connect to EnsEMBL
        try {
            String dbURL = "jdbc:mysql://"+dbHost+"/"+dbListName+"?user="+dbUser;
            if (dbPass != null) {
                dbURL = dbURL+"&password="+dbPass;
            }
            System.out.println("Database URL: "+dbURL);
            java.sql.Driver driver = java.sql.DriverManager.getDriver(dbURL);
            try {
                System.out.println("Got suitable driver:"+driver.toString());
            } catch (NullPointerException npe) {
                mView.alertMessage("Cannot find suitable database driver");
                return;
            }
            Connection con = java.sql.DriverManager.getConnection(dbURL);
            System.out.println("Got connection to database");
	    // Retrieve list of databases and store in array
	    Statement stmt = con.createStatement();
	    ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            speciesNames = new Vector();
            dbNames = new Vector();
	    while (rs.next()) {
		String ensDB = rs.getString("Database");
		if (ensDB.contains("core")) {
		    dbNames.addElement(ensDB);
		    // Trim species name from start and add to species list
		    int index = ensDB.indexOf("_core");
		    String spec = ensDB.substring(0,index);
		    if (!speciesNames.contains(spec)) {
			speciesNames.addElement(spec);
		    }
		}
	    }
	} catch (SQLException ex) {
	    mView.alertMessage("Error in querying EnsEMBL for database list: " + ex);
	}
    }

    private void showQTLInfoDialog() {
	
	final JDialog qtlInfoDialog = new JDialog(frame, "Describe QTL", false);

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
	JButton selectButton = new JButton("Select database");
	selectButton.addActionListener(new ActionListener() {
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
	        showSelectDialog();
	    }
	});
	selectButton.setToolTipText("Select the EnsEMBL database to use for checking which probes lie within the QTL");
	JButton latestButton = new JButton("Run with latest");
	latestButton.addActionListener(new ActionListener() {
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

	        // dbName =  name of latest database for species
		for (int i = 0; i < dbNames.size(); i++) {
		    String name = (String)dbNames.elementAt(i);
		    if (name.contains(species) && name.contains("core")) {
			dbName = name;
		    }
		}
	        // Find QTL probes
	        findQTLProbes();
	        // Label spots
	        labelSpots();
		qtlInfoDialog.setVisible(false);
		qtlInfoDialog.dispose();
	        cleanUp();
	    }
	});
	latestButton.setToolTipText("Label spots for genes within the QTL, using the latest EnsEMBL database to check positions");
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
	JButton cancelButton = new JButton("Cancel");
	cancelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
		// Close the dialog
		qtlInfoDialog.setVisible(false);
		qtlInfoDialog.dispose();
	    }
	});
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#describe");
            }
        });

	// Lay out form components
	GridBagConstraints gbc = new GridBagConstraints();
	qtlInfoDialog.setLayout(new GridBagLayout());

	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.gridwidth = 1;
	gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
	qtlInfoDialog.add(nameLabel, gbc);
	gbc.gridx = 1;
	gbc.gridwidth = 2;
	qtlInfoDialog.add(nameField, gbc);
	gbc.gridx = 0;
	gbc.gridy = 1;
	gbc.gridwidth = 1;
	qtlInfoDialog.add(speciesLabel, gbc);
	gbc.gridx = 1;
	gbc.gridwidth = 2;
	qtlInfoDialog.add(speciesCombo, gbc);
	gbc.gridx = 0;
	gbc.gridy = 2;
	gbc.gridwidth = 1;
	qtlInfoDialog.add(chrLabel, gbc);
	gbc.gridx = 1;
	gbc.gridwidth = 2;
	qtlInfoDialog.add(chrField, gbc);
	gbc.gridx = 0;
	gbc.gridy = 3;
	gbc.gridwidth = 1;
	qtlInfoDialog.add(startLabel, gbc);
	gbc.gridx = 1;
	qtlInfoDialog.add(startField, gbc);
	gbc.gridx = 2;
	qtlInfoDialog.add(startCombo, gbc);
	gbc.gridx = 0;
	gbc.gridy = 4;
	qtlInfoDialog.add(endLabel, gbc);
	gbc.gridx = 1;
	qtlInfoDialog.add(endField, gbc);
	gbc.gridx = 2;
	qtlInfoDialog.add(endCombo, gbc);
	
	buttonPanel.setLayout(new FlowLayout());
	buttonPanel.add(selectButton);
	buttonPanel.add(latestButton);
	buttonPanel.add(clearButton);
	buttonPanel.add(resetButton);
	buttonPanel.add(cancelButton);
        buttonPanel.add(helpButton);
	
	gbc.gridx = 0;
	gbc.gridy = 5;
	gbc.gridwidth = 3;
	qtlInfoDialog.add(buttonPanel, gbc);

	qtlInfoDialog.pack();
	qtlInfoDialog.setVisible(true);
    }


    private void showSelectDialog() {
	// Show dialog for selecting a database from the list
	final JDialog selectDialog = new JDialog(frame, "Select EnsEMBL database", false);
    
	JLabel nameLabel = new JLabel("Database:");
	Vector specDBNames = new Vector();
	for (int i = 0; i < dbNames.size(); i++) {
	    String name = (String)dbNames.elementAt(i);
	    if (name.contains(species)) {
		specDBNames.addElement(name);
	    }
	}
	final JComboBox nameCombo = new JComboBox(specDBNames);
	if (dbName != null) {
	    nameCombo.setSelectedItem(dbName);
	}
	nameCombo.setToolTipText("Select the EnsEMBL database to use");
	JPanel buttonPanel = new JPanel();
	JButton labelButton = new JButton("Label spots");
	labelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
		dbName = (String)nameCombo.getSelectedItem();
		// Label spots to show presence within QTL
		findQTLProbes();
		labelSpots();
		selectDialog.setVisible(false);
		selectDialog.dispose();
                cleanUp();
	    }
	});
        labelButton.setToolTipText("Label spots to indicate presence within QTL");
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
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
		// Close dialog
		selectDialog.setVisible(false);
		selectDialog.dispose();
	    }
	});
        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mView.getPluginHelpTopic("QTLMapper", "QTLMapper", "#ensembl");
            }
        });
	
	// Lay out components in form
        selectDialog.setLayout(new GridBagLayout());
	GridBagConstraints gbc = new GridBagConstraints();
	
	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.gridwidth = 1;
	gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST;
	selectDialog.add(nameLabel, gbc);
	gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
	selectDialog.add(nameCombo, gbc);
	
	buttonPanel.setLayout(new FlowLayout());
	buttonPanel.add(labelButton);
	buttonPanel.add(clearButton);
	buttonPanel.add(resetButton);
	buttonPanel.add(cancelButton);
        buttonPanel.add(helpButton);
	
	gbc.gridx = 0;
	gbc.gridy = 1;
	gbc.gridwidth = 2;
	selectDialog.add(buttonPanel, gbc);

	selectDialog.pack();
	selectDialog.setVisible(true);
    }

    private void findQTLProbes() {
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
                        return;
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
                        return;
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
                    //System.out.println("Found external ref "+refID);
                    // Then check through synonyms for ones that contain "AFFY"
                    if (source.contains("AFFY")) {
                        System.out.println("Found QTL probe "+refID);
                        qtlProbes.addElement(refID);
                    }
                }
            }
        } catch (Exception aex) { //(org.ensembl.driver.AdaptorException aex) {
            mView.alertMessage("Error in retrieving EnsEMBL data: "+aex);
        }
    }

    private void labelSpots() {
        int numSpots = exprData.getNumSpots();
        double[] qtlFlags = new double[numSpots];
	// For each spot
        for (int i = 0; i < numSpots; i++) {
            // Get probe name
            String probeName = exprData.getProbeNameAtIndex(i);
            // Check if name is in QTL list
            if (qtlProbes.contains(probeName)) {
                qtlFlags[i] = 1.0d;
            } else {
                qtlFlags[i] = 0.0d;
            }
        }
	
        // Create new measurement
        exprData.addMeasurement(exprData.new Measurement(qtlName, ExprData.UnknownDataType, qtlFlags));
    }
    
    /**
     * Load classes from external Jar files
     */
    private boolean loadExternalClasses() {
        maxdView.CustomClassLoader ensjCL = (maxdView.CustomClassLoader)getClass().getClassLoader();
        String ensjJarLocation = mView.getTopDirectory() + java.io.File.separatorChar + 
        "external" + java.io.File.separatorChar +  "ensj" + java.io.File.separatorChar + "ensj-31.1.jar";

        String ensjFilePath = mView.getProperty("ensjfilepath", ensjJarLocation);
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

                // NEED TO CHANGE
                // Shouldn't we add this path to class loader as above?
                // Shouldn't we test loading a class?
                // Shouldn't we add mView property?
		

            } else {
                mView.putProperty("ensjfilepath", ensjFilePath);
            }
	
            // Exit loop
            break;
        }


        maxdView.CustomClassLoader mysqlCL = (maxdView.CustomClassLoader)getClass().getClassLoader();
        String mysqlJarLocation = mView.getTopDirectory() + java.io.File.separatorChar + 
        "external" + java.io.File.separatorChar +  "mysql-connector-java" + java.io.File.separatorChar + "mysql-connector-java-3.1.8-bin.jar";

        String mysqlFilePath = mView.getProperty("mysqlfilepath", mysqlJarLocation);
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

            Class testClass = mysqlCL.findClass("com.mysql.jdbc.Driver");

            if (testClass != null) {
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

		// NEED TO CHANGE
		// Shouldn't we add this path to class loader as above?
		// Shouldn't we test loading a class?
		// Shouldn't we add mView property?

            } else {
		
                mView.putProperty("mysqlfilepath", mysqlFilePath);
            }
	
            // Exit loop
            break;
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
        

        // Try commenting out normal database driver class loader code and loading here instead
        Class mdC = mysqlCL.findClass("com.mysql.jdbc.Driver");
        
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
    }
}

