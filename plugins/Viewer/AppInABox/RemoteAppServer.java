import java.io.*;
import java.net.*;

public class RemoteAppServer
{
    public static final boolean debug_start_stop = false;
    public static final boolean debug_comms      = false;

    //public static final boolean debug_start_stop = true;
    //public static final boolean debug_comms      = true;

    private static final int default_host_port = 28282;

    private static final int AppStartedOK       = 0;
    private static final int AppNotFound        = 1;
    private static final int AppWillNotStart    = 2;
    private static final int AppStartNotAllowed = 3;

    public static void main(String[] args) throws Exception 
    {
	RemoteAppServer ras = new RemoteAppServer();
	ras.beginListenRecvLoop(default_host_port);
    }

    public int input_count = 0;
    public int output_count = 0;

    public void beginListenRecvLoop(final int host_port)
    {
	ServerSocket server_socket = null;

	while(true)
	{
	    try 
	    {
	  	server_socket = new ServerSocket(host_port);
	    } 
	    catch (IOException e) 
	    {
	  	System.err.println("unable to listen on port: " + host_port);
	  	System.exit(-23);
	    }
    
	    System.out.println("listening on port: " + host_port);
    
	    Socket client_socket = null;
	    try 
	    {
	  	client_socket = server_socket.accept();
	    } 
	    catch (IOException e) 
	    {
	  	System.out.println("accept failed on port: " + host_port);
	  	System.exit(-1);
	    }

	    System.out.println("client has connected from " + client_socket.getInetAddress());
	    
	    try 
	    {
		PrintWriter client_out = new PrintWriter(client_socket.getOutputStream(), true);
		BufferedReader client_in  = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));

		
		String inputLine, outputLine;

		input_count = 0;

		while((inputLine = client_in.readLine()) != null) 
		{   
		    input_count += inputLine.length();

		    // what to do with this line?
		    
		    if(inputLine.startsWith("input:"))
		    {
			String data = inputLine.substring(6);

			writeToApp(data);

			snooze();

			String res = readFromApp();
	 	    
			//System.out.println("input: '" + data + "'");
			
			client_out.print(res);
			client_out.flush();
		    }

		    if(inputLine.startsWith("version:"))
		    {
			client_out.print("RemoteAppServer v0.1");
			client_out.flush();
		    }
		    
		    if(inputLine.startsWith("start:"))
		    {
			// it's a start command
			
			app_path = inputLine.substring(6);
			//System.out.println("running " + app_path);
			
			System.out.println("request for: " + app_path);
			
			int result = startApplication();
			
			switch(result)
			{
			case AppStartedOK:
			    client_out.println("ack");
			    System.out.println("application started ok");
			    break;
			case AppNotFound:
			    client_out.println("nack: '" + app_path + "' not found");
			    System.out.println("application not found");
			    break;
			case AppWillNotStart:
			    client_out.println("nack: '" + app_path + "' cannot be started");
			    System.out.println("application cannot be started");
			    break;
			case AppStartNotAllowed:
			    client_out.println("nack: security exception when starting '" + app_path + "'");
			    System.out.println("application cannot be started for security reasons");
			    break;
			}
		    }
		}

		stopApplication();
		
		System.out.println("connection closed by client (" + input_count + " bytes received in total)");
		
		client_out.close();
		client_in.close();
		server_socket.close();
		client_socket.close();
	    }
	    catch (IOException e) 
	    {
		System.out.println("warning: IOException during communicatation\n" + e.toString());
	    }
	}
    }

    private String app_path = null;

    private Process process;

    private OutputStream os;
    private InputStream is;
    private InputStream es;

    private Writer writer;
    private Reader reader;
    private Reader e_reader;
 
    private void stopApplication()
    {
	try
	{
	    if(writer != null)
		os.close();
	}
	catch (IOException ioe)
	{
	    System.out.println("unable to close OutputStream...error: \n" + ioe);
	}
	
	try
	{
	    if(reader != null)
		is.close();
	}
	catch (IOException ioe)
	{
	    System.out.println("unable to close InputStream...error: \n" + ioe);
	}
	
	try
	{
	    if(process != null)
		process.waitFor();
	    
	    if(debug_start_stop)
		System.out.println("app stopped  ok...");
	}
	catch (InterruptedException ie)
	{
	    System.out.println("unable to stop application...error: \n" + ie);
	}
	
    }

    private int startApplication()
    {
	//System.out.println("app is " + app_path);
	
	try
	{
	    Runtime rt = Runtime.getRuntime();
	    
	    try
	    {
		process = rt.exec(app_path);
	    }
	    catch(SecurityException se)
	    {
		return AppStartNotAllowed;
	    }
	    catch(IOException ioe)
	    {
		return AppNotFound;
	    }

	    if(debug_start_stop)
		System.out.println("app '" + app_path + "' started ok...");
	    
	    os  = process.getOutputStream(); // output _from_ plugin to Process
	    //writer = new BufferedWriter(new OutputStreamWriter(os));
	    writer = new OutputStreamWriter(os);
	    
	    is = process.getInputStream();   // input _from_ Process to plugin 
	    //reader = new BufferedReader(new InputStreamReader(is));
	    reader = new InputStreamReader(is);
	    
	    es = process.getInputStream();   // input _from_ Process to plugin 
	    //reader = new BufferedReader(new InputStreamReader(is));
	    e_reader = new InputStreamReader(es);
	    
	    if(debug_start_stop)
		System.out.println("streams connected...");

	    return AppStartedOK;
	    
	}
	catch(Exception e)
	{
	    process = null;
	    writer = null;
	    reader = null;
	    System.err.println("Unexpected exception whilst starting application '" + app_path+  "'\n...error: \n" + e);
	    return AppWillNotStart;
	}
    }

    private void writeCommandToApp(String com, String arg)
    {
	//System.out.println("writing to app...");
	if(writer == null)
	    return;

	try
	{
	    writer.write(com);
	    writer.write(":");
	    writer.write(arg);
	    writer.write("\n");
	    writer.flush();
	    
	    if(debug_comms)
		System.out.println("command : " + com + ":" + arg + " sent to remote app");
	}
	catch(IOException ioe)
	{
	    //System.out.println("problem writing to app\n  " + ioe);
	    System.err.println("(unable to write to app)\n");
	}
	
    }


    private void writeToApp(String str)
    {
	//System.out.println("writing to app...");
	if(writer == null)
	    return;

	for(int c=0; c< str.length(); c++)
	    if(str.charAt(c) == 4)
		System.out.println("writeToApp(): Ctrl-D in input");

	try
	{
	    writer.write(str);
	    writer.write("\n");
	    writer.flush();
	    
	    if(debug_comms)
		System.out.println((str.length()+1) + " char" + ((str.length() == 1) ? " sent" : "s sent"));

	}
	catch(IOException ioe)
	{
	    //System.out.println("problem writing to app\n  " + ioe);
	    System.err.println("(unable to write to app)\n");
	}
	
    }

    private void snooze()
    {
	try
	{
	    Thread.sleep(500);
	}
	catch(java.lang.InterruptedException ie)
	{
	    
	}
    }

    private String readFromApp()
    {
	if(reader == null)
	    return "(not connected)";

	StringBuffer sbuf = new StringBuffer();
	
	if(debug_comms)
	    System.out.println("reading from app...");

	try
	{
	    int avail = 1;

	    while(avail > 0)
	    {
		//System.out.println(len +  " chars available");
		int len = is.available();
		char[] cbuf = new char[len];

		if(reader.read(cbuf, 0, len) != len)
		{
		    System.out.println("couldn't read all " + len + " chars");
		}
		sbuf.append(new String(cbuf));

		if(es != null)
		{
		    len = es.available();
		    cbuf = new char[len];
		    
		    if(e_reader.read(cbuf, 0, len) != len)
		    {
			System.out.println("couldn't read all " + len + " chars");
		    }
		    sbuf.append(new String(cbuf));
		}

		if(debug_comms)
		{
		    String res = sbuf.toString();
		    System.out.println(" ..." + res.length() + " char" + ((res.length() == 1) ? " read" : "s read"));
		}

		// give the application a chance to do something....
		snooze();

		avail = is.available();
		if(es != null)
		    avail += es.available();
	    }


	    if(sbuf.length() == 0)
	    {
		if(debug_comms)
		    System.out.println(" ...no data available");
	    }
	}
	catch(IOException ioe)
	{
	    if(debug_comms)
		System.out.println("problem reading from app\n  " + ioe);
	}

	//System.out.println(sbuf.toString().length() + " chars read");

	return sbuf.toString();
    }

}
