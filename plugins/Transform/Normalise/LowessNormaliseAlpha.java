/*
 * LowessNormaliseAlpha.java
 *
 * Created on 13 February 2003, 10:54
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author  HelenH
 */
public class LowessNormaliseAlpha implements Plugin, ExprData.ExprDataObserver {
    
    public static boolean DEFAULT_CREATE_RATIO_COL = true;
    public static boolean DEFAULT_CREATE_INT_COLS = false;
    
    private maxdView mview;
    private ExprData edata;
    
    
    //Data Objects
    
    private double[][] data = null;
    private int left_measure_index;
    private int right_measure_index;
    private int left_measure_id;
    private int right_measure_id;
    private int[] data_id_to_spot_id = null;
    private int[] data_id_to_meas_id = null;
    private String[] measurement_names = null;
    
    
    //private Normaliser selected_normaliser= null;
   
    //GUI Objects
    
    
    private JFrame frame;
    private JTabbedPane tabbed;
    
    private DragAndDropList left_meas_list;
    private DragAndDropList right_meas_list;
    
    private JCheckBox apply_filter_jchkb;
    private JCheckBox create_ratio_col_jchkb;
    private JCheckBox create_intens_cols_jchkb;
    
    private JTextField ratio_colname_jtp;
    private JTextField intens_colname1_jtp;
    private JTextField intens_colname2_jtp;
    
    private JComboBox method_jcb;
    
    private JPanel ui_panel;
    private JPanel info_panel;
    private JTextPane info_text_area;
    
    private JButton nbutton;
    private JButton bbutton;
    
    //mode for the tabbing
    private int mode;
    //
    
    
    /** Creates a new instance of LowessNormalise */
    public LowessNormaliseAlpha(maxdView mview_) {
        mview = mview_;
        edata = mview.getExprData();
    }
    
    public PluginCommand[] getPluginCommands() {
        return null;
    }
    
    public PluginInfo getPluginInfo() {
        PluginInfo pinf = new PluginInfo("Lowess", 
                                         "transform", 
                                         "Apply Lowess Normalisation", 
                                         "", // add any comment return development at Manchester HERE
                                         1, 1, 0);                
        return pinf;
    }
    
    public void runCommand(String str, String[] str1, CommandSignal commandSignal) {
    }
    
    public void startPlugin() {
        
        addComponents();
        edata.addObserver(this);
        frame.pack();
        frame.setVisible(true);
    }
    
    public void stopPlugin() {
        cleanUp();
    }
    
    
    //BEGIN Methods from ExprDataObserver
    public void clusterUpdate(ExprData.ClusterUpdateEvent clusterUpdateEvent) {
    }
    
