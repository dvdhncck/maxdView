/*	
 * 	Export maxdView data to the Weka ARFF (Attribute-Relation File Format)
 * 
 *	@version 0.1
 *  @author Kieran Holland, holki659@student.otago.ac.nz
 */

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Vector;
import java.lang.*;
import javax.swing.border.*;
import ExprData;

public class ExportToARFF implements ExprData.ExprDataObserver, Plugin {

	/*
	 * Plug-in methods
	 */

	public ExportToARFF(maxdView mview_) {
		mview = mview_;
		edata = mview.getExprData();
		dplot = mview.getDataPlot();
	}

	public void startPlugin() {
		frame = new JFrame("Export Weka Dataset");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				cleanUp();
			}
		});

		export_panel = new JPanel();
		export_panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		frame.getContentPane().add(export_panel, BorderLayout.CENTER);
		GridBagLayout gridbag = new GridBagLayout();
		export_panel.setLayout(gridbag);
		GridBagConstraints c;
		int line = 0;

		// Options Dialog
		{
			JLabel label = new JLabel("Dataset Name  ");
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(label, c);
			export_panel.add(label);
			
			relation = new JTextField(mview.getProperty("exportToARFF.relation_name", "Anonymous"));
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.fill = GridBagConstraints.HORIZONTAL;
			gridbag.setConstraints(relation, c);
			export_panel.add(relation);
			line++;
		}
		{
			JLabel label = new JLabel("Export Measurement Labels  ");
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(label, c);
			export_panel.add(label);

			incl_meas_jchkb = new JCheckBox("");
			incl_meas_jchkb.setSelected(mview.getBooleanProperty("exportToARFF.incl_meas", true));
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(incl_meas_jchkb, c);
			export_panel.add(incl_meas_jchkb);
			line++;
		}		
		{
			JLabel label = new JLabel("Export Measurement Attributes  ");
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = line;
			c.anchor = GridBagConstraints.NORTHWEST;
			gridbag.setConstraints(label, c);
			export_panel.add(label);

			meas_attr_labels_list = new JList();
			populateMeasAttrsList();
			JScrollPane jsp = new JScrollPane(meas_attr_labels_list);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.fill = GridBagConstraints.BOTH;
			gridbag.setConstraints(jsp, c);
			export_panel.add(jsp);
			line++;
		}
		{
			JPanel inner = new JPanel();
			inner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			JButton all = new JButton("All");
			all.setMargin(new Insets(0, 2, 0, 2));
			all.setFont(mview.getSmallFont());
			inner.add(all);
			JButton none = new JButton("None");
			none.setFont(mview.getSmallFont());
			none.setMargin(new Insets(0, 2, 0, 2));
			inner.add(none);
			all.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					meas_attr_labels_list.addSelectionInterval(0, meas_attr_labels_list.getModel().getSize() - 1);
				}
			});
			none.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					meas_attr_labels_list.clearSelection();
				}
			});
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.anchor = GridBagConstraints.EAST;
			gridbag.setConstraints(inner, c);
			export_panel.add(inner);
			line++;
		}
		{
			JLabel label = new JLabel("Significant digits  ");
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = line;
			c.anchor = GridBagConstraints.NORTHWEST;
			gridbag.setConstraints(label, c);
			export_panel.add(label);

			sig_dig_slider = new LabelSlider(null, JSlider.VERTICAL, JSlider.HORIZONTAL, 0, 32, 6);
			sig_dig_slider.setMode(LabelSlider.INTEGER);
			sig_dig_slider.setValue(mview.getIntProperty("exportToARFF.sig_digs", 6));
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(sig_dig_slider, c);
			export_panel.add(sig_dig_slider);
			line++;
		}
		{	
			JLabel label = new JLabel("Apply Filter  ");
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(label, c);
			export_panel.add(label);

			apply_filter_jchkb = new JCheckBox("");
			apply_filter_jchkb.setSelected(mview.getBooleanProperty("exportToARFF.apply_filter", true));
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(apply_filter_jchkb, c);
			export_panel.add(apply_filter_jchkb);
			line++;
		}

		// Control buttons
		{
			JButton jb = new JButton("Export");
			jb.setToolTipText("Ready to choose a filename...");
			jb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					File file = pickFileName();
					if (file != null)
						exportData(file);
					saveProps();
				}
			});
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = line;
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(jb, c);
			export_panel.add(jb);

			JPanel inner = new JPanel();
			inner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//			GridBagLayout innerbag = new GridBagLayout();
