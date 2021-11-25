import java.util.Vector;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteExprDataInterface extends Remote 
{
    public ExprData getRemoteExprData() throws RemoteException;

    public double eValue(int m, int s)              throws RemoteException;

    // =============================================================================================
    // spot data and spot indexing
    // =============================================================================================
    //
    public int getNumSpots()        throws RemoteException;
    public String getSpotName(int s)   throws RemoteException;
    public String getProbeName(int s)   throws RemoteException;            
    public String[] getGeneNames(int s)   throws RemoteException;            
    public String getGeneName(int s) throws RemoteException;
    
    public int getSpotAtIndex(int index) throws RemoteException;
    public int getIndexOfSpot(int spot) throws RemoteException;
    public int getIndexBySpotName(String name) throws RemoteException;
    
    public int[] getSpotSelection() throws RemoteException;

    public boolean filter(int spot_id) throws RemoteException;

    // =============================================================================================
    // measurements
    // =============================================================================================
    //
    public int getNumMeasurements() throws RemoteException;

    public String getMeasurementName(int m) throws RemoteException;              	
    public double[] getMeasurementData(int m) throws RemoteException;

    public int getMeasurementAtIndex(int ind) throws RemoteException;
    public int getIndexOfMeasurement(int m)  throws RemoteException;

    //  public ExprData.MeasurementHandle createMeasurementHandle(String name int name_mode, Vector elems);
    //  public boolean addMeasurementHandle(ExprData.MeasurementHandle new_meas) throws RemoteException;

    // =============================================================================================
    // clusters
    // =============================================================================================
    //
    // note: handles are used to refer to clusters remotely. A handle (int) is 
    //   allocated when the object is created. This handle is used to refer to the 
    //   object in subsequent remote methods
    //
    public int getNumClusters() throws RemoteException;

    public ExprData.ClusterHandle getRootClusterHandle() throws RemoteException;

    // create a new cluster with the specified parent (no ClusterUpdate event generated)
    public ExprData.ClusterHandle createClusterHandle(ExprData.ClusterHandle parent,
						      String name, 
						      int name_mode, 
						      Vector elems) throws RemoteException;

    // create a new a parent-less cluster (no ClusterUpdate event generated)
    public ExprData.ClusterHandle createClusterHandle(String name, 
						      int name_mode, 
						      Vector elems) throws RemoteException;
    
    // adds a child to the specified parent (generates a ClusterUpdate event)
    public void addClusterHandle(ExprData.ClusterHandle parent, ExprData.ClusterHandle cl) throws RemoteException;

    // =============================================================================================
    // events
    // =============================================================================================

    public void generateClusterUpdate(int event) throws RemoteException;
    public void generateMeasurementUpdate(int event) throws RemoteException;
    public void generateDataUpdate(int event) throws RemoteException;
    public void generateEnvironmentUpdate(int event) throws RemoteException;

    // =============================================================================================
    // observers and listeners
    // =============================================================================================

    // note: handles are used to refer to observers and listeners. A handle (int) is 
    //   allocated hen the object is added. This handle is used to refer to the object
    //   in the remove method
    //

    // events
    //
    public int addRemoteDataObserver(ExprData.RemoteExprDataObserver redo) throws RemoteException;
    public void removeRemoteDataObserver(int redo_handle) throws RemoteException;

    // data sinks
    //
    public int addRemoteDataSink(ExprData.RemoteDataSink rds) throws RemoteException;
    public void removeRemoteDataSink(int rds_handle) throws RemoteException;


    // selection listener
    //
    public int addRemoteSelectionListener(ExprData.RemoteSelectionListener rsl) throws RemoteException;
    public void removeRemoteSelectionListener(int rsl_handle) throws RemoteException;

    
}

