import javax.swing.*;

public interface Normaliser 
{
    //
    // this method should return the name of this Normaliser
    //
    public String getName();
    

    //
    // the result of this method defines whether this 
    // Normaliser can handle data containing NaN values
    //
    // if false, the Normalise plugin will not allow data
    // with NaN values to be used with this Normaliser
    //
    public boolean canHandleNaNs();


    //
    // should return the minimum number of Measurements that
    // this Normaliser needs to operate correctly
    //
    public int getMinimumNumMeasurements();



    //
    // !! NEW METHOD !!
    //
    // should return the name of the html file containing the help documentation
    // for this Normaliser, or 'null' if no documentation exists.
    //
    // note: the .html extension should not be included in the name
    //
    public String getHelpDocumentName();


    //
    // !! NEW METHOD !!
    //
    // should return the maximum number of Measurements that
    // this Normaliser can handle, or 0 if no maximum
    //
    public int getMaximumNumMeasurements();


    //
    // !! NEW METHOD !!
    //
    // should return the maximum number of Measurements that
    // this Normaliser can handle, or 0 if no maximum
    //
    public int getNumberOfReturnedMeasurements( int number_of_measurements_given );



    //
    // !! NEW METHOD !!
    //
    // return true if this normaliser returns one Meaurement for each of the
    // Measurements that are input, i.e. there is a one-to-one mapping from 
    // input to output.
    //
    public boolean isOneToOne( );


    
    //
    // this method should return a JPanel containing the user interface (UI)
    // components that this Normaliser needs.
    // 
    // return null if this Normaliser has no UI
    //
    // the maxdView object is passed so that the application properties
    // can be queried for saved parameters (see below)
    //
    // the data matrix is passed so that the Normaliser can adjust its
    // parameters to match the data values. do not modify this data!
    //
    public JPanel getUI(final maxdView mview, final NormaliserInput input );


    //
    // this method should return a String describing how the
    // Normaliser operates and what any parameters mean.
    //
    public String getInfo();


    //
    // !! NEW METHOD !!
    //
    // this method should return a String describing the current
    // settings of the Normaliser
    //
    // this string will be saved as an attribute in any Measurements that are created
    // 
    public String getSettings();


    //
    // this method contains the actual normalisation code
    //
    // the data is organised by Measurements:
    //
    //    data[i][j] is the expression in Measurement 'i' of Spot 'j'
    // 
    //    the Measurements are those that were selected in the plugin's Measurement list
    //
    //    the Spots have already been filtered (if any filter was applied).
    //
    //    this method should return a 2d array of doubles with the same dimensions
    //    as the input array. DO NOT MODIFY THE INPUT ARRAY!
    //
    //    the Measurement names are provided in case you wish to report
    //    any extra information to the user (e.g. statistics about each Measurement)
    //
    public NormaliserOutput normalise( NormaliserInput input );


    //
    // this method will be called when the Normalise plugin thinks that this
    // Normaliser should save its properties.
    //
    // this method should save any properties that you want to be able to
    // retrieve next time.
    //
    // (you don't have to do anything in this method if you don't want to)
    //
    public void  saveProperties(maxdView mview);


    //
    // !! NEW METHODS !!
    //
 
    //
    // if you want the normaliser to be available via the plugin
    // command mechanism, then define this method to return a suitable command definition
    //
    // return null if you do not wish to add a command for this normaliser
    //
    // (see the SampleNormaliser.java for an example of how to write this method)
    //
    public PluginCommand getCommand();

    //
    // if you have registered a plugin command with the above method, then this
    // method will be called when the command is invoked.
    //
    // this method should set normalisation parameters (and the UI components) that
    // have been supplied as arguments in the plugin command.
    //
    // provide an empty definition if you have not registered a plugin command.
    // 
    // (see the SampleNormaliser.java for an example of how to write this method)
    //
    public void parseArguments( final maxdView mview, final String args[] );

}
