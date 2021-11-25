/*
 * QCDisplayPanel.java
 *
 * Created on 16 June 2004, 16:33
 */
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
/**
 *Displays the results from a QC chart calculation.
 *The results are displayed in a table and on a graph.
 * @author  hhulme
 */
public class QCResultsGraphDisplay  {
    //reference to the parent maxd view
    private maxdView mview;
    
    /** Creates a new instance of QCDisplayPanel */
    public QCResultsGraphDisplay(QCResults results, maxdView _mview) {
        
        mview = _mview;
        
        

        
        
        //show the results graphically in another withdow.
        showPlot(results);
    }
    
    
    /**Display the reulsts graphically*/
    private void showPlot( QCResults results ) {
        String[] groups = results.getGroupNames();
        int numGroups = groups.length;
        double[] groupLabelXpos = new double[numGroups];
        double[] groupLabelYpos = new double[numGroups];
        
        
        //record where should place vertical lines between groups
        double[] vertLinePoints = new double[numGroups - 1];
        
        //create and decorate the frame
        GraphFrame gframe = new GraphFrame( mview, "QC Results Plot", 1);
       
	GraphPanel gpanel = gframe.getGraphPanel(0);
        
         
        //set axis labels and colours
        gpanel.getHorizontalAxis().setTitle( "slide" );
        gpanel.getVerticalAxis().setTitle( "log (base 2) of variance of difference from mean" );
        gpanel.getContext().setBackgroundColour( mview.getBackgroundColour() );
        gpanel.getContext().setForegroundColour( mview.getTextColour() );
        
        //keep count of slides
        int slideNum = 0;
        
        //add data points one group at a time
        for (int i = 0 ; i < numGroups ; i++) {
            String group = groups[i];
            String[] cols = results.getCols(group);
            int numSlidesInGroup = cols.length;
            
            
            
            double[] xpoints = new double[cols.length];
            double[] ypoints = new double[cols.length];
            
            
            
            
            
            
            for (int j = 0 ; j < cols.length ; j++) {
                xpoints[j] = slideNum+1.0*(j+1);
                ypoints[j] = Math.log(results.getValue(cols[j]))/Math.log(2.0);
                
            }
            groupLabelXpos[i] = (xpoints[0] + xpoints[cols.length-1])/2;
            
            
            
            gpanel.getContext().addScatterPlot( xpoints, ypoints, cols, mview.getTextColour().brighter(), GraphPlot.CIRCLE_GLYPH );
            //move slideNum
            slideNum = slideNum + numSlidesInGroup;
            if (i!=numGroups-1) {
                vertLinePoints[i] = slideNum+0.5;
            }
            
            
        }
        
        
        //Add horizontal lines for Centre line, Upper and Lower Control Lines
        
        double minx = 0;//gpanel.getContext().xmin;
        double maxx = gpanel.getContext().xmax;
        
        double centreLineY = results.getMeanOfLoggedVars();
        double stdev = results.getStdevOfLoggedVars();
        
        
        gpanel.getContext().addLinePlot("CL", new double[]{minx, maxx}, new double[]{centreLineY, centreLineY}, Color.white);
        
        QCControlLineSet controlLineSet = results.getPrimaryControlLineSet();
        
        double ucl = controlLineSet.upperControlLine;
        double lcl = controlLineSet.lowerControlLine;
        
        double cl_bottom = controlLineSet.confidenceBarCentreLine[0];
        double cl_top = controlLineSet.confidenceBarCentreLine[1];
        
        double ucl_bottom = controlLineSet.confidenceBarUCL[0];
        double ucl_top = controlLineSet.confidenceBarUCL[1];
        
        double lcl_bottom = controlLineSet.confidenceBarLCL[0];
        double lcl_top = controlLineSet.confidenceBarLCL[1];
        
        double alpha_mu = controlLineSet.sigLevel_mu;
        double alph_sig = controlLineSet.sigLevel_sigma;
        
        gpanel.getContext().addLinePlot("LCL, UCL=" + controlLineSet.sigLevel , new double[]{minx, maxx}, new double[]{lcl, lcl}, Color.RED);
        gpanel.getContext().addLinePlot(new double[]{minx, maxx}, new double[]{ucl, ucl}, Color.RED);
        
        
        gpanel.getContext().addLinePlot("UCL confidence", new double[]{minx, maxx}, new double[]{ucl_top, ucl_top}, Color.PINK);
        gpanel.getContext().addLinePlot(new double[]{minx, maxx}, new double[]{ucl_bottom,ucl_bottom}, Color.PINK);
        
        gpanel.getContext().addLinePlot("CL confidence", new double[]{minx, maxx}, new double[]{cl_top, cl_top}, Color.YELLOW);
        gpanel.getContext().addLinePlot(new double[]{minx, maxx}, new double[]{cl_bottom, cl_bottom}, Color.YELLOW);
        
       
        
        gpanel.getContext().addLinePlot("LCL confidence", new double[]{minx, maxx}, new double[]{lcl_top, lcl_top}, Color.GREEN);
        gpanel.getContext().addLinePlot(new double[]{minx, maxx}, new double[]{lcl_bottom, lcl_bottom}, Color.GREEN);
        
       
        
        //Add vertical Lines to divide groups
        
        double miny = gpanel.getContext().ymin-0.1;
        double maxy = gpanel.getContext().ymax;
        
        for (int i = 0 ; i < vertLinePoints.length ; i++) {
            gpanel.getContext().addLinePlot(new double[]{vertLinePoints[i], vertLinePoints[i]}, new double[]{miny, maxy}, Color.white);
        }
        
        //label groups
        for (int i = 0 ; i < numGroups ; i++) {
            groupLabelYpos[i] = (maxy+miny)/2;
        }
        //gpanel.getContext().addScatterPlot(groupLabelXpos, groupLabelYpos, groups, Color.WHITE, GraphPlot.NO_GLYPH);
        
        gpanel.getVerticalAxis().setTicks(new double[] {lcl, centreLineY, ucl});
        double[] xticks = new double[slideNum];
        for (int i = 0 ; i < slideNum ; i++) {
            xticks[i] = i+1;
        }
        gpanel.getHorizontalAxis().setTicks(xticks);
        
        StringBuffer buf = new StringBuffer();
        buf.append("Slides in group");
        if (numGroups > 1) {
            buf.append("s");
        }
        buf.append(" ");
        for (int i = 0 ; i<numGroups-1 ; i++) {
            buf.append(groups[i]);
            buf.append(",");
        }
        buf.append(groups[numGroups-1]);
        gpanel.getHorizontalAxis().setTitle(buf.toString());
    }
}