    public void dataUpdate(ExprData.DataUpdateEvent dataUpdateEvent) {
    }
    
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent environmentUpdateEvent) {
    }
    
    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue) {
        switch(mue.event)
        {
        case ExprData.SizeChanged:
        case ExprData.ElementsAdded:
        case ExprData.ElementsRemoved:
        case ExprData.NameChanged:
        case ExprData.OrderChanged:
            left_meas_list.setModel(new MeasListModel());
            right_meas_list.setModel(new MeasListModel());
            // updateDisplay();
            
            break;
        }       
    }
    //END methods from ExprDataObserver   
    
    //BEGIN private methods
    
    private void cleanUp() {
        edata.removeObserver(this);
        if(frame != null) {
            frame.setVisible(false);
        }
    }
    
    private void addComponents() {
        JPanel o_panel;
        GridBagLayout o_gridbag;
        GridBagConstraints c;
        
        //Initialise frame and deal with windo closing
        frame = new JFrame("Lowess Normalise");
        //mview.decorateFrame(frame);
        frame.addWindowListener(new WindowAdapter() 
                          {
                              public void windowClosing(WindowEvent e)
                              {
                                  cleanUp();
                              }
                          });
         
                         
        //Initialise overall panel and constrains
        o_panel = new JPanel();
        o_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        o_gridbag = new GridBagLayout();
        o_panel.setLayout(o_gridbag);
        
        tabbed = new JTabbedPane();
        tabbed.setEnabled(false);
        
       c = new GridBagConstraints();
       c.gridx = 0;
        c.gridy = 0;
        c.weightx = 10.0;
        c.weighty = 9.0;
        c.fill = GridBagConstraints.BOTH;
        o_gridbag.setConstraints(tabbed, c);
        o_panel.add(tabbed);
        
        {
            // the measurements picking panel
            
            GridBagLayout gridbag = new GridBagLayout();
            
            DragAndDropPanel panel = new DragAndDropPanel();
            
            panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            
            panel.setLayout(gridbag);
            panel.setPreferredSize(new Dimension(500, 350));
            
            int n_cols = 2;
            int line = 0;
            
            left_meas_list = new DragAndDropList();
            right_meas_list = new DragAndDropList();
            
            JLabel leftLabel = new JLabel("Channel 1");
            JLabel rightLabel = new JLabel("Channel 2");
            
            JScrollPane left_jsp = new JScrollPane(left_meas_list);
            JScrollPane right_jsp = new JScrollPane(right_meas_list);
            
            left_meas_list.setModel(new MeasListModel());
            right_meas_list.setModel(new MeasListModel());
            
            left_meas_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            right_meas_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            //Add labels to the panel
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = line;
            c.gridwidth = 1;
            c.weightx = 4.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridbag.setConstraints(leftLabel,c);
            panel.add(leftLabel);
            c.gridx++;
            gridbag.setConstraints(rightLabel,c);
            panel.add(rightLabel);
            
            //Add lists to the panel
            line++;
            c.gridx = 0;
            c.gridy = line;
            c.weighty = 8.0;
            c.fill = GridBagConstraints.BOTH;
            gridbag.setConstraints(left_jsp, c);
            panel.add(left_jsp);
            c.gridx++;
            gridbag.setConstraints(right_jsp, c);
            panel.add(right_jsp);
            
            //Add apply filter checkbox to the panel
            line++;
            apply_filter_jchkb = new JCheckBox("Apply filter");
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = line;
            c.weightx = 1.0;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(apply_filter_jchkb, c);
            panel.add(apply_filter_jchkb);

            tabbed.add(" Pick Measurements ", panel);
        }
        
        
        

        
        ////////////HEY!
        // ===== options ======================================================

        {
            int line = 0;
            GridBagLayout gridbag = new GridBagLayout();
            
            JPanel panel = new JPanel();

            panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            
            panel.setLayout(gridbag);


            // --------------------------------------------------------------


            JPanel meth_panel = new JPanel();
            meth_panel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
            GridBagLayout meth_gridbag = new GridBagLayout();
            meth_panel.setLayout(meth_gridbag);

            JLabel label = new JLabel("Method ");
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            meth_gridbag.setConstraints(label, c);
            meth_panel.add(label);

            method_jcb = new JComboBox();//normaliser_names);
            /*
            if(selected_normaliser != null)
            {
                // work out which one...
                method_jcb.setSelectedItem( selected_normaliser.getName() );
            }

            method_jcb.addActionListener(new ActionListener() 
                {
                    public void actionPerformed(ActionEvent e) 
                    {
                        // save the parameters of the currently selected normaliser

                        if(selected_normaliser != null)
                        {
                            try
                            {
                                selected_normaliser.saveProperties(mview);
                            }
                            catch(NullPointerException npe)
                            {
                                // in case it never had its UI constructed
                            }
                        }

                        // and setup the newly selected one
                        String method = (String) method_jcb.getSelectedItem();
                        selected_normaliser = getNormaliserByName( method );
                        
                        setupNormaliser();
                    }
                });
             */
            c = new GridBagConstraints();
            c.gridx = 1;
            c.anchor = GridBagConstraints.WEST;
            meth_gridbag.setConstraints(method_jcb, c);
            meth_panel.add(method_jcb);

            c.gridx = 0;
            c.gridy = line++;
            c.weightx = 10.0;
            c.gridwidth = 3;
            c.anchor = GridBagConstraints.NORTH;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridbag.setConstraints(meth_panel, c);
            panel.add(meth_panel);

            // --------------------------------------------------------------


            ui_panel = new JPanel();
            // ui_panel.setBackground(Color.pink);
            ui_panel.setBorder(BorderFactory.createTitledBorder(" Columns to create "));
            
            //make a check box for whether to create a new ratio column
            //and a text box for specifying its name
            create_ratio_col_jchkb = new JCheckBox("Create Ratio Column", DEFAULT_CREATE_RATIO_COL);
            JLabel ratio_label = new JLabel("Ratio Column Name: ");
            ratio_colname_jtp = new JTextField("");
            ratio_colname_jtp.setEditable(DEFAULT_CREATE_RATIO_COL);
            create_ratio_col_jchkb.addChangeListener(new ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    ratio_colname_jtp.setEditable(create_ratio_col_jchkb.isSelected());
                }
            });
            
            //make a check box for whether to create a new expr column
            //and a text box for specifying its name
            create_intens_cols_jchkb = new JCheckBox("Create Intensity Columns", DEFAULT_CREATE_INT_COLS);
            create_intens_cols_jchkb.setEnabled(false);
            JLabel int1_lab= new JLabel("Column Name for Intensities 1: ");
            JLabel int2_lab= new JLabel("Column Name for Intensities 2: ");
            intens_colname1_jtp = new JTextField("");
            intens_colname2_jtp = new JTextField("");
            intens_colname1_jtp.setEditable(DEFAULT_CREATE_INT_COLS);
            intens_colname2_jtp.setEditable(DEFAULT_CREATE_INT_COLS);
            create_intens_cols_jchkb.addChangeListener(new ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    intens_colname1_jtp.setEditable(create_intens_cols_jchkb.isSelected());
                    intens_colname2_jtp.setEditable(create_intens_cols_jchkb.isSelected());
                }
            });
            
            GridBagLayout ui_layout = new GridBagLayout();
            ui_panel.setLayout(ui_layout);
            c = new GridBagConstraints();
            c.anchor = c.NORTHWEST;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weightx = 1.0;
            
            ui_layout.setConstraints(create_ratio_col_jchkb, c);
            ui_panel.add(create_ratio_col_jchkb);
            c.gridy++;
            ui_layout.setConstraints(ratio_label, c);
            ui_panel.add(ratio_label);
            c.gridy++;
            c.fill = c.HORIZONTAL;
            ui_layout.setConstraints(ratio_colname_jtp,c);
            ui_panel.add(ratio_colname_jtp);
            c.gridy++;
            c.gridy++;
            c.fill = c.NONE;
            ui_layout.setConstraints(create_intens_cols_jchkb,c);
            ui_panel.add(create_intens_cols_jchkb);
            c.gridy++;
            ui_layout.setConstraints(int1_lab, c);
            ui_panel.add(int1_lab);
            c.gridy++;
            c.fill = c.HORIZONTAL;
            ui_layout.setConstraints(intens_colname1_jtp,c);
            ui_panel.add(intens_colname1_jtp);
            c.gridy++;
            ui_layout.setConstraints(int2_lab, c);
            ui_panel.add(int2_lab);
            c.gridy++;
            c.fill = c.HORIZONTAL;
            ui_layout.setConstraints(intens_colname2_jtp,c);
            ui_panel.add(intens_colname2_jtp);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = line;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.NORTH;
            c.fill = GridBagConstraints.BOTH;
            gridbag.setConstraints(ui_panel, c);
            panel.add(ui_panel);

            Dimension fillsize = new Dimension(5,5);
            Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = line;
            gridbag.setConstraints(filler, c);
            panel.add(filler);

            info_text_area = new JTextPane();
            info_text_area.setMinimumSize(new Dimension(400, 300));
            info_text_area.setEditable(false);
            info_text_area.setFont(mview.getSmallFont());
            info_text_area.setBackground(ui_panel.getBackground());
            JScrollPane jsp = new JScrollPane(info_text_area);
            jsp.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));

            info_panel = new JPanel();
            info_panel.setBorder(BorderFactory.createTitledBorder(" Description "));
            GridBagLayout infobag = new GridBagLayout();
            info_panel.setLayout(infobag);
            c = new GridBagConstraints();
            c.weightx = 9.0;
            c.weighty = 9.0;
            c.fill = GridBagConstraints.BOTH;
            infobag.setConstraints(jsp, c);
            info_panel.add(jsp);

            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = line;
            c.weightx = 4.0;
            c.weighty = 4.0;
            c.fill = GridBagConstraints.BOTH;
            gridbag.setConstraints(info_panel, c);
            panel.add(info_panel);

            line++;

            // ============================================

            JPanel wrapper = new JPanel();
            wrapper.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
            GridBagLayout wrapbag = new GridBagLayout();
            wrapper.setLayout(wrapbag);
            
            
            JButton jb = new JButton("Normalise");
            c = new GridBagConstraints();
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.SOUTH;
            //c.fill = GridBagConstraints.HORIZONTAL;
            jb.addActionListener(new ActionListener() 
                {
                    public void actionPerformed(ActionEvent e) 
                    {
                        doNormalise(create_ratio_col_jchkb.isSelected(), create_intens_cols_jchkb.isSelected());
                    }
                });
            wrapbag.setConstraints(jb, c);
            wrapper.add(jb);


            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = line++;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridbag.setConstraints(wrapper, c);
            panel.add(wrapper);

            // ============================================


            tabbed.add(" Normalise ", panel);
        }
       
        
        // ======= buttons =============================================================

    {
            JPanel wrapper = new JPanel();
            GridBagLayout w_gridbag = new GridBagLayout();
            wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
            wrapper.setLayout(w_gridbag);
            nbutton  = new JButton("Next");
            bbutton  = new JButton("Back");
            bbutton.setEnabled(false);

            {
                wrapper.add(bbutton);
                
                bbutton.addActionListener(new ActionListener() 
                    {
                        public void actionPerformed(ActionEvent e) 
                        {
                            gotoPrevTab();
                        }
                    });
                
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.weighty = c.weightx = 1.0;
                //c.anchor = GridBagConstraints.EAST;
                w_gridbag.setConstraints(bbutton, c);
            }
            {
                wrapper.add(nbutton);
                
                nbutton.addActionListener(new ActionListener() 
                    {
                        public void actionPerformed(ActionEvent e) 
                        {
                            gotoNextTab();
                        }
                    });
                
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 0;
                c.weighty = c.weightx = 1.0;
                //c.anchor = GridBagConstraints.EAST;
                w_gridbag.setConstraints(nbutton, c);
            }
            {
                Dimension fillsize = new Dimension(16,16);
                Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = 0;
                w_gridbag.setConstraints(filler, c);
                wrapper.add(filler);
                                                   
            }
            {
                JButton button = new JButton("Close");
                wrapper.add(button);
                
                button.addActionListener(new ActionListener() 
                    {
                        public void actionPerformed(ActionEvent e) 
                        {
                            cleanUp();
                        }
                    });
                
                c = new GridBagConstraints();
                c.gridx = 3;
                c.gridy = 0;
                c.weighty = c.weightx = 1.0;
                //c.anchor = GridBagConstraints.EAST;
                w_gridbag.setConstraints(button, c);
            }
            {
                JButton button = new JButton("Help");
                wrapper.add(button);
                
                button.addActionListener(new ActionListener() 
                    {
                        public void actionPerformed(ActionEvent e) 
                        {
                            mview.getPluginHelpTopic("Normalise", "Normalise");
                        }
                    });
                
                c = new GridBagConstraints();
                c.gridx = 4;
                c.gridy = 0;
                c.weighty = c.weightx = 1.0;
                w_gridbag.setConstraints(button, c);
            }

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 10.0;
            o_gridbag.setConstraints(wrapper, c);
            o_panel.add(wrapper);
            
        }
        
        
        frame.getContentPane().add(o_panel);
                          
        
    }
    
    private void gotoPrevTab()
    {
        nbutton.setEnabled(true);
        if(--mode <= 0)
        {
            mode = 0;
            bbutton.setEnabled(false);
        }
        
        tabbed.setSelectedIndex(mode);
        
        //if(mode == 0)
        //doNothing...();
    }
    
        private void gotoNextTab()
    {
        bbutton.setEnabled(true);
        
        if(mode == 0)
        {
            final int[] left_si = left_meas_list.getSelectedIndices();
            final int[] right_si = right_meas_list.getSelectedIndices();
            
            if (left_si == null || right_si == null || left_si.length == 0 || right_si.length == 0) {
                mview.alertMessage("You must select one Measurement for each Channel");
                return;
            }
            
            getData();
            /*
            setupNormaliser();
            
            if(selected_normaliser != null)
            {
                if( nsi < selected_normaliser.minimumNumMeasurements())
                {
                    mview.alertMessage("You must select at least " + 
                                       selected_normaliser.minimumNumMeasurements() + 
                                       " Measurements");
                    return;
                }
            }
             **/
        }
        
        if(++mode >= 1)
        {
            mode = 1;
            nbutton.setEnabled(false);
        }
        
        tabbed.setSelectedIndex(mode);
        
    }
        
        private void getData() {
        int unfiltered = 0;
        data_id_to_spot_id = null;

        if(apply_filter_jchkb.isSelected())
        {
            // filtered, contruct new double[]s containing just the unfiltered spots
            //
            final int real_n_spots = edata.getNumSpots();
            
            for(int s=0; s < real_n_spots; s++)
                if(!edata.filter(s))
                    unfiltered++;
            
            data_id_to_spot_id = new int[unfiltered];
            
            // build an array mapping filtered index to 'real' index
            //
            
            int spot_n = 0;
            for(int s=0; s < real_n_spots; s++)
                if(!edata.filter(s))
                    data_id_to_spot_id[spot_n++] = s;
          
        }
        
        
            final int[] left_si = left_meas_list.getSelectedIndices();
            final int[] right_si = right_meas_list.getSelectedIndices();
            
            
            int n_meas = 2;
             
             
             data_id_to_meas_id = new int[n_meas];
        
        data = new double[n_meas][];

        measurement_names = new String[n_meas];
        
        left_measure_index = left_si[0];
             right_measure_index = right_si[0];
             left_measure_id = edata.getMeasurementAtIndex(left_measure_index);
             measurement_names[0] = edata.getMeasurementName( left_measure_id );
             right_measure_id = edata.getMeasurementAtIndex(right_measure_index);
             measurement_names[1] = edata.getMeasurementName( right_measure_id );
             data_id_to_meas_id[0] = left_measure_id;
             data_id_to_meas_id[1] = right_measure_id;
             
             if (apply_filter_jchkb.isSelected()) {
                 double[] left_raw_data = edata.getMeasurementData( left_measure_id );
                 double[] right_raw_data = edata.getMeasurementData( right_measure_id );
                 data[0] = new double[ unfiltered];
                 data[1] = new double[ unfiltered];
                 for (int s = 0 ; s < unfiltered; s++) {
                     data[0][s] = left_raw_data[data_id_to_spot_id[s]];
                     data[1][s] = right_raw_data[data_id_to_spot_id[s]];
                 }
             } else {
                 data[0]  = edata.getMeasurementData( left_measure_id );
                 data[1]  = edata.getMeasurementData( right_measure_id );
                 
             }
             
             
        }
        
       
        
        private void doNormalise(boolean createRatioCol, boolean createIntensityColumns) {
            String colName;
            if (!createRatioCol && !createIntensityColumns) {
                mview.alertMessage("No column types were selected, so no new columns were created");
                return;
            }
	    else
	    {
		MicroArrayIntensityLowess mil = new MicroArrayIntensityLowess();
		double[] correctedLgR = mil.normalize( data[0], data[1] );
		for( int i = 0; i < correctedLgR.length; i++ )
		{
		    if( data[0][i] > 0.0 && data[1][i] > 0.0 )
		    {
			double a = 0.5 * (Math.log( data[0][i] ) + Math.log( data[1][i] ));
			double m = Math.log( data[0][i] ) - Math.log( data[1][i] );
			System.out.println( i + "\t\t" + a + "\t\t" + "\t\t" + m + "\t\t" + correctedLgR[i] );
		    }
		}
            }
        
        }
    //END private methods
    
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