//			innerexport_panel.setLayout(innerbag);
			jb = new JButton("Help");
			jb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					mview.getPluginHelpTopic("ExportToARFF", "ExportToARFF");
				}
			});
			inner.add(jb);

			jb = new JButton("Close");
			jb.setToolTipText("Close this dialog...");
			jb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveProps();
					cleanUp();
				}
			});
			inner.add(jb);
			c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = line;
			c.anchor = GridBagConstraints.EAST;
			gridbag.setConstraints(inner, c);			
			export_panel.add(inner);
		}
		mview.getExprData().addObserver(this);
		frame.pack();
		frame.setVisible(true);
	}

	public void stopPlugin() {
		cleanUp();
	}

	public void cleanUp() {
		mview.getExprData().removeObserver(this);
		frame.setVisible(false);
	}

	public PluginInfo getPluginInfo() {
		PluginInfo pinf = new PluginInfo("Export Weka Dataset", "exporter", "Write data in a Weka ARFF format", "", 1, 1, 0);
		return pinf;
	}

	private void saveProps() {
		mview.putBooleanProperty("exportToARFF.apply_filter", apply_filter_jchkb.isSelected());
		mview.putIntProperty("exportToARFF.sig_dig", (int) sig_dig_slider.getValue());
		mview.putProperty("exportToARFF.relation_name", relation.getText());
	}

	public PluginCommand[] getPluginCommands() {
		return null;
	}

	public void runCommand(String name, String[] args, CommandSignal done) {
	}

	private File pickFileName() {
		JFileChooser fc = mview.getFileChooser();
		fc.addChoosableFileFilter(new ARFFFilter());
		fc.setCurrentDirectory(new File(mview.getProperty("exportToARFF.current_directory", ".")));
		int returnVal = fc.showSaveDialog(export_panel);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = mview.getFileChooser().getSelectedFile();
			if (!file.getName().endsWith(".arff")) file = new File(file.getAbsoluteFile() + ".arff");
			if (file.getParent() != null)
				mview.putProperty("exportToARFF.current_directory", file.getParent());
			if (file.exists()) {
				if (mview.infoQuestion("File exists, overwrite?", "No", "Yes") == 0)
					return null;
			}
			return file;
		}
		return null;
	}

	/*
	 *	Export methods
	 */

	public void exportData(File file) {
		try {
			BufferedWriter writer = null;
			writer = new BufferedWriter(new FileWriter(file));
			int lines = writeData(writer, -1);
			writer.close();
			if (report_status)
				mview.successMessage(lines + " measurements written to " + file.getName());
		} catch (java.io.IOException ioe) {
			mview.alertMessage("Unable to write\n" + ioe);
		}
	}

	// Write the data to a Writer 
	public int writeData(Writer writer, int max_lines) {
		int meas_count = 0;
		int spot_count = 0;
		int good_lines = 0;
		try {
			// relation
			writer.write("% \n" + "% Weka ARFF formatted dataset\n" + "% Exported from maxdView\n" + "% \n");
			writer.write("@relation '" + relation.getText() + "'\n\n");

			// measurement attributes
			Object[] meas_attr_sel = meas_attr_labels_list.getSelectedValues();
			if (incl_meas_jchkb.isSelected()) {
				Vector meas_labels = getMeasurementLabels();
				String uniq_meas = getUniqStrings(meas_labels);
				writer.write("@attribute 'Measurement' {" + uniq_meas + "}\n\n");
			}
			if (meas_attr_sel != null) {
				Vector meas_attrs;
				for (int a = 0; a < meas_attr_sel.length; a++) {
					meas_attrs = getMeasurementAttrs((String) meas_attr_sel[a]);
					String uniq_meas_attrs = getUniqStrings(meas_attrs);
					writer.write("@attribute '" + (String) meas_attr_sel[a] + "' {" + uniq_meas_attrs + "}\n");
					//					meas_count++;
				}
				writer.write("\n");
			}

			// data attributes	
			if (apply_filter_jchkb.isSelected()) {
				use_filter = ExprData.ApplyFilter;
			}
			ExprData.SpotIterator spots = edata.getSpotIterator(use_filter);
			try {
				String spot_ID;
				while (spots.isValid()) {
					spot_ID = edata.getSpotName(spots.getSpotID());
					//					System.out.print(spots.getSpotID());
					writer.write("@attribute '" + spot_ID + "' real\n");
					//					spot_count++;
					spots.next();
				}
				writer.write("\n");
			} catch (ExprData.InvalidIteratorException iie) {
			}

			// data	
			writer.write("@data\n");
			ExprData.MeasurementIterator measurements = edata.getMeasurementIterator(use_filter);
			try {
				double expr;
				String expr_str;
				Vector meas_attrs;
				ExprData.Measurement meas;
				boolean start;
				int sig_digs = (int) sig_dig_slider.getValue();
				while (measurements.isValid()) {
					start = true;
					meas = edata.getMeasurement(measurements.getMeasurementID());
					if (incl_meas_jchkb.isSelected()) {
						writer.write("'" + meas.getName() + "'");
						start = false;
					}
					for (int a = 0; a < meas_attr_sel.length; a++) {
						expr_str = meas.getAttribute((String) meas_attr_sel[a]);
						if (expr_str == null)
							expr_str = "?";
						else expr_str = "'" + expr_str + "'";
						if (start)
							start = false;
						else
							expr_str = ',' + expr_str;
						writer.write(expr_str);
					}
					spots = measurements.getSpotIterator(use_filter);
					while (spots.isValid()) {
						//						System.out.print(spots.getSpotID());					
						expr = spots.value();
						if (Double.isNaN(expr))
							expr_str = "?";
						else
							expr_str = mview.niceDouble(expr, 100, sig_digs + 1);
						if (start)
							start = false;
						else
							expr_str = ',' + expr_str;
						writer.write(expr_str);
						spots.next();
					}
					writer.write("\n");
					good_lines++;
					measurements.next();
				}
			} catch (ExprData.InvalidIteratorException iie) {
			}
		} catch (java.io.IOException ioe) {
			mview.alertMessage("Unable to write file\n\n" + ioe);
		}
		//		System.out.println("Lines: " + good_lines + " Meas_attr: " + meas_count + " Spot_attr: " + spot_count);
		return good_lines;
	}

	public String tidyName(String n) {
		String t = n.trim();
		String r = t.replace('\t', '_');
		return r.replace(' ', '_');
	}

	// build a list of all possible attrs in all (selected?) Measurements
	public Vector getMeasurementLabels() {
		Vector result = new Vector();
		ExprData.MeasurementIterator meas = edata.getMeasurementIterator(use_filter);
		try {
			String meas_label;
			while (meas.isValid()) {
				meas_label = edata.getMeasurement(meas.getMeasurementID()).getName();
				result.add(meas_label);
				meas.next();
			}
		} catch (ExprData.InvalidIteratorException iie) {
		}
		return result;
	}

	// build a list of all possible attrs in all (selected?) Measurements
	public void populateMeasAttrsList() {
		java.util.HashSet uniq = new java.util.HashSet();
		for (int m = 0; m < edata.getNumMeasurements(); m++) {
			ExprData.Measurement ms = edata.getMeasurement(m);
			for (java.util.Enumeration e = ms.getAttributes().keys(); e.hasMoreElements();) {
				final String name = (String) e.nextElement();
				uniq.add(name);
			}
		}
		Vector vec = new Vector();
		java.util.Iterator it = uniq.iterator();
		while (it.hasNext())
			vec.addElement(it.next());
		String[] all_attr_names = (String[]) vec.toArray(new String[0]);
		java.util.Arrays.sort(all_attr_names);
		meas_attr_labels_list.setListData(all_attr_names);
	}

	// create a Vector containing the values for the specified MeasurementAttribute
	public Vector getMeasurementAttrs(String name) {
		Vector result = new Vector();
		ExprData.MeasurementIterator meas = edata.getMeasurementIterator(use_filter);
		try {
			String attr_str;
			while (meas.isValid()) {
				attr_str = edata.getMeasurement(meas.getMeasurementID()).getAttribute(name);
				if (attr_str != null)
					result.add(attr_str);
				meas.next();
			}
		} catch (ExprData.InvalidIteratorException iie) {
		}
		return result;
	}

	// find unique strings in Vector for output in ARFF attribute list
	public String getUniqStrings(Vector list) {
		String result = "";
		//		System.out.println("List: " + list);
		java.util.HashSet uniq = new java.util.HashSet();
		java.util.Iterator it = list.iterator();
		while (it.hasNext())
			uniq.add(it.next());
		Vector vec = new Vector();
		it = uniq.iterator();
		while (it.hasNext())
			vec.addElement(it.next());
		String[] uniqs = (String[]) vec.toArray(new String[0]);
		java.util.Arrays.sort(uniqs);
		for (int i = 0; i < uniqs.length; i++) {
			if (i > 0)
				result += ",";
			result += "'" + uniqs[i] + "'";
		}
		return result;
	}

	// Expression Data Observer
	public void dataUpdate(ExprData.DataUpdateEvent due) {
		populateMeasAttrsList();
	}

	public void measurementUpdate(ExprData.MeasurementUpdateEvent mue) {
	}

	public void clusterUpdate(ExprData.ClusterUpdateEvent cue) {
	}

	public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue) {
	}

	// Declarations
	private maxdView mview;
	private ExprData edata;
	private DataPlot dplot;

	private JFrame frame = null;
	private JPanel export_panel;
	private JTextField relation;
	private JList meas_attr_labels_list;
	private JCheckBox apply_filter_jchkb, incl_meas_jchkb;
	private LabelSlider sig_dig_slider;
	private int use_filter;
	private boolean report_status = true;
}

class ARFFFilter extends javax.swing.filechooser.FileFilter {
	public boolean accept(File f) {
		if (f.isDirectory() || f.getName().endsWith(".arff"))
			return true;
		else
			return false;
	}
	public String getDescription() {
		return "Weka Dataset";
	}
}