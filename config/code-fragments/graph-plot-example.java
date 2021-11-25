
public void doIt()
{
   GraphFrame gframe = new GraphFrame( "Example Chart", 1);
       
   mview.decorateFrame( gframe );
        
   GraphPanel gpanel = gframe.getGraphPanel(0);
        
   //set axis labels and colours
   gpanel.getHorizontalAxis().setTitle( "Cats" );
   gpanel.getVerticalAxis().setTitle( "Dogs" );

   double[] xpoints = { -3, -1, 1, 3, 5, 7 };
   double[] ypoints = { 15, 17, 23, 18, 28, 29 };
   String[] labels  = { "One", "Two", "Three", "Four", "Five", "Six" };

   gpanel.getContext().addScatterPlot( xpoints, ypoints, labels, Color.blue, GraphPlot.FILLED_CIRCLE_GLYPH );
 
   gpanel.getHorizontalAxis().setTicks( new double[] { -5, 0, 5, 10 } );
   gpanel.getVerticalAxis().setTicks( new double[] { 10, 15, 20, 25, 30 }  );
 
   gframe.repaint();
}