import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;


public class GraphTest
{	
    final static int FEATURE_DEMO = 0;
    final static int AXIS_DEMO    = 1;

    final static int DEBUG        = 2;

    public GraphTest( int mode )
    {
	
	JFrame frame = new JFrame(); 

	GridBagConstraints c;
	GridBagLayout gridbag = new GridBagLayout();
	frame.getContentPane().setLayout(gridbag);

	if(mode == FEATURE_DEMO)
	{
	    double[] line_test_x = { 0.01, 0.1, 1, 10, 100, 1000, 10000 };
	    double[] line_test_y = { -800,-700,-600,-500,-400,-300,-200 };
	    
	    {
		
		GraphPanel gp1a  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(gp1a, c);
		frame.getContentPane().add(gp1a);
		
		gp1a.getContext().setBackgroundColour( new Color( 240,240,240 ));
		
		gp1a.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
		gp1a.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    	
		gp1a.getVerticalAxis().setTickSigDigits( 2 );
		gp1a.getVerticalAxis().setTitle("Linear scale");
		gp1a.getHorizontalAxis().setTickSigDigits( 2 );
		gp1a.getHorizontalAxis().setTitle("Linear scale");
	    	
		gp1a.getVerticalAxis().setLinear( );
		gp1a.getHorizontalAxis().setLinear( );
	      
		gp1a.getContext().setTitle( "Lin/Lin Plot" );
		
		gp1a.getContext().addLinePlot( line_test_x, line_test_y, Color.red );
	    }

	    {
		GraphPanel gp1b  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints(gp1b, c);
		frame.getContentPane().add(gp1b);
		
		gp1b.getContext().setBackgroundColour( new Color( 210,210,210 ));
		
		gp1b.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
		gp1b.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	      	
		gp1b.getVerticalAxis().setTickSigDigits( 2 );
		gp1b.getVerticalAxis().setTitle("Log scale");
		gp1b.getHorizontalAxis().setTickSigDigits( 2 );
		gp1b.getHorizontalAxis().setTitle("Log scale");
	      	
		gp1b.getVerticalAxis().setLog( );
		gp1b.getHorizontalAxis().setLog( );
		
		gp1b.getContext().setTitle( "Log/Log Plot" );
		
		gp1b.getContext().addLinePlot( line_test_x, line_test_y, Color.red );
		
	    }

	    {
		GraphPanel gp1c  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		gridbag.setConstraints(gp1c, c);
		frame.getContentPane().add(gp1c);
		
		gp1c.getContext().setBackgroundColour( new Color( 250,250,250 ));
		
		gp1c.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
		gp1c.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	      	
		gp1c.getVerticalAxis().setTickSigDigits( 2 );
		gp1c.getVerticalAxis().setTitle("Linear scale");
		gp1c.getHorizontalAxis().setTickSigDigits( 2 );
		gp1c.getHorizontalAxis().setTitle("Log scale");
		
	      	
		gp1c.getVerticalAxis().setLinear( );
		gp1c.getHorizontalAxis().setLog( );
		
		gp1c.getContext().setTitle( "Lin/Log Plot" );
		
		gp1c.getContext().addLinePlot( line_test_x, line_test_y, Color.red );
		
	    }
	    
	    {
		GraphPanel gp3  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		gridbag.setConstraints(gp3, c);
		frame.getContentPane().add(gp3);
		
		double[] test_x = { 1,2,3,4,5,6,7,8,9,10 };
		double[] test_y_ticks = { 0, 2.5, 5, 7.5, 10, 12.5, 15  };
		double[] test_y1 = { 4,7,3,2,5,5,8,10,14,11 };
		double[] test_y2 = { 6,5,5,3,6,7,10,11,13,12 };
		double[] test_y3 = { 5,7,8,5,5,9,9,12,15,13 };
		
		gp3.getContext().setBackgroundColour( new Color( 220,220,220 ));
		
		gp3.getVerticalAxis().setTicks(test_y_ticks);
		gp3.getHorizontalAxis().setTicks(test_x);
	      	
		gp3.getVerticalAxis().setTickSigDigits( 1 );
		gp3.getHorizontalAxis().setTickSigDigits( 0 );
	      	
		gp3.getVerticalAxis().setLinear( );
		gp3.getHorizontalAxis().setLinear( );
		
		gp3.getContext().setTitle( "Bar Chart" );
		
		gp3.getContext().setLegendAlignment( GraphPlot.ALIGN_LEFT, GraphPlot.ALIGN_TOP );
		
		gp3.getHorizontalAxis().setMouseTracking( false );
		
		gp3.getContext().addBarChart( "yellow", test_x, test_y1, Color.yellow );
		gp3.getContext().addBarChart( "blue", test_x, test_y2, Color.blue );
		gp3.getContext().addBarChart( "green", test_x, test_y3, Color.green );
		
	    }
	    
	    
	    {
		GraphPanel gp5  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		gridbag.setConstraints(gp5, c);
		frame.getContentPane().add(gp5);
		
		double[] test_x  = { 1,2,3,4,5,6,7 };
		double[] test_y_ticks = { 1, 10, 100, 1000, 10000 };
		double[] test_y1 = { 1, 4, 50, 110, 480, 990, 1500 };
		double[] test_y2 = { 2, 5, 45, 130, 400, 920, 1300 };
		
		gp5.getContext().setBackgroundColour( new Color( 250,250,250 ));
		
		gp5.getVerticalAxis().setTicks(test_y_ticks);
		gp5.getHorizontalAxis().setTicks(test_x);
	    	
		gp5.getVerticalAxis().setTickSigDigits( 0 );
		gp5.getHorizontalAxis().setTickSigDigits( 0 );
		
		gp5.getHorizontalAxis().setMouseTracking( false );
		
		gp5.getVerticalAxis().setLog( );
		
		gp5.getContext().setTitle( "Log Bar Char" );
		
		gp5.getContext().setLegendAlignment( GraphPlot.ALIGN_RIGHT, GraphPlot.ALIGN_TOP );
		
		gp5.getContext().addBarChart( "data 1", test_x, test_y1, Color.blue );
		gp5.getContext().addBarChart( "data 2", test_x, test_y2, Color.green );
	    }
	    
	    
	    {
		GraphPanel gp4  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		gridbag.setConstraints(gp4, c);
		frame.getContentPane().add(gp4);
		
		double[] test_x  = { 1,2,3,4,5,6,7,8,9,10 };
		double[] test_y1 = { 7,4,3,2,5,7,7,8,5,6 };
		double[] test_y2 = { 6,7,8,7,4,6,8,9,8,7 };
		double[] test_y3 = { 5.5,4.5,3.2,3.6,5.9,7.2,6.9,8.1,5.3,6.7 };
		
		gp4.getContext().setBackgroundColour( new Color( 210,210,210 ));
		
		gp4.getVerticalAxis().setTicks(test_x);
		gp4.getHorizontalAxis().setTicks(test_x);
	    	
		gp4.getVerticalAxis().setTickSigDigits( 3 );
		gp4.getVerticalAxis().setTitle("3 sig digits");
		gp4.getHorizontalAxis().setTickSigDigits( 0 );
		
		gp4.getHorizontalAxis().setMouseTracking( false );
		
		gp4.getVerticalAxis().setLinear( );
		gp4.getHorizontalAxis().setLinear( );
		
		gp4.getContext().setTitle( "Mixed Mode Plot" );
		
		gp4.getContext().setLegendAlignment( GraphPlot.ALIGN_RIGHT, GraphPlot.ALIGN_TOP );
		
		gp4.getContext().addBarChart( "data 1", test_x, test_y1, Color.pink );
		gp4.getContext().addLinePlot( "data 2", test_x, test_y2, Color.blue );
		gp4.getContext().addLinePlot( "data 3", test_x, test_y3, Color.magenta );
		
		
	    }
	    
	    {
		int n_revs = 5;
		int pts_per_rev = 18;
		
		int n_pts =  n_revs * pts_per_rev ;
		
		double dist_d = 0.1;
		double dist = 10.0;
		
		double ang = .0;
		double ang_d = (2 * Math.PI) / (double) pts_per_rev;
		
		double[] spiral_x = new double[n_pts];
		double[] spiral_y = new double[n_pts];
		
		for(int p=0; p < n_pts; p++)
		{
		    double c_ang = Math.cos(ang);
		    double s_ang = Math.sin(ang);
		    
		    spiral_x[p] = dist * s_ang;
		    spiral_y[p] = -dist * c_ang;
		    
		    dist -= dist_d;
		    
		    ang += ang_d;
		}
		
		
		{
		    GraphPanel gp6  = new GraphPanel();
		    c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 2;
		    gridbag.setConstraints(gp6, c);
		    frame.getContentPane().add(gp6);
		    
		    double[] ticks = { -10, 10 };
		    gp6.getHorizontalAxis().setTicks(ticks);
		    gp6.getVerticalAxis().setTicks(ticks);
		    
		    
		    gp6.getContext().setBackgroundColour( new Color( 240,240,240 ));
		    
		    gp6.getContext().setTitle( "Fixed Ticks" );
		    
		    gp6.getContext().addLinePlot( spiral_x, spiral_y, Color.blue );
		    
		}
		
		{
		    GraphPanel gp7  = new GraphPanel();
		    c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 2;
		    gridbag.setConstraints(gp7, c);
		    frame.getContentPane().add(gp7);
		    
		    gp7.getContext().setBackgroundColour( new Color( 210,210,210 ));
		    gp7.getHorizontalAxis().setTickMode(GraphAxis.AUTO_MODE);
		    gp7.getVerticalAxis().setTickMode(GraphAxis.AUTO_MODE);
		    
		    gp7.getContext().setTitle( "Auto Ticks 1" );
		    
		    gp7.getContext().addLinePlot( spiral_x, spiral_y, Color.blue );
		    
		}
		
		{
		    GraphPanel gp8  = new GraphPanel();
		    c = new GridBagConstraints();
		    c.gridx = 2;
		    c.gridy = 2;
		    gridbag.setConstraints(gp8, c);
		    frame.getContentPane().add(gp8);
		    
		    gp8.getContext().setBackgroundColour( new Color( 240,240,240 ));
		    
		    gp8.getContext().setTitle( "Auto Ticks 2" );
		    gp8.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_ZERO_MODE);
		    gp8.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_ZERO_MODE);
		    
		    gp8.getContext().addLinePlot( spiral_x, spiral_y, Color.blue );
		
		}
		
	    }
	}

	if(mode == AXIS_DEMO)
	{
	    double[] line_test_0_x = { -1000, -500, -100, 100, 500, 1000 };
	    double[] line_test_0_y = { -1000, -500, -100, 100, 500, 1000 };
	    
	    double[] line_test_1_x = { 0, 100, 500, 1000, 2000 };
	    double[] line_test_1_y = { 0, 100, 500, 1000, 2000 };
	    
	    double[] line_test_2_x = { -300, -250, -200, -150, -50, 0 };
	    double[] line_test_2_y = { -400, -300, -275, -250, -225, -200 };
		
	    double[] line_test_3_x = { 50, 60, 80, 90, 100, 110 };
	    double[] line_test_3_y = { -200, -100, -50, 50, 100, 200 };
	    
	    String[] title = { "mixed", "all pos", "all neg", "mixed" };

	    double[][] line_x = { line_test_0_x, line_test_1_x, line_test_2_x, line_test_3_x };
	    double[][] line_y = { line_test_0_y, line_test_1_y, line_test_2_y, line_test_3_y };

	    for(int l=0; l < 4; l++)
	    {
		GraphPanel gp  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = l;
		c.gridy = 0;
		gridbag.setConstraints(gp, c);
		frame.getContentPane().add(gp);
		
		gp.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
		gp.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    	
		gp.getVerticalAxis().setTickSigDigits( 2 );
		gp.getHorizontalAxis().setTickSigDigits( 2 );
		 
		gp.getVerticalAxis().setLinear( );
		gp.getHorizontalAxis().setLinear( );
	      	  
		gp.getContext().setTitle( title[l] + " : Lin/Lin Plot" );
		
		gp.getContext().addLinePlot( line_x[l], line_y[l], Color.red );

		 gp  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = l;
		c.gridy = 1;
		gridbag.setConstraints(gp, c);
		frame.getContentPane().add(gp);
		
		gp.getVerticalAxis().setTickMode(GraphAxis.AUTO_MODE);
		gp.getHorizontalAxis().setTickMode(GraphAxis.AUTO_MODE);
	    	
		gp.getVerticalAxis().setTickSigDigits( 2 );
		gp.getHorizontalAxis().setTickSigDigits( 2 );
		 
		gp.getVerticalAxis().setLinear( );
		gp.getHorizontalAxis().setLinear( );
	      	  
		gp.getContext().setTitle( title[l] + " : Auto ticks" );
		
		gp.getContext().addLinePlot( line_x[l], line_y[l], Color.red );


		gp  = new GraphPanel();
		c = new GridBagConstraints();
		c.gridx = l;
		c.gridy = 2;
		gridbag.setConstraints(gp, c);
		frame.getContentPane().add(gp);
		
		gp.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
		gp.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    	
		gp.getVerticalAxis().setTickSigDigits( 2 );
		gp.getHorizontalAxis().setTickSigDigits( 2 );
		  
		gp.getVerticalAxis().setLog( );
		gp.getHorizontalAxis().setLog( );
	      	  
		gp.getContext().setTitle( title[l] + " : Log/Log Plot" );
		
		gp.getContext().addLinePlot( line_x[l], line_y[l], Color.red );
	    }
	}


	if(mode == DEBUG)
	{
	    GraphPanel gp  = new GraphPanel();
	    c = new GridBagConstraints();
	    gridbag.setConstraints(gp, c);
	    frame.getContentPane().add(gp);
	    
	    //gp.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    //gp.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    
	    gp.getVerticalAxis().setTickSigDigits( 4 );
	    gp.getHorizontalAxis().setTickSigDigits( 2 );
	    
	    gp.getVerticalAxis().setTitle( "penguins/hour" );
	    gp.getHorizontalAxis().setTitle( "mushrooms/person" );
	    
	    gp.getVerticalAxis().setTickMode(GraphAxis.AUTO_MODE);
	    gp.getHorizontalAxis().setTickMode(GraphAxis.AUTO_MODE);
	        
	    //gp.getVerticalAxis().setLog( );
	    //gp.getHorizontalAxis().setLog( );
	    
	    gp.getContext().setTitle( "Tick test" );
	    
	    //double[] line_x = { -300, -250, -200, -150, 50, 150  };
	    //double[] line_x = { -300, -250, -200, -150, -100, 0  };
	    double[] line_x = { 10,20.,30,40,50,60. };
	    //double[] line_x = { 1,2,30,45,50,109 };
	    //double[] line_y = { -400, -300, -275, -250, -225, -200 };
	    //double[] line_y = { 10.04, 11.34, 13.32, 12.96, 10.09, 10.56 };
	    double[] line_y = { 0.001, 0.003, 0.002, 0.0025, 0.003, 0.0015 };
	    double[] line_y2 = { 0.002, 0.0025, 0.003, 0.0015, 0.0012, 0.0014 };
	    double[] line_y3 = { 0.002, 0.0018, 0.0015, 0.0010, 0.0007, 0.0002 };
    
	    gp.getContext().addLinePlot( "red", line_x, line_y, Color.red,   GraphPlot.CIRCLE_GLYPH );
	    gp.getContext().addLinePlot( "blue", line_x, line_y2, Color.blue, GraphPlot.CIRCLE_GLYPH );
	    gp.getContext().addLinePlot( "green", line_x, line_y3, Color.green, GraphPlot.CIRCLE_GLYPH );

	}

	frame.pack();
	frame.setVisible(true);
    }


    public static void main(String[] args) 
    {		
	//new GraphTest( AXIS_DEMO );

	new GraphTest( FEATURE_DEMO );

	//new GraphTest( DEBUG );
    }

}
