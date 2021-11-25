import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.lang.Compiler;
import java.lang.reflect.*;
import javax.swing.event.*;
import javax.swing.JOptionPane;

import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.undo.*;

//
// Runs 'Code Fragments' by embedding them in a simple class, saving
//  a temporary file, invoking the compiler on it, loading the
//  class, creating an instance and calling the method using
//  reflection.
//

public class CodeRunner implements Plugin
{
    public CodeRunner(maxdView mview_)
    {
	mview = mview_;
    }

    public void cleanUp()
    {
	saveDefaultOptions();
	saveLibraryDetailsToFile(library_path_str);
	//closeCodeLibDialog();
	frame.setVisible(false);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	frame = new JFrame("Code Runner");

	mview.decorateFrame( frame );

	//System.out.println("++ CodeRunner is constructed ++");
	

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	
	loadDefaultOptions();
	
	if(compiler_path_str.length() == 0)
	{
	    tryToFindCompiler();
	}

	loadLibraryDetailsFromFile(library_path_str);  // t-l-d + "coderunner.dat");

	addComponents();
	frame.pack();
	frame.setVisible(true);

	buildList();

	updateLibraryButtons();

	//if(show_library)
	//    createCodeLibDialog();
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Code Runner", "transform", "Execute Java code fragments", "",
								1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[1];
	
	/*
	String[] args = new String[] { "code", "string" };
	
	com[0] = new PluginCommand("runCode", args);
	
	args = new String[] { "file", "file" };
	
	com[2] = new PluginCommand("runFromFile", args);

	*/

	String[] args = new String[] { "name", "string", "", "m", "the name of the library entry to run" };
	
	com[0] = new PluginCommand("runFromLibrary", args);
	
	return com;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("runFromLibrary"))
	{
	    String arg = mview.getPluginArg("name", args);

	    if(arg != null)
	    {
		//System.out.println("running library entry " + arg);

		// if the GUI has not been launched, we will not have loaded the library
		//
		if((library_v == null) || (library_v.size() == 0))
		{
		    loadDefaultOptions();
		    loadLibraryDetailsFromFile(library_path_str); 
		}

		if(library_v != null)
		{
		    for(int c=0; c < library_v.size(); c++)
		    {
			CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(c);
			if(cuc.name.equals(arg))
			{
			    mview.setMessage("CodeRunner: running '" + arg + "'");
			    runFromLibrary(c);
			}
		    }
		}
		else
		{
		    mview.errorMessage("Code Runner: cannot find '" + arg + "' in library");
		}
	    }
	    else
	    {
		mview.errorMessage("Code Runner: expecting 'name' argument");
	    }
	}
	if(done != null)
	    done.signal();
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  properties
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void loadDefaultOptions()
    {
	load_path_str = mview.getProperty("coderun.load_path", mview.getUserSpecificDirectory() + "code-fragments" );
	save_path_str = mview.getProperty("coderun.save_path", mview.getUserSpecificDirectory() + "code-fragments" );
	
	compiler_path_str  = mview.getProperty("coderun.compiler_path", "");

	compiler_flags_str = mview.getProperty("coderun.compiler_flags", "-O");

	//library_path_str  = mview.getProperty("coderun.library_path", 
	//				      mview.getTopDirectory() +
	//				      File.separatorChar + 
	//                                    mview.getPluginDirectory("CodeRunner"));

	library_path_str  = mview.getProperty("coderun.code_library_path", mview.getUserSpecificDirectory() + "code-library" );


	use_jit      = mview.getBooleanProperty("coderun.use_jit", false);
	clean_errors = mview.getBooleanProperty("coderun.clean_errors", true);

	//user_class_dir = mview.getTopDirectory() + File.separatorChar
	//    		 mview.getPluginDirectory("CodeRunner");

	user_class_dir = mview.getTemporaryDirectory(); // mview.getProperty("coderun.library_path", getTemporaryDirectory() );

	compiler_classpath_str = System.getProperty("java.class.path") + System.getProperty("path.separator") + 
	                         mview.getTopDirectory() + System.getProperty("path.separator") +
	                         mview.getPluginDirectory("Code Runner") + System.getProperty("path.separator") +
		    		 user_class_dir + System.getProperty("path.separator") + ".";

	compiler_time_limit = mview.getIntProperty("coderun.time_limit", 15);

	//System.out.println("user_class_dir = " + user_class_dir);

	//show_library = mview.getBooleanProperty("coderun.show_library", false);
    }
    
    public void saveDefaultOptions()
    {
	mview.putProperty("coderun.compiler_path",  compiler_path_str);
	mview.putProperty("coderun.compiler_flags", compiler_flags_str);
	
	mview.putProperty("coderun.code_library_path", library_path_str);
	
	mview.putProperty("coderun.load_path", load_path_str);
	mview.putProperty("coderun.save_path", save_path_str);
	
	mview.putBooleanProperty("coderun.use_jit", use_jit);
	mview.putBooleanProperty("coderun.clean_errors", clean_errors);

	mview.putIntProperty("coderun.width", code_jsp.getWidth());
	mview.putIntProperty("coderun.height", code_jsp.getHeight());
	
	mview.putBooleanProperty("coderun.show_method_tree", show_method_tree);

	mview.putIntProperty("coderun.time_limit", compiler_time_limit);

	//mview.putBooleanProperty("coderun.show_library", (cld != null));
	// dont save the classpath?
    }
    
    private void tryToFindCompiler()
    {
	final String[] paths = 
	{ "/usr/local/java",
	  "/home/java",
	  "/opt/java",
	  "/usr/java",
	  "/usr",
	  "/opt",
	  "/home",
	  "/usr/common/java",
	  "/usr/share/java",
	  "C:\\java",
	  "D:\\java",
	  "C:\\Program Files\\",
	  "D:\\Program Files\\",
	  "C:\\Program Files\\java",
	  "D:\\Program Files\\java",
	  "C:\\",
	  "D:\\"
	};
	final String[] jdks =
	{
	    "jdk1.3.0",
	    "jdk1.3",
	    "JDK1.3.0",
	    "JDK1.3",
	    "JDK 1.3",
	    "1.3",
	    "jdk1.2.2",
	    "JDK1.2.2",
	    "JDK 1.2.2",
	    "1.2.2"
	};

	for(int p=0; p < paths.length; p++)
	    for(int j=0; j < jdks.length; j++)
	    {
		String path = paths[p] + File.separatorChar + jdks[j] + File.separatorChar + "bin" + File.separatorChar + "javac";
		
		File f = new File(path);
		if(f.exists())
		{
		    compiler_path_str = path;
		    //		    System.out.println("tryToFindCompiler(): compiler found: " + path);
		    return;
		}
	    }
    }
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  compiler
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void compileLoadAndRunUserClass()
    {
	if(saveUserClass() == 1)
	{
	    // whether it's already compiled....
	    int result = (code_status != CodeCompiled) ? compileUserClass() : 0;
	    
	    if(result == 0)
	    {
		if(debug_load_class)
		    System.out.println("compile ok");
		loadAndRunUserClass();
	    }
	    else
	    {
		if(debug_load_class)
		{
		    System.out.println("compile failed");
		    System.out.println("exec result is " + result);
		} 
	    }
	}
    }

    private int saveUserClass()
    {
	try
	{	
	    String user_class_file_name = user_class_dir + File.separatorChar + "CustomCodeWrapper.java";

	    // write the code to a file...
	
	    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(user_class_file_name)));
	    
	    String user_code = 	code_jta.getText();

 	    writer.write(default_code_header);
	    writer.write(user_code);
	    writer.write(default_code_footer);

	    writer.close();
	    
	    if(debug_load_class)
		System.out.println("code fragment written to file '" + user_class_file_name + "'");

	    return 1;
	}
	catch (java.io.IOException ioe)
	{
	    mview.errorMessage("Unable to write class file\n  " + ioe);
	}
	return -1;

    }

    // some state for the compliation

    boolean compile_has_been_cancelled = false;
    boolean compile_has_completed = false;

    /*
    private int compileUserClass()
    {
	
	Runnable doIt = new Runnable() 
	    {
		public void run() 
		{
		    compileUserClassWrapper();
		}
	    };
	
	SwingUtilities.invokeLater(doIt);

	return 0;
    }
    */

    private int compileUserClass()
    {
	// start a thread and wait for either successful completion, or the user to cancel the
	// operation

	/*
	ProgressOMeter pm = new ProgressOMeter("Compiling");

	pm.setCancelAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    compile_has_been_cancelled = true;
		}
	    });
	*/

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	compile_has_been_cancelled = false;
	compile_has_completed      = false;
	 
	//pm.startIt();

	UserClassCompilerThread ucct = new UserClassCompilerThread();
	ucct.start();

	int ticker = 0;

	while(!compile_has_been_cancelled && !compile_has_completed)
	{
	    try
	    {
		Thread.sleep(250);
		
		// System.out.println("waiting..." + ((compiler_time_limit * 4) - ticker));
	    }
	    catch (InterruptedException ie)
	    {
	    }

	    if(++ticker > (compiler_time_limit * 4))
	    {
		System.err.println("compile terminated");

		compile_has_been_cancelled = true;
		ucct.error_message = "Timeout: Compile aborted after " + compiler_time_limit + " seconds" + 
		    "\n\n(The 'Options' panel can be used to adjust the timeout delay)";
		
		ucct.result = -1;
	    }
	}

	if(compile_has_been_cancelled)
	    ucct.stop();

	//pm.stopIt();


	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	if(ucct.result != 0)
	{
	    System.err.println("compile failed");

	    CustomCompilerMessageDialog cmd = new CustomCompilerMessageDialog("Compile Error", 
									      ucct.error_message,
									      ucct.error_map);
	    cmd.pack();
	    cmd.setVisible(true);
	    
	}
	else
	{
	    System.err.println("compile succeeded");
	}


	return ucct.result;

    }


    private class UserClassCompilerThread extends Thread
    {
	public void run()
	{
	    try
	    {	
		//if(debug_load_class)
		//{
		//    System.out.println("java home is " + System.getProperty("java.home"));
		//    System.out.println("application dir is " + mview.getTopDirectory());
		//    System.out.println("plugin dir is " + mview.getPluginDirectory("CodeRunner"));
		//    System.out.println("classpath is " + System.getProperty("java.class.path"));
		//    System.out.println("JIT compiler is " + System.getProperty("java.compiler"));
		//}
		
		if( (compiler_path_str == null) || (compiler_path_str.length() == 0) )
		{
		    error_message = "No compiler specified (see Options)";
		    result = -1;
		    compile_has_completed = true;
		    return;
		}
		
		String user_class_file_name = user_class_dir + File.separatorChar + "CustomCodeWrapper.class";
		String user_code_file_name  = user_class_dir + File.separatorChar + "CustomCodeWrapper.java";
		
		// write the code to a file...
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(user_class_file_name)));
		
		String user_code = 	code_jta.getText();

		writer.write(default_code_header);
		writer.write(user_code);
		writer.write(default_code_footer);
		
		writer.close();

		//if(debug_load_class)
		System.out.println( "user code written to '" + user_class_file_name + "'" ) ;

		
		{
		    //  exec the normal compiler...
		    //
		    if(debug_load_class)
			System.out.println(" UserClassCompilerThread:no JIT compiler, using exec");
		    
		    try
		    {
			
			//String exec_cmd = compiler_path_str + " " + 
			//    compiler_flags_str + 
			//    " -classpath \"" +  compiler_classpath_str +
			//    "\" " + user_code_file_name;
			
			String[] exec_args = new String[] { compiler_path_str, "-classpath", compiler_classpath_str, user_code_file_name };


			for(int ea=0; ea < exec_args.length; ea++)
			    System.out.print( exec_args[ ea ] + " ");
			System.out.println();

			
			System.out.println("doing exec()...");
			
			// run the compile command and wait for execution to terminate
			//
			Process p = Runtime.getRuntime().exec( exec_args );
			
			if(debug_load_class)
			    System.out.println("starting output grabbers...");
			
			// any error message?
			StreamGobbler errorGobbler = new StreamGobbler( p.getErrorStream(), "ERROR" );
			
			// any output?
			StreamGobbler outputGobbler = new StreamGobbler( p.getInputStream(), "OUTPUT" );
			
			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			if(debug_load_class)
			    System.out.println("waiting for process to terminate...");
			

			try
			{
			    p.waitFor();
			}
			catch (InterruptedException ie)
			{
			}

			System.out.println("process has ended...");
			
			/*
			// optionally try using the buit-in JIT compiler 
			//
			if(use_jit)
			{
			    if( Compiler.compileClasses("CustomCodeWrapper") )
			    {
				if(debug_load_class)
				    System.out.println(" UserClassCompilerThread:compiled ok using JIT");
			    }
			    else
			    {
				error_message = "Unable to invoke JIT native-code compiler";
				result = -1;
				compile_has_completed = true;
				return;
			    }
			}
			*/

			
			if(p.exitValue() != 0)
			{
			    //error_message = grabAndParseCompilerOutput( p, user_code_file_name, error_map );

			    error_message = processCompilerOutput( errorGobbler.getResult(), user_code_file_name, error_map );

			    //error_message = errorGobbler.getResult();
			}
			else
			{
			    //if(debug_load_class)
			    System.out.println("UserClassCompilerThread:...finished with no errors");
			    
			    code_status = CodeCompiled;
			}
			
			result = p.exitValue();

			compile_has_completed = true;

			return;
		    }
		    catch (java.io.IOException ioe)
		    {
			System.out.println("exec failed....\n  " + ioe);

			compile_has_completed = true;
			result = -1;

			File test_comp = new File(compiler_path_str);
			if(!test_comp.exists())
			{
			    mview.errorMessage("Specified compiler '" + compiler_path_str + "' not found");
			    return;
			}

			error_message = "Couldn't start compiler\n  " + ioe;
		    }
		}
	    }
	    catch (java.io.IOException ioe)
	    {
		error_message = "Unable to write class file\n  " + ioe;
	    }
	    catch( Exception ex )
	    {
		ex.printStackTrace();
	    }

	    result = -1;
	    compile_has_completed = true;
	    return;
	}

	public int result;
	public String error_message;
	public Hashtable error_map = new Hashtable();
    }


    class StreamGobbler extends Thread
    {
	InputStream is;
	String type;
	StringBuffer result;

	StreamGobbler(InputStream is, String type)
	{
	    this.is = is;
	    this.type = type;
	    result = new StringBuffer();
	}
	
	public String getResult() { return result.toString(); }

	public void run()
	{
	    try
	    {
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line=null;
		while ( (line = br.readLine()) != null)
		{
		    result.append( line + "\n" );
		}
	    } catch (IOException ioe)
	    {
		ioe.printStackTrace();
	    }
	}
    }

/*
    private String grabAndParseCompilerOutput( Process p, String user_code_file_name, Hashtable error_map )
    {
	// collate the error messages into a String
	//
	StringWriter sw = new StringWriter();
	
	// record the error stream
	InputStream errors = p.getErrorStream();
	
	boolean in_line_no = false;
	boolean first_line_no = true;
	boolean in_filename = false;
	int in_filename_pos = 0;
	
	StringBuffer sbuf = null;
	
	int error_id = 0;
	int cur_line_num = 0;
	
	error_map.clear();
	
	//if(debug_load_class)
	System.out.println("UserClassCompilerThread:...finished with errors");
	//
	//  parse error messages as they are read in:
	//
	//  remove occurences of the filename and adjust the line numbers
	//  to make it look like just this function is being compiled 
	//      
	try
	{
	    int ch = 0;
	    while(ch >= 0)
	    {
		if(clean_errors)
		{
		    if(ch == ':')
		    {
			if(!in_line_no)
			{
			    in_line_no = true;
			    sbuf = new StringBuffer();
			    if(first_line_no)
			    {
				first_line_no = false;
				in_line_no = true;
			    }
			}
			else
			{
			    if(sbuf != null)
			    {
				//System.out.println("sbuf is " + sbuf.toString());
				try
				{
				    int l_no = new Integer(sbuf.toString()).intValue();
				    sw.write(":" + String.valueOf(l_no+wrapper_lines_offset));
				    //System.out.println("sbuf is " + sbuf.toString() + " l_no is " + l_no);
				    
				    // System.out.println("error " + error_id + 
				    //	       " starts on line " + cur_line_num + 
				    //	       " refers to line " + (l_no+wrapper_lines_offset));
				    
				    error_map.put( new Integer(cur_line_num ), 
						   new Integer(l_no+wrapper_lines_offset ) );
				    
				    error_id++;
				}
				catch(NumberFormatException nfe)
				{
				    
				}
			    }
			    in_line_no = false;
			    
			}
		    }
		    
		    if(ch == '\n')
		    {
			first_line_no = true;
			in_line_no = false;
			cur_line_num++;
		    }
		    
		    if(!in_filename)
		    {
			// might this character be the start of the filename?
			if(ch == user_code_file_name.charAt(0))
			{
			    in_filename = true;
			    sbuf = new StringBuffer();
			    in_filename_pos = 0;
			}
		    }
		    
		    if((!in_line_no) && (!in_filename))
		    {
			if(ch != '\0')
			    sw.write(ch);
		    }
		    else
		    {
			if((ch != ':') || (in_filename))
			    // accumulate line number or filename chars
			    sbuf.append((char) ch);
		    }
		    
		    if(in_filename)
		    {
			// System.out.println("filename: " + sbuf.toString());
			if((in_filename_pos+1) == user_code_file_name.length())
			{
			    // we have successfully got to the end of the filename
			    // dump the characters and continue as if nothing happnened
			    //
			    sw.write("line");
			    in_filename = false;
			    
			}
			else
			{
			    // already in filename (possibly), keep checking
			    //
			    if(ch == user_code_file_name.charAt(in_filename_pos))
			    {
				in_filename_pos++;
			    }
			    else
			    {
				// this wasn't the filename after all....
				//
				in_filename = false;
				sw.write(sbuf.toString());
			    }
			}
		    }
		}
		else
		{
		    
		    // dont play around with the compiler's text
		    //
		    if(ch != '\0')
			sw.write((char) ch);
		    if(ch == '\n')
			sw.write("\n");
		    
		}
		
		ch = errors.read();
		
		if(ch == '\t')
		    ch = ' ';
	    }
	}
	catch(IOException ioe)
	{
	    
	}
	
	return sw.toString();
    }
*/

    private String processCompilerOutput( String compiler_output, String user_code_file_name, Hashtable error_map )
    {
	// collate the error messages into a String
	//
	StringWriter sw = new StringWriter();
	
	boolean in_line_no = false;
	boolean first_line_no = true;
	boolean in_filename = false;
	int in_filename_pos = 0;
	
	StringBuffer sbuf = null;
	
	int error_id = 0;
	int cur_line_num = 0;
	
	error_map.clear();
	
	//if(debug_load_class)

	System.out.println("processCompilerOutput():...parsing error message");

	//
	//  parse error messages as they are read in:
	//
	//  remove occurences of the filename and adjust the line numbers
	//  to make it look like just this function is being compiled 
	//      
//	try
	{
	    int ch_pos = 0;

	    int ch = 0;

	    while(ch >= 0)
	    {
		if(clean_errors)
		{
		    if(ch == ':')
		    {
			if(!in_line_no)
			{
			    in_line_no = true;
			    sbuf = new StringBuffer();
			    if(first_line_no)
			    {
				first_line_no = false;
				in_line_no = true;
			    }
			}
			else
			{
			    if(sbuf != null)
			    {
				//System.out.println("sbuf is " + sbuf.toString());
				try
				{
				    int l_no = new Integer(sbuf.toString()).intValue();
				    sw.write(":" + String.valueOf(l_no+wrapper_lines_offset));
				    //System.out.println("sbuf is " + sbuf.toString() + " l_no is " + l_no);
				    
				    // System.out.println("error " + error_id + 
				    //	       " starts on line " + cur_line_num + 
				    //	       " refers to line " + (l_no+wrapper_lines_offset));
				    
				    error_map.put( new Integer(cur_line_num ), 
						   new Integer(l_no+wrapper_lines_offset ) );
				    
				    error_id++;
				}
				catch(NumberFormatException nfe)
				{
				    
				}
			    }
			    in_line_no = false;
			    
			}
		    }
		    
		    if(ch == '\n')
		    {
			first_line_no = true;
			in_line_no = false;
			cur_line_num++;
		    }
		    
		    if(!in_filename)
		    {
			// might this character be the start of the filename?
			if(ch == user_code_file_name.charAt(0))
			{
			    in_filename = true;
			    sbuf = new StringBuffer();
			    in_filename_pos = 0;
			}
		    }
		    
		    if((!in_line_no) && (!in_filename))
		    {
			if(ch != '\0')
			    sw.write(ch);
		    }
		    else
		    {
			if((ch != ':') || (in_filename))
			    // accumulate line number or filename chars
			    sbuf.append((char) ch);
		    }
		    
		    if(in_filename)
		    {
			// System.out.println("filename: " + sbuf.toString());
			if((in_filename_pos+1) == user_code_file_name.length())
			{
			    // we have successfully got to the end of the filename
			    // dump the characters and continue as if nothing happnened
			    //
			    sw.write("line");
			    in_filename = false;
			    
			}
			else
			{
			    // already in filename (possibly), keep checking
			    //
			    if(ch == user_code_file_name.charAt(in_filename_pos))
			    {
				in_filename_pos++;
			    }
			    else
			    {
				// this wasn't the filename after all....
				//
				in_filename = false;
				sw.write(sbuf.toString());
			    }
			}
		    }
		}
		else
		{
		    
		    // dont play around with the compiler's text
		    //
		    if(ch != '\0')
			sw.write((char) ch);
		    if(ch == '\n')
			sw.write("\n");
		    
		}
		
		ch = ch_pos < compiler_output.length() ? compiler_output.charAt( ch_pos++ ) : -1;
		
		if(ch == '\t')
		    ch = ' ';
	    }
	}
//	catch(IOException ioe)
//	{
//	    
//	}
	
	return sw.toString();
    }


    private void reportUserClassError(String fc, String c, String e)
    {
	System.out.println("\nwhilst loading: " + c);
	System.out.println("          from: " + fc);
    	System.out.println("         error: " + e + "\n");

	
    }

    void printModifiers(Object o) 
    {
	Class c = o.getClass();
	int m = c.getModifiers();

	System.out.print(o.getClass().getName() + " modifiers : ");
	if (Modifier.isPublic(m))
	    System.out.print("public ");
	if (Modifier.isAbstract(m))
	    System.out.print("abstract ");
	if (Modifier.isFinal(m))
	    System.out.print("final ");
	if (Modifier.isInterface(m))
	    System.out.print("interface ");
	if (Modifier.isPrivate(m))
	    System.out.print("private ");
	if (Modifier.isProtected(m))
	    System.out.print("protected ");
	if (Modifier.isStatic(m))
	    System.out.print("static ");
	if (Modifier.isSynchronized(m))
	    System.out.print("synchronized ");
	if (Modifier.isVolatile(m))
	    System.out.print("volatile ");
	if (Modifier.isTransient(m))
	    System.out.print("transient ");
	System.out.println("");
    }

    void printClassInfo(Object o) 
    {
	System.out.println("class name is " + o.getClass().getName());
	
	printModifiers(o);

	Class subclass = o.getClass();
	Class superclass = subclass.getSuperclass();
	System.out.println("superclasses:");
	while (superclass != null) 
	{
	    String className = superclass.getName();
	    System.out.println("  " + className);
	    subclass = superclass;
	    superclass = subclass.getSuperclass();
	}
	
	CodeWrapper sc = new CodeWrapper(mview);
	Class sc_class = sc.getClass();
	printModifiers(sc);

	if(sc.getClass().isInstance(o))
	    System.out.println("is instance of CodeWrapper");
	else
	    System.out.println("is not instance of CodeWrapper");
	
	if(sc_class.isAssignableFrom(o.getClass()))
	    System.out.println("is subclass of CodeWrapper");
	else
	    System.out.println("is not subclass of CodeWrapper");

	Object obj = new Object();
	Class obj_class = obj.getClass();
	if(obj_class.isAssignableFrom(o.getClass()))
	    System.out.println("is subclass of Object");
	else
	    System.out.println("is not subclass of Object");

    }

    private void loadAndRunUserClass()
    {
	if(ucrt != null)
	{
	    mview.alertMessage("A code fragment is already running.\nUse \"Stop\" to terminate it.");
	    return;
	}


	CodeWrapper sc = null;
	Class cls_def = null;

	Class[]  uc_args_class = new Class[]  { maxdView.class };
	Object[] uc_args       = new Object[] { mview };

	Constructor uc_args_constructor;

	String class_name = "CustomCodeWrapper";

	//String full_name = mview.getTopDirectory() +  
	//                   File.separatorChar + 
	//		   mview.getPluginDirectory("CodeRunner") + 
	//                   class_name + ".class";
	
	String full_name = user_class_dir + File.separatorChar + class_name + ".class";

	try 
	{

	    if(use_jit)
	    {
		try
		{
		    // must unload the previous instance of the class

		    cls_def = Class.forName(class_name);
		}
		catch(java.lang.ClassNotFoundException cnfe)
		{
		    mview.errorMessage("Using JIT, and couldn't load class");
		    return;
		}
	    }
	    else
	    {
		// if(loader == null)

		
		
		try
		{
		    //maxdView.CustomClassLoader ucl = (maxdView.CustomClassLoader) getClass().getClassLoader();
		    //ucl.addPath( mview.getTemporaryDirectory() );
		    
		    maxdView.CustomClassLoader ucl = mview.new CustomClassLoader( mview.getTemporaryDirectory(), 
										  getClass().getClassLoader() );
		    
		    
		    cls_def = ucl.findClass( class_name );
		}
		catch( java.net.MalformedURLException murle )
		{
		    System.err.println( "expected location of user class seems to be faulty" );
		}

		//loader = new UserClassLoader( this.getClass() );
		//cls_def = loader.loadClass(full_name, class_name);

		if(debug_load_class)
		    System.out.println("loaded from '" + full_name + "'");
	    }

	    if(cls_def != null)
	    {
		// get a handle to the constructor method of the plugin
		uc_args_constructor =  cls_def.getConstructor(uc_args_class);

		if(uc_args_constructor != null)
		{
		    Object obj = mview.createObject(uc_args_constructor, uc_args);

		    if(obj != null)
		    {
			if(debug_load_class)
			    printClassInfo(obj);

			// try to invoke function called doIt()...
			try
			{
			    Class c = obj.getClass();
			    Method do_it_method = c.getMethod("doIt", null);
			    
			    ucrt = new UserClassRunnerThread(full_name, class_name, obj, do_it_method);

			    ucrt.start();
			} 
			catch (NoSuchMethodException e) 
			{
			    reportUserClassError(full_name, class_name, "finding doIt(): " + e.toString());
			} 
			//catch(ClassCastException cce)
			    //{
			//    sc = null;
			//    reportUserClassError(full_name, class_name, "calling doIt(): " + cce.toString());
			//}
		    }
		    /*
		    if(sc != null)
		    {
			// initialise the plugin....

			System.out.println("user class loaded ok");
		    }
		    else
			reportUserClassError(full_name, class_name,
					  "couldn't instantiate object" );
		    */
		}
		else
		    reportUserClassError(full_name, class_name,
				      "couldn't locate correctly formed constructor method");
	    }
	    else
		reportUserClassError(full_name, class_name,
				  "couldn't load class");

	} 
	catch (NullPointerException e)
	{
	    reportUserClassError(full_name, class_name,
				  "couldn't find class"); 
	}
	catch (NoSuchMethodException e) 
	{
	   reportUserClassError(full_name, class_name,
				  "couldn't locate correctly formed method"); 
	}
 
    }

    class UserClassRunnerThread extends Thread
    {
	private Method method;
	private Object obj;
	private String class_name;
	private String full_name;

	public UserClassRunnerThread(String fn, String cn, Object obj_, Method method_)
	{
	    full_name = fn;
	    class_name = cn;
	    method = method_;
	    obj = obj_;
	}

	public void run()
	{
	    if(run_jb != null)
		run_jb.setText("Stop");

	    try
	    {
		method.invoke(obj, null);
	    }
	    catch (IllegalAccessException e) 
	    {
		reportUserClassError(full_name, class_name, "Cannot invoke doIt(): " + e.toString());
	    } 
	    catch (InvocationTargetException e) 
	    {
		// this means the doIt() function threw an exception itself...

		//reportUserClassError(full_name, class_name, "Exception thrown by doIt(): " + e.getTargetException().toString());
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		e.getTargetException().printStackTrace(pw);

		mview.infoMessage("Code fragment threw an Exception: \n\n" + 
				  "  " + e.getTargetException().toString() + "\n\n" + 
				  "Stack Trace:\n" + sw.toString() );
	    }
	    catch (Exception e) 
	    {
		reportUserClassError(full_name, class_name, "UserClassRunnerThread(): Unexpected exception: " + e.toString());
	    }

	    //System.out.println("code fragment has finished running");

	    if(run_jb != null)
		run_jb.setText("Run");

	    ucrt = null;


	    
	    // ::TODO:: now we need to unload this class definition ....
	    
	    

	    /*
	    catch (Exception e)
	    {
		mview.errorMessage("Exception generated by code fragment:\n  " + e);
	    }
	    catch (Error e)
	    {
		mview.errorMessage("Error generated by code fragment:\n  " + e);
	    }
	    */
	}
    }

/*
    public class UserClassLoader extends java.lang.ClassLoader
    {
	ClassLoader parent_cl = null;

	public UserClassLoader( Class parent_class )
	{
	    parent_cl = parent_class.getClassLoader();
	}

	private byte[] loadClassData(String name)
	{
	    // load the class data from the connection

	    if(debug_load_class)
		System.out.println("     - UserClassLoader.loadClassData() trying '" + name + "'");

	    try
	    {
		File file = new File(name);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

		// what happens for really big classes??
		//
		int file_length = (int) file.length();

		byte b[] = new byte[file_length];

		if(bis.read(b, 0, file_length) == file_length)
		{
		    if(debug_load_class)
			 System.out.println("     - UserClassLoader.loadClassData() load is good (" + file_length + " bytes)");
		}

		return b;

	    }
	    catch (IOException e) 
	    {
		if(debug_load_class)
		     System.out.println("     -                                 FAILED");
		return null;
	    }
	}
	
	public synchronized Class loadClass(String class_name, boolean resolve) 
	{
	    return loadClass(mview.getTopDirectory() + File.separatorChar + mview.getPluginDirectory("CodeRunner") + class_name + ".class", 
			     class_name);
 	}

	public synchronized Class loadClass(String full_name, String class_name) 
	{
	    Class c = null;

	    try
	    {
		
		if(class_name.indexOf("java") < 0)
		{
		    if(debug_load_class)
			System.out.println("     - UserClassLoader() trying file loader on '" + full_name + "'");
		    
		    byte data[] = loadClassData(full_name);
		    
		    if(data != null)
		    {
			c = defineClass(class_name, data, 0, data.length);
			
			if(debug_load_class)
			{
			    if(c != null)
				System.out.println("     ->  ok");
			    else
				System.out.println("     ->  failed, trying custom load");
			}
		    }
		}

		if(c == null)
		{
		    try
		    {
			if(debug_load_class)
			    System.out.println("     - UserClassLoader() trying parent class loader on '" + class_name + "'");
			
			c = parent_cl.loadClass(class_name);
			
			if(debug_load_class)
			{
			    if(c != null)
				System.out.println("     ->  ok");
			    else
				System.out.println("     ->  failed, givning up");
			}
		    }
		    catch(ClassNotFoundException e)
		    {
			if(debug_load_class)
			    System.out.println("     -                   failed (ClassNotFoundException)" );
		    }
		    catch(NoClassDefFoundError e)
		    {
			if(debug_load_class)
			    System.out.println("     -                   failed (NoClassDefFoundError)");
		    }
		}

		// always resolve
		if(c != null)
		    resolveClass(c);

	    }
	    catch (java.lang.ClassFormatError e)
	    {
		c = null;
	    }
	    catch (java.lang.NoClassDefFoundError e)
	    {
		c = null;
	    }
	    catch (java.lang.IncompatibleClassChangeError e)
	    {
		c = null;
	    }
	    return c;
	}
    }
*/

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  files
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean loadFileIntoTextArea(File file, boolean insert)
    {
	if(code_jta == null)
	{
	    needs_save = false;   // needed?
	    return true;
	}

	try 
	{
	    boolean result = false;
	    
	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	    int file_length = (int) file.length();
	    byte b[] = new byte[file_length];
	    if(bis.read(b, 0, file_length) == file_length)
	    {
		code_status = CodeEdited;

		if(insert)
		{
		    code_jta.insert(new String(b), code_jta.getCaretPosition());
		    needs_save = true;
		}
		else
		{
		    code_jta.setText(new String(b));
		    needs_save = false;
		}

		// scroll the view back to the top 
		code_jta.scrollRectToVisible(new Rectangle(1,1));
		
		undo_man.discardAllEdits();
		undo_action.update();
		redo_action.update();

		return true;
	    }
	    else
	    {
		mview.errorMessage("Couldn't read file '" + file.getName() + "'");
	    }
	}
	catch(FileNotFoundException f)
	{ 
	    mview.errorMessage("No such file '" + file.getName() + "'");
	}
	catch (IOException e) 
	{
	    mview.errorMessage("File '" + file.getName() + "' broken");
	}
	return false;
    }

    public boolean loadUserCode()
    {
	if(needs_save)
	{
	    if(mview.infoQuestion("Current code has not been saved.\nSave it now?", "No", "Yes") == 1)
		saveUserCode();
	}

	JFileChooser fc = mview.getFileChooser(load_path_str);

	int returnVal = fc.showOpenDialog(frame);
	
	if (returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    File file = fc.getSelectedFile();

	    load_path_str = fc.getCurrentDirectory().getPath();

	    library_list.clearSelection();
	    
	    updateLibraryButtons();

	    current_library_entry = -1;

	    return loadFileIntoTextArea(file, false);
	}
	return false;
    }


    public boolean insertUserCode()
    {
	JFileChooser fc = mview.getFileChooser(load_path_str);

	int returnVal = fc.showOpenDialog(frame);
	
	if (returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    File file = fc.getSelectedFile();

	    load_path_str = fc.getCurrentDirectory().getPath();

	    library_list.clearSelection();
	    
	    updateLibraryButtons();

	    current_library_entry = -1;

	    return loadFileIntoTextArea(file, true);
	}
	return false;
    }

    public boolean resetUserCode()
    {
	if(needs_save)
	{
	    if(mview.infoQuestion("Really reset to empty method body?", "Yes", "No") == 1)
		return false;
	}

	
	code_jta.setText(default_code_body);
	code_status = CodeEdited;
	needs_save = false;

	undo_man.discardAllEdits();
	undo_action.update();
	redo_action.update();

	return true;
    }

    public boolean saveUserCode()
    {
	JFileChooser fc = mview.getFileChooser(save_path_str);

	int returnVal = fc.showSaveDialog(frame);
	if (returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    File file = fc.getSelectedFile();

	    save_path_str = fc.getCurrentDirectory().getPath();

	    try
	    {	
		String user_code = code_jta.getText();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(user_code);
		writer.close();
		needs_save = false;
		return true;
	    }
	    catch (java.io.IOException ioe)
	    {
		JOptionPane.showMessageDialog(null, 
					      "Unable to write class file", 
					      "Unable to write", 
					      JOptionPane.ERROR_MESSAGE); 
		return false;
	    }
	}
	return false;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  custom error reporting panel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    class CustomCompilerMessageDialog extends JFrame implements javax.swing.event.CaretListener
    {
	private Hashtable error_map;
	private JTextArea text_area;

	public CustomCompilerMessageDialog(String title, String str, Hashtable error_map_) 
	{
	    error_map = error_map_;

	    addComponents(title, str);

	    mview.decorateFrame( this );
	}

	public void caretUpdate(javax.swing.event.CaretEvent e) 
	{
	    try
	    {
		int line_num = text_area.getLineOfOffset(text_area.getCaretPosition());
		//System.out.println("click on line "+ line_num);
		if(error_map != null)
		{
		    while(line_num >= 0)
		    {
			Integer e_pos = (Integer) error_map.get(new Integer(line_num));
			if(e_pos != null)
			{
			    int e_pos_i = e_pos.intValue();
			    //System.out.println("refers to error in line " + e_pos_i);

			    int start = code_jta.getLineStartOffset( e_pos_i - 1);
			    int end = code_jta.getLineStartOffset( e_pos_i ) - 1;

			    //System.out.println("selecting chars " + start + " to " + end);

			    code_jta.grabFocus();

			    code_jta.select(start, end);
			    return;
		    }
			line_num--;
		    }
		}
	    }
	    catch(javax.swing.text.BadLocationException ble)
	    {
	    }
	}

	private void addComponents(String title, String str)
	{
	    JPanel panel = new JPanel();
	    GridBagConstraints c = null;
	    GridBagLayout gbag = new GridBagLayout();
	    panel.setLayout(gbag);
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	    setTitle(title);

	    /*
	    JLabel icon_label = new JLabel( mview.error_icon );
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    gbag.setConstraints(icon_label, c);
	    panel.add(icon_label);
	    */

	    int line = 0;
	    text_area = new JTextArea(str);
	    text_area.setEditable(false);
	    text_area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    text_area.setFont(new Font("Courier", Font.PLAIN, 12));

	    text_area.addCaretListener(this);
	    
	    JScrollPane jsp = new JScrollPane(text_area);
	    jsp.setPreferredSize(new Dimension(500, 250));

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 5.0;

	    gbag.setConstraints(jsp, c);
	    panel.add(jsp);

	    JButton jb = new JButton("OK");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setVisible(false);
		    }
		});
	    c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weightx = 1.0;

	    gbag.setConstraints(jb, c);
	    panel.add(jb);

	    getContentPane().add(panel);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  popup menu
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    class PopupListener extends MouseAdapter 
    {
	public PopupListener()
	{
	}

	public void mousePressed(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
    }

    private void maybeShowPopup(MouseEvent e)
    {
	if (e.isPopupTrigger() || e.isAltDown() || e.isAltGraphDown() || e.isControlDown()) 
	{
	    

	    popup.show(e.getComponent(),
		       e.getX(), e.getY());
	}

    }



    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  options
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    OptionsDialog od = null;

    public void createOptionsDialog()
    {
	od = new OptionsDialog();
	od.pack();
	od.setVisible(true);
    }

    public class OptionsDialog extends JFrame
    {
	public OptionsDialog()
	{
	    super("CodeRunner Options");
	    
	    mview.decorateFrame( this );

	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	    panel.setPreferredSize(new Dimension(460, 300));
	    getContentPane().add(panel, BorderLayout.CENTER);

	    GridBagLayout gridbag = new GridBagLayout();
	    panel.setLayout(gridbag);
	    
	    int line = 0;

	    {
		JLabel label = new JLabel("Compiler  ");
		panel.add(label);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		//c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(label, c);
	    }
	    {
		compiler_path_jtf = new JTextField(30);
		compiler_path_jtf.setText(compiler_path_str);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(compiler_path_jtf, c);

		panel.add(compiler_path_jtf);
	    }

	    {
		JButton jb = new JButton("Browse");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JFileChooser fc = mview.getFileChooser();
			    fc.setSelectedFile(new File(compiler_path_jtf.getText()));
			    if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			    {
				compiler_path_jtf.setText(fc.getSelectedFile().getPath());
			    }
			}
		    });
		
		jb.setFont(mview.getSmallFont());
		jb.setMargin(new Insets(0,0,0,0));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(jb, c);

		panel.add(jb);
	    }

	    line++;
	    {
		use_jit_jchkb = new JCheckBox("Use internal JIT compiler if possible");
		use_jit_jchkb.setSelected(use_jit);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(use_jit_jchkb, c);

		panel.add(use_jit_jchkb);
	    }
	    line++;
	    {
		clean_errors_jchkb = new JCheckBox("Attempt to parse error messages");
		clean_errors_jchkb.setSelected(clean_errors);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(clean_errors_jchkb, c);

		panel.add(clean_errors_jchkb);
	    }
	    line++;

	    {
		JLabel label = new JLabel("Flags  ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);

		panel.add(label);
	    }
	    {
		compiler_flags_jtf = new JTextField(30);
		compiler_flags_jtf.setText(compiler_flags_str);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(compiler_flags_jtf, c);

		panel.add(compiler_flags_jtf);
	    }
	    line++;
	    
	    {
		JLabel label = new JLabel("Time limit ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);

		panel.add(label);
	    }
	    {
		compiler_time_limit_jtf = new JTextField(10);
		compiler_time_limit_jtf.setText(String.valueOf(compiler_time_limit));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(compiler_time_limit_jtf, c);

		panel.add(compiler_time_limit_jtf);
	    }
	    {
		JLabel label = new JLabel("(seconds)");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		
		panel.add(label);
	    }
	    line++;
	    
	    {
		JLabel label = new JLabel("Classpath  ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		//c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(label, c);

		panel.add(label);
	    }
	    {
		compiler_classpath_jta = new JTextArea(30, 5);

		compiler_classpath_jta.setLineWrap(false);
		compiler_classpath_jta.setText(compiler_classpath_str);
	    
		//Font font = new Font("Courier", Font.PLAIN, 12);
		//view_panel.setFont(font);

		JScrollPane scroller = new JScrollPane(compiler_classpath_jta);
		scroller.setPreferredSize(new Dimension(200, 100));

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		//c.gridheight = 3;
		c.weightx = 1.0;
		c.weighty = 3.0;
		gridbag.setConstraints(scroller, c);

		panel.add(scroller);
	    }
	    line++;

	    {
		JLabel label = new JLabel("Library Path  ");
		panel.add(label);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		//c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(label, c);
	    }
	    {
		library_path_jtf = new JTextField(30);
		library_path_jtf.setText(library_path_str);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(library_path_jtf, c);

		panel.add(library_path_jtf);
	    }
	    line++;

	    line += 4;
	    
	    {
		JPanel button_panel = new JPanel();
		button_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		GridBagLayout button_gridbag = new GridBagLayout();
		button_panel.setLayout(button_gridbag);
		
		{
		    final JButton jb = new JButton(" Cancel ");
		    button_panel.add(jb);
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				setVisible(false);
			    }
			});
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 0;
		    //c.anchor = GridBagConstraints.CENTER;
		    //c.weightx = 1.0;
		    //c.weighty = 1.0;
		    button_gridbag.setConstraints(jb, c);
		}
		{
		    final JButton jb = new JButton(" Apply ");
		    button_panel.add(jb);
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				use_jit = use_jit_jchkb.isSelected();
				clean_errors = clean_errors_jchkb.isSelected();
				compiler_path_str = compiler_path_jtf.getText();
				compiler_flags_str = compiler_flags_jtf.getText();
				compiler_classpath_str = compiler_classpath_jta.getText();

				try
				{
				    compiler_time_limit = new Integer(compiler_time_limit_jtf.getText()).intValue();
				}
				catch(NumberFormatException nfe)
				{
				    mview.alertMessage("Compile time limit must a whole number greater than 0");				    
				}

				if(!library_path_str.equals(library_path_jtf.getText()))
				{
				    saveLibraryDetailsToFile(library_path_str);
				    library_path_str = library_path_jtf.getText();
				    loadLibraryDetailsFromFile(library_path_str);
				    buildList();
				    updateLibraryButtons();
				}

				saveDefaultOptions();
				setVisible(false);
			    }
			});
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 0;
		    //c.anchor = GridBagConstraints.CENTER;
		    //c.weightx = 1.0;
		    //c.weighty = 1.0;
		    button_gridbag.setConstraints(jb, c);
		}
		{
		    final JButton jb = new JButton(" Help ");
		    button_panel.add(jb);
		    jb.setEnabled(false);
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				mview.getPluginHelpTopic("CodeRunner", "CodeRunner", "#options");
			    }
			});
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 2;
		    c.gridy = 0;
		    //c.anchor = GridBagConstraints.CENTER;
		    //c.weightx = 1.0;
		    //c.weighty = 1.0;
		    button_gridbag.setConstraints(jb, c);
		}
		
		// add the button panel
		{
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = line;
		    c.gridwidth = 2;
		    c.anchor = GridBagConstraints.SOUTH;
		    c.weightx = 1.0;
		    c.weighty = 2.0;
		    gridbag.setConstraints(button_panel, c);
		    panel.add(button_panel);
		}
	    }
	    
	    
	}
    }    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  code library
    // --- --- ---  (precompiled classes)
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    Vector library_v = new Vector();

    JList library_list = null;
    //CodeLibDialog cld = null;
    DefaultListModel library_list_model = null;
    int current_library_entry = -1;

    private void buildList()
    {
	if(library_v == null)
	    return;

	library_list_model = new DefaultListModel();
	
	for(int lp=0; lp < library_v.size(); lp++)
	{
	    library_list_model.addElement(((CompiledUserClass) library_v.elementAt(lp)).name);
	}

	if(library_list != null)
	{
	    library_list.setModel(library_list_model);
	    updateLibraryButtons();
	}
    }
    
    // return true if it is safe to replace the current code (i.e. it has been saved since the last edit)
    //
    private boolean safeToReplaceCode()
    {
	if(needs_save)
	{
	    return (mview.infoQuestion("Current code is not saved, really replace it?", "Yes", "No") == 0);
	}
	return true;
    }

    // in response to a double click in the library list
    //
    private void runFromLibrary(int lpi)
    {
	if(!safeToReplaceCode())
	    return;

	CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(lpi);

	if(debug_library)
	    System.out.println("running " + cuc.name + " from " + cuc.class_name);

	// maybe its already there?
	//
	if(current_library_entry != lpi)
	{
	    if(debug_library)
		System.out.println("copying source code to editor");
	    
	    loadFileIntoTextArea(new File(cuc.code_name), false);
	    current_library_entry = lpi;
	}
			     
	code_status = CodeEdited;

	String user_class_file_name = user_class_dir + File.separatorChar + "CustomCodeWrapper.class";
	
	if(debug_library)
	    System.out.println("copying byte code to run location '" + user_class_file_name + "'");

	copyFile(cuc.class_name, user_class_file_name);

	code_status = CodeCompiled;

	//needs_save = false; // not needed any more..

	loadAndRunUserClass();
    }

    // in response to a selection in the library list
    //
    private void loadFromLibrary(int lpi)
    {
	
	if((lpi < 0) || (lpi >= library_v.size()))
	    return;

	if(!safeToReplaceCode())
	    return;

	CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(lpi);

	if(debug_library)
	    System.out.println("loading " + cuc.name + " from " + cuc.class_name);

	loadFileIntoTextArea(new File(cuc.code_name), false);
			     
	code_status = CodeEdited;
	needs_save = false;

	current_library_entry = lpi;

    }

    private void renameSelected()
    {
	int[] sels = library_list.getSelectedIndices();
	if(sels.length == 1)
	{
	    CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(sels[0]);

	    try
	    {
		String name = mview.getString("New name for '" + cuc.name + "'", cuc.name);
		if(name != null)
		{
		    //
		    // make sure this name is unique
		    //
		    while( libraryHasEntryCalled( name ) )
		    {
			mview.alertMessage("There is already an entry called '" + name + "'.\nEach entry must have a unique name." );
			name = mview.getString("New name for '" + cuc.name + "'", cuc.name);
		    }
		    
		    cuc.name = name;
		    buildList();
		    library_list.setSelectedIndex(sels[0]);
		}
	    }
	    catch(UserInputCancelled e)
	    {
	    }
	}
	else
	{
	    Toolkit.getDefaultToolkit().beep();
	}
    }

    private void lowerSelected()
    {
	int[] sels = library_list.getSelectedIndices();
	if(sels.length == 1)
	{
	    int id = sels[0];
	    if((id+1) < library_list.getModel().getSize())
	    {
		CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(id);
		library_v.removeElementAt(id);
		library_v.insertElementAt(cuc, id+1);
		
		buildList();

		library_list.setSelectedIndex(id+1);
	    }
	}
	else
	{
	    Toolkit.getDefaultToolkit().beep();
	}
    }

    private void raiseSelected()
    {
	int[] sels = library_list.getSelectedIndices();
	if(sels.length == 1)
	{
	    int id = sels[0];
	    if(id > 0)
	    {
		CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(id);
		library_v.removeElementAt(id);
		library_v.insertElementAt(cuc, id-1);

		library_list.setSelectedIndex(id-1);

		buildList();
	    }
	}
	else
	{
	    Toolkit.getDefaultToolkit().beep();
	}
    }
    private void removeSelectedFromLibrary()
    {
	int[] sels = library_list.getSelectedIndices();
	if(sels.length > 0)
	{
	    if(mview.infoQuestion( "Really remove " + 
				   ((sels.length == 1) ? "this entry" : ("these " + sels.length + " entries")) +
				   "?",
				   "Yes", "No") == 0)
	    {
		for(int s=0; s < sels.length; s++)
		{
		    // rememeber to adjust indices as we remove things from the
		    // vector....
		    library_v.removeElementAt(sels[s] - s);
		}
		
		buildList();
	    }
	}
	else
	{
	    Toolkit.getDefaultToolkit().beep();
	}

    }

    private void addCurrentToLibrary()
    {
	if(code_status == CodeEdited)
	{
	    //mview.alertMessage("The current code is not compiled.\nYou must 'Run' it before you can add it to the library");

	    if(mview.infoQuestion("The current code is not compiled.\nCompile it and add to library?", "Yes", "No") == 1)
		return;

	    if(saveUserClass() != 1)
	    {
		mview.alertMessage("Unable to save code. Not added to library");
		return;
	    }
	    
	    // compile the class but don't run it....
	    int result = compileUserClass();
	    

	    if(result != 0)
	    {
		mview.alertMessage("Compile failed. Not added to library");
		return;
	    }
	    else
	    {
		code_status = CodeCompiled;
		needs_save = false;
	    }

	}
	
	if( ensureThatCodeLibraryExists() == false )
	    return;
	    
	try
	{
	    String name = mview.getString("Name for new entry");
	    
	    
	    if(name != null)
	    {
		//
		// make sure this name is unique
		//
		while( libraryHasEntryCalled( name ) )
		{
		    mview.alertMessage("There is already an entry called '" + name + "'.\nEach entry must have a unique name." );
		    name = mview.getString("Name for new entry", name);
		}
		


		CompiledUserClass cuc = new  CompiledUserClass();
		cuc.name = name;
	    
		//
		// generate a suitable class name
		//
		Hashtable existing_names = new Hashtable();

		File library_path = new File(library_path_str);

		File[] existing_files =  library_path.listFiles();

		if(existing_files != null)
		{
		    for(int f=0; f < existing_files.length; f++)
		    {
			existing_names.put(existing_files[f].getName(), "x");
			
			//System.out.println(existing_files[f].getName() + " already used...");
		    }
		}

		boolean unique = false;
		int icount = 0;
		String uname = null;

		final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		while((!unique) && (++icount < 1000))
		{
		    uname = "cr";
		    
		    for(int c=0; c < 10; c++)
		    {
			//uname += new Character(((int)(Math.random() * 26) + (Char.getNumericValue('A')))).charValue();
			uname += alpha.charAt((int)(Math.random() * 26.0));
		    }
		    
		    unique = (existing_names.get(uname + ".uc") == null);

		    // System.out.println(uname + " is " + (unique ? "unique" : "not unique"));
		}
		
		if(!unique)
		{
		    mview.errorMessage("Unable to generate unique name. Too many entries in this library?");
		    return;
		}

		cuc.class_name = library_path_str + File.separatorChar + uname + ".uc";
		cuc.code_name  = library_path_str + File.separatorChar + uname + ".uj";
		
		try
		{	
		    String user_code = code_jta.getText();
		    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(cuc.code_name)));
		    writer.write(user_code);
		    writer.close();
		    
		    // copy the current compiled class to the new name
		    //
		    String user_class_file_name = user_class_dir + File.separatorChar + "CustomCodeWrapper.class";
		    copyFile(user_class_file_name, cuc.class_name);
		    
		    library_v.addElement(cuc);
		    buildList();

		    int li = library_list.getModel().getSize() - 1;
		    library_list.setSelectedIndex(li);
		    current_library_entry = li;
		    
		    saveLibraryDetailsToFile(library_path_str);

		    if(debug_library)
		    {
			System.out.println(" code saved in " + cuc.code_name);
			System.out.println("class saved in " + cuc.class_name);
		    }
		    
		}
		catch (java.io.IOException ioe)
		{
		    mview.alertMessage("Unable to write code to library\nError: " + ioe);
		}
	    }
	}
	catch (UserInputCancelled e)
	{
	    // no action
	}
    }

    // the same as addCurrentToLibrary except it uses the C.U.C of the current 
    // selection in the library list
    //
    private void updateCurrentToLibrary()
    {
	int[] sels = library_list.getSelectedIndices();
	if(sels.length == 1)
	{

	    if(code_status == CodeEdited)
	    {
		//mview.alertMessage("The current code is not compiled.\nYou must 'Run' it before you can add it to the library");
		
		if(mview.infoQuestion("The current code is not compiled.\nCompile it then update library?", "Yes", "No") == 1)
		    return;
		
		if(saveUserClass() != 1)
		{
		    mview.alertMessage("Unable to save code. Not added to library");
		    return;
		}
	
		// compile the class but don't run it....
		int result = compileUserClass();
		
		
		if(result != 0)
		{
		    mview.alertMessage("Compile failed. Library version not updated");
		    return;
		}
		else
		{
		    //mview.successMessage("Compiled and library updated with new version");
		    code_status = CodeCompiled;
		    needs_save = false;
		}
		
		if( ensureThatCodeLibraryExists() == false )
		    return;
	    
	    }
	    
	    CompiledUserClass cuc = (CompiledUserClass) library_v.elementAt(sels[0]); 
	    
	    if(debug_library)
		System.out.println("updating " + cuc.name + " into " + cuc.class_name);

	    
	    String library_class_name = cuc.class_name ;
	    String library_code_name = cuc.code_name ;

	    try
	    {	
		String user_code = code_jta.getText();
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(library_code_name)));
		writer.write(user_code);
		writer.close();

		// copy the current compiled class to the new name
		//
		String user_class_file_name = user_class_dir + File.separatorChar + "CustomCodeWrapper.class";
		copyFile(user_class_file_name, library_class_name);

		mview.successMessage("Library updated with new version");

		if(debug_library)
		{
		    System.out.println(" code updated in " + cuc.code_name);
		    System.out.println("class updated in " + cuc.class_name);
		}

	    }
	    catch (java.io.IOException ioe)
	    {
		mview.alertMessage("Unable to update code to library\nError: " + ioe);
	    }
	}
    }

    private boolean libraryHasEntryCalled( String name )
    {
	DefaultListModel dlm = (DefaultListModel) library_list.getModel();
	
	if( dlm == null )
	    return false;
	
	return ( dlm.indexOf( name ) >= 0 );
    }
    
    private boolean ensureThatCodeLibraryExists()
    {
	File test = new File ( library_path_str );

	if( test.exists() == false )
	{
	    if( test.mkdirs() == false )
	    {
		mview.alertMessage("Unable to create the 'code-library' directory\n" + 
				   "(in '" + library_path_str + "'" );
		
		return false;
	    }
	    else
	    {
		System.out.println("Information: code library created in '" + library_path_str + "'" );
	    }

	}
	
	if( test.canWrite() == false )
	{
	    mview.alertMessage("Unable to write files in 'code-library' directory\n" + 
			       "(which is '" + library_path_str + "')." + 
			       "Ensure that this directory exists and has suitable\n" + 
			       "write permissions then try again." );
	    
	    return false; 
	}

	return true;
	
    }

    private boolean copyFile(String src, String dest)
    {
	try
	{
	    //BufferedWriter writer = new BufferedWriter(new FileWriter(new File(user_class_file_name)));
	    File ifile = new File(src);
	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(ifile));
	    File ofile = new File(dest);
	    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(ofile));
	    
	    int file_length = (int) ifile.length();
	    
	    byte b[] = new byte[file_length];
	    
	    if(bis.read(b, 0, file_length) == file_length)
	    {
		bis.close();
		bos.write(b, 0, file_length);
		//mview.infoMessage(src + " copied to " + dest + " (" + file_length + " bytes)");
		bos.close();
		return true;
	    }
	}
	catch(IOException ioe)
	{
	    mview.alertMessage("Cannot copy files\n  " + ioe);
	}
	return false;
    }
    
    private void loadLibraryDetailsFromFile(String path_name)
    {
	if(debug_library)
	    System.out.println("loading library from " + path_name);
	
	library_v = new Vector();

	try
	{
	    FileInputStream fis = new FileInputStream(new File(path_name + File.separatorChar + "CodeRunnerLibrary.dat"));
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    library_v = (Vector) ois.readObject();
	    ois.close();
	}
	catch(FileNotFoundException ioe)
	{
	    // no problem, it just means the plugin has not been run before

	}
	catch(ClassNotFoundException fnfe)
	{
	    mview.errorMessage("Cannot understand library details file\n  " + fnfe);

	}
	catch(IOException ioe) // other than FileNotFound
	{
	    mview.errorMessage("Cannot load library details file\n  " + ioe);
	}
    }
    
    private void saveLibraryDetailsToFile(String path_name)
    {
	if(debug_library)
	    System.out.println("saving library to " + path_name);
	
	try
	{
	    FileOutputStream fos = new FileOutputStream(new File(path_name + File.separatorChar + "CodeRunnerLibrary.dat"));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(library_v);
	    oos.flush();
	    oos.close();
	}
	catch(IOException ioe)
	{
	    mview.errorMessage("Cannot save library details file\n  " + ioe);
	}
    }

 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	ActionListener al = null;
	JMenuItem jmi = null;

	frame.getContentPane().setLayout(new BorderLayout());

	popup = new JPopupMenu();

	JToolBar tool_bar = new JToolBar();
	tool_bar.setFloatable(false);

	run_jb = new JButton("Run");
	jmi = new JMenuItem("Run");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    if(ucrt == null)
		    {
			compileLoadAndRunUserClass();
		    }
		    else
		    {
			ucrt.stop();
			ucrt = null;
			mview.infoMessage("Code fragment terminated");
		    }
		}
	    };
	
	run_jb.addActionListener(al);
	jmi.addActionListener(al);

	popup.add(jmi);
	tool_bar.add(run_jb);
	tool_bar.addSeparator();

	JButton jb = new JButton("Load");
	jmi = new JMenuItem("Load");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    loadUserCode();
		}
	    };
	popup.addSeparator();
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.add(jmi);
	tool_bar.add(jb);

	tool_bar.addSeparator();
		
	jb = new JButton("Insert");
	jmi = new JMenuItem("Insert");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    insertUserCode();
		}
	    };
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.add(jmi);
	tool_bar.add(jb);

	tool_bar.addSeparator();
		
	jb = new JButton("Reset");
	jmi = new JMenuItem("Reset");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    resetUserCode();
		}
	    };
	popup.addSeparator();
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.add(jmi);
	tool_bar.add(jb);

	tool_bar.addSeparator();
	
	jb = new JButton("Save");
	jmi = new JMenuItem("Save");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    saveUserCode();
		}
	    };
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.add(jmi);
	tool_bar.add(jb);

	tool_bar.addSeparator();
		
	jb = new JButton("Options");
	jmi = new JMenuItem("Options");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    createOptionsDialog();
		}
	    };
	popup.addSeparator();
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.add(jmi);
	tool_bar.add(jb);

	tool_bar.add(Box.createHorizontalGlue());

	popup.addSeparator();
	popup.add(undo_action);
	tool_bar.add(undo_action);

	popup.add(redo_action);
	tool_bar.add(redo_action);
	popup.addSeparator();
	

	tool_bar.add(Box.createHorizontalGlue());

	jb = new JButton("Help");
	jmi = new JMenuItem("Help");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic("CodeRunner", "CodeRunner");
		}
	    };
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.add(jmi);
	tool_bar.add(jb);

	tool_bar.addSeparator();

	jb = new JButton("Close");
	jmi = new JMenuItem("Close");
	al = new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    if(needs_save)
		    {
			if(mview.infoQuestion("Current code has not been saved.\nSave it now?", "No", "Yes") == 1)
			    saveUserCode();
		    }
		    cleanUp();
		}
	    };
	jb.addActionListener(al);
	jmi.addActionListener(al);
	popup.addSeparator();
	popup.add(jmi);
	tool_bar.add(jb);

	frame.getContentPane().add(tool_bar, BorderLayout.NORTH);

        int w = mview.getIntProperty("coderun.width", 500);
	int h = mview.getIntProperty("coderun.height", 300);
	
	code_jta = new JTextArea(256,500);
	code_jta.setPreferredSize(new Dimension(w, h));

	code_jta.getDocument().addDocumentListener(new CodeChangedListener());
	//code_jta.setPreferredSize(new Dimension(400, 300));
	
	code_jta.setText(default_code_body);
	code_jta.setFont(new Font("Courier", Font.PLAIN, 12));

	undo_man = new UndoManager();

	code_jta.getDocument().addUndoableEditListener(new CustomUndoableEditListener());

	code_jta.addMouseListener( new PopupListener() );

	
	needs_save = false;

	code_jsp = new JScrollPane(code_jta);
	code_jsp.setPreferredSize(new Dimension(w, h)); //

	// ================

	JPanel wrapper = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	wrapper.setLayout(gridbag);
	GridBagConstraints c = null;

	ButtonGroup bg = new ButtonGroup();
	
	JCheckBox jchkb = new JCheckBox("Library");
	jchkb.setSelected(true);
	jchkb.setFont(mview.getSmallFont());
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    displayLibrary();
		}
	    });
	bg.add(jchkb);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	gridbag.setConstraints(jchkb, c);
	wrapper.add(jchkb);

	jchkb = new JCheckBox("Methods");
	jchkb.setFont(mview.getSmallFont());
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    displayMethodTree();
		}
	    });
	bg.add(jchkb);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	gridbag.setConstraints(jchkb, c);
	wrapper.add(jchkb);

	jchkb = new JCheckBox("Plugin commands");
	jchkb.setFont(mview.getSmallFont());
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    displayPluginCommandTree();
		}
	    });
	bg.add(jchkb);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jchkb, c);
	wrapper.add(jchkb);


	list_and_tree_wrapper = new JPanel();
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	c.gridwidth = 2;
	gridbag.setConstraints(list_and_tree_wrapper, c);
	wrapper.add(list_and_tree_wrapper);

	// ================

	
	h_split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	h_split_pane.setLeftComponent(wrapper);
	h_split_pane.setRightComponent(code_jsp);

	displayLibrary();

	
	frame.getContentPane().add(h_split_pane);
    }

    private void updateLibraryButtons()
    {
	if(library_list == null)
	    return;

	int[] sels = library_list.getSelectedIndices();

	add_to_lib_jb.setEnabled(true);

	raise_jb.setEnabled((sels.length == 1) && (sels[0] > 0));
	lower_jb.setEnabled((sels.length == 1) && ((sels[0]+1) < library_list.getModel().getSize()));
	
	update_entry_jb.setEnabled(sels.length == 1);
	rename_entry_jb.setEnabled(sels.length == 1);
	del_from_lib_jb.setEnabled(sels.length > 0);

    }

    private void displayLibrary()
    {
	if(library_list == null)
	{
	    library_list = new JList();
	    
	    MouseListener mouseListener = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			updateLibraryButtons();
			int index = library_list.locationToIndex(e.getPoint());
			if (e.getClickCount() == 2) 
			{
			    runFromLibrary(index);
			}
			else
			{
			    loadFromLibrary(index);
			}
			
		    }
	    };
	    
	    library_list.addMouseListener(mouseListener);
	}

	JPanel wrapper = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	wrapper.setLayout(gridbag);
	
	
	JScrollPane library_jsp = new JScrollPane(library_list);
	library_jsp.setPreferredSize(new Dimension(100, 300));
	
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	c.gridwidth = 2;
	gridbag.setConstraints(library_jsp, c);
	wrapper.add(library_jsp);
	
	raise_jb = new JButton("Raise");
	raise_jb.setToolTipText("Move the selected entry up the list");
	Font f = raise_jb.getFont();
	Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	
	raise_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    raiseSelected();
		}
	    });
	raise_jb.setFont(small_font);
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints(raise_jb, c);
	
	wrapper.add(raise_jb);
	
	lower_jb = new JButton("Lower");
	lower_jb.setToolTipText("Move the selected entry down the list");
	lower_jb.setFont(small_font);
	lower_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    lowerSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints(lower_jb, c);
	
	wrapper.add(lower_jb);
	
	add_to_lib_jb = new JButton("Add");
	add_to_lib_jb.setToolTipText("Add the current code to the library");
	add_to_lib_jb.setFont(small_font);
	add_to_lib_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    addCurrentToLibrary();
		}
	    });
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints(add_to_lib_jb, c);
	
	wrapper.add(add_to_lib_jb);
	
	update_entry_jb = new JButton("Update");
	add_to_lib_jb.setToolTipText("Update the code of the selected library entry");
	update_entry_jb.setFont(small_font);
	update_entry_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    updateCurrentToLibrary();
		}
	    });
	
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints(update_entry_jb, c);
	
	wrapper.add(update_entry_jb);
	
	del_from_lib_jb = new JButton("Remove");
	del_from_lib_jb.setFont(small_font);
	del_from_lib_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    removeSelectedFromLibrary();
		}
		});
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 3;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints(del_from_lib_jb, c);
	
	wrapper.add(del_from_lib_jb);
	
	rename_entry_jb = new JButton("Rename");
	rename_entry_jb.setFont(small_font);
	rename_entry_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    renameSelected();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 3;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints(rename_entry_jb, c);
	
	wrapper.add(rename_entry_jb);
	
	list_and_tree_wrapper.removeAll();
	
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	gridbag = new GridBagLayout();
	list_and_tree_wrapper.setLayout(gridbag);
	gridbag.setConstraints(wrapper, c);
	list_and_tree_wrapper.add(wrapper);

	list_and_tree_wrapper.revalidate();
	h_split_pane.revalidate();

	buildList();
    }

    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=
    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=
    //
    // The Method Tree
    // 
    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=
    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=

    JTextField c_jtf, m_jtf, a_jtf, t_jtf;

    //Hashtable method_args_ht, method_types_ht;
    //int total_n_tree_rows;

    Vector tree_nodes;
    Hashtable node_to_method;

    private class MethodData
    {
	public String class_name;
	public String return_type;
	public String name;
	public String args;
	
	public MethodData( String cn, String rt, String na, String ar )
	{
	    class_name = cn; return_type = rt; name = na; args = ar;
	}
    }

    private void displayMethodTree()
    {
	JPanel wrapper = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	wrapper.setLayout(gridbag);
	
	if(method_tree == null)
	{
	    method_tree = new JTree();
	    
	    fillMethodTree( method_tree );

	    /*
	    method_tree.addTreeSelectionListener(new TreeSelectionListener() 
		{
		    public void valueChanged(TreeSelectionEvent e) 
		    {
			treeSelectionHasChanged();
		    }
		});
	    */

	    MouseListener mouseListener = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			if (e.getClickCount() == 2) 
			{
			    insertTreeSelection();
			}
			else
			{
			    treeSelectionHasChanged();
			}

		    }
		};
	    method_tree.addMouseListener(mouseListener);
	}
	
	
	JScrollPane jsp = new JScrollPane(method_tree);
	
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	c.gridwidth = 2;
	gridbag.setConstraints(jsp, c);
	wrapper.add(jsp);


	
	JLabel label = new JLabel("Class ");
	label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	wrapper.add(label);

	c_jtf = new JTextField(15);
	c_jtf.setEditable(false);
	c_jtf.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(c_jtf, c);
	wrapper.add(c_jtf);



	label = new JLabel("Type ");
	label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	wrapper.add(label);

	t_jtf = new JTextField(15);
	t_jtf.setFont(mview.getSmallFont());
	t_jtf.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    m_jtf.setText("");
		    a_jtf.setText("");
		    findMethod();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(t_jtf, c);
	wrapper.add(t_jtf);



	label = new JLabel("Name ");
	label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 3;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	wrapper.add(label);

	m_jtf = new JTextField(15);
	m_jtf.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    t_jtf.setText("");
		    a_jtf.setText("");
		    findMethod();
		}
	    });
	m_jtf.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 3;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(m_jtf, c);
	wrapper.add(m_jtf);


	
	label = new JLabel("Args ");
	label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 4;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	wrapper.add(label);

	a_jtf = new JTextField(15);
	a_jtf.setFont(mview.getSmallFont());
	a_jtf.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    m_jtf.setText("");
		    t_jtf.setText("");
		    findMethod();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 4;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(a_jtf, c);
	wrapper.add(a_jtf);


	JButton jb = new JButton("Find");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    findMethod();
		}
	    });
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 5;
	c.gridwidth = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(jb, c);
	wrapper.add(jb);

	list_and_tree_wrapper.removeAll();
	
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	gridbag = new GridBagLayout();
	list_and_tree_wrapper.setLayout(gridbag);
	gridbag.setConstraints(wrapper, c);
	list_and_tree_wrapper.add(wrapper);
	
	list_and_tree_wrapper.revalidate();
	h_split_pane.revalidate();
    }

    private void treeSelectionHasChanged()
    {
	TreePath tp = method_tree.getSelectionPath();

	if(tp == null)
	    return;

	DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();

	if(dmtn == null)
	    return;

	if(dmtn.getChildCount() > 0)
	{
	    c_jtf.setText( (String) dmtn.getUserObject() );
	    m_jtf.setText( "" );
	    t_jtf.setText( "" );
	    a_jtf.setText( "" );
	    return;
	}
	
	MethodData md = (MethodData) node_to_method.get( dmtn );

	DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dmtn.getParent();

	c_jtf.setText( md.class_name );
	m_jtf.setText( md.name );
	t_jtf.setText( md.return_type );
	a_jtf.setText( md.args );
    }
    
    private void insertTreeSelection()
    {
	TreePath tp = method_tree.getSelectionPath();
	DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();

	if(dmtn == null)
	    return;

	if(dmtn.getChildCount() == 0)
	{
	    MethodData md = (MethodData) node_to_method.get( dmtn );

	    String args = "( ";
	    
	    if(md.args != null)
		args += md.args;
	    
	    args += " )";

	    // insert the argument types if any

	    String v_name = "";
	    if(md.class_name.equals("DataPlot"))
		v_name = "dplot.";
	    if(md.class_name.equals("ExprData"))
		v_name = "edata.";
	    if(md.class_name.equals("maxdView"))
		v_name = "mview.";
	      
	    /*
	    // prefix with ' = ' if the method has a non-void return type

	    String m_type = (m_name == null) ? null : (String) method_type_ht.get( c_name + "." + m_name );
	    if((m_type != null) && (!m_type.equals("void")))
		v_name = " = " + v_name;
	    */

	    code_jta.insert( v_name + md.name + args, code_jta.getCaretPosition() );
	}
    }

    private void findMethod( )
    {
	final boolean debug = false;

	if((tree_nodes == null) || (tree_nodes.size() == 0))
	    return;

	int start_pos = 0;

	TreePath tp = method_tree.getSelectionPath();
	if(tp != null)
	{
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();
	    
	    start_pos = tree_nodes.indexOf( dmtn );
	}

	if(start_pos < 0)
	    start_pos = 0;

	if(debug)
	    System.out.println("starting at " + start_pos);
		
	int pos = start_pos+1;
	if(pos >= tree_nodes.size())
	    pos = 0;

	boolean found = false;
	TreePath hit_tp = null;
	int checked = 0;

	// note: cannot use tree rows as planned (otherwise collapsed branches get ignored!)

	String mt = m_jtf.getText().toLowerCase();
	String tt = t_jtf.getText().toLowerCase();
	String at = a_jtf.getText().toLowerCase();
		
	while(!found && (pos != start_pos))
	{
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tree_nodes.elementAt( pos );
	    
	    if(dmtn.getChildCount() == 0)
	    {
		MethodData md = (MethodData) node_to_method.get( dmtn );

		checked++;
		
		found = true;
		
		if((mt != null) && (mt.length() > 0))
		{
		    if(debug)
			System.out.println( md.name + " -n- " + mt);
		    
		    if(md.name.toLowerCase().indexOf(mt) < 0)
			found = false;
		}
		
		if((tt != null) && (tt.length() > 0))
		{
		    String mtype = md.return_type;
		    
		    if(debug)
			System.out.println( mtype + " -t- " + tt);
		    
		    if((mtype == null) || (mtype.toLowerCase().indexOf(tt) < 0))
			found = false;
		}
		
		if((at != null) && (at.length() > 0))
		{
		    String margs = md.args;
		    
		    if(debug)
			System.out.println( margs + " -a- " + at);
		    
		    if((margs == null) || (margs.toLowerCase().indexOf(at) < 0))
			found = false;
		}

		if(found)
		{
		    DefaultTreeModel dtm = (DefaultTreeModel) method_tree.getModel();
		    tp = new TreePath( dtm.getPathToRoot( dmtn ));

		     if(debug)
			 System.out.println("found....");
		     method_tree.setSelectionPath( tp );
		     method_tree.expandPath( tp );
		     method_tree.scrollPathToVisible( tp );

		     c_jtf.setText( md.class_name );
		}
		
	    }
 
	    pos++;

	    if(pos >= tree_nodes.size())
	    {
		if(debug)
		    System.out.println("reset to start after " + pos + " nodes");
		
		pos = 0;
	    }

	}

	if(debug)
	{
	    System.out.println(checked + " checked");
	    
	    if(!found)
		System.out.println("not found....");
	}

	
    }

    private void fillMethodTree( JTree tree )
    {
	String name = mview.getHelpDirectory() + "method-ref.dat";

	tree_nodes = new Vector();
	node_to_method = new Hashtable();

	try
	{ 
	    BufferedReader br = new BufferedReader(new FileReader(new File(name)));
	    
	    DefaultMutableTreeNode dmtn = readMethods( null, br );
	    if(dmtn == null)
		return;

	    DefaultTreeModel model =  new DefaultTreeModel( dmtn );
	    tree.setModel(model);
	    
	    tree.putClientProperty("JTree.lineStyle", "Angled");
	    
	    DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	    renderer.setLeafIcon(null);
	    renderer.setOpenIcon(null);
	    renderer.setClosedIcon(null);
	    tree.setCellRenderer(renderer);
	}
	catch(FileNotFoundException fnfe)
	{ 
	    mview.errorMessage("Cannot open file ' " + name + "' (not found)\n  " + fnfe);
	    return;
	}
	catch (IOException ioe) 
	{ 
	    mview.errorMessage("File ' " + name + "' broken\n  " + ioe);
	    return;
	}
    }

    private DefaultMutableTreeNode readMethods( String name, BufferedReader br ) throws IOException
    {
	DefaultMutableTreeNode node = new DefaultMutableTreeNode( name == null ? "Top" : name );

	String str = br.readLine();
	while(str != null)
	{
	    if(str.startsWith( "#START" ))
	    {
		String cname = str.substring(6).trim();

		DefaultMutableTreeNode subnode = readMethods( cname, br );
		
		node.add( subnode );

	    }
	    
	    if(str.startsWith( "#DONE" ))
	    {
		return node;
	    }

	    if(!str.startsWith( "#" ))
	    {
		int first  = str.indexOf('\t');
		int second = str.indexOf('\t', first+1);

		if(first >= 0)
		{
		    String cname = name.trim();
		    String mname = str.substring( 0, first ).trim();
		    
		    //String fullname = cname + "." + mname;
		    
		    String mtype = ((second >= 0) && (second > first)) ? str.substring( first+1, second ).trim() : null;
		    
		    String margs = ((second >= 0) && ((second+1) < str.length())) ? str.substring( second+1 ).trim() : null;

		    // System.out.println(mname + "::" +  mtype+ "::" + margs);

		    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode( mname );

		    MethodData md = new MethodData( cname, mtype, mname, margs );

		    tree_nodes.addElement( dmtn );
		    node_to_method.put( dmtn, md );

		    node.add( dmtn );
		}
	    }

	    str = br.readLine();
	}

	return node;
    }

    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=
    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=
    //
    // The Plugin Command Tree
    // 
    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=
    // =--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=

    private void displayPluginCommandTree()
    {
	JPanel wrapper = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	wrapper.setLayout(gridbag);

	if(pc_tree == null)
	{
	    pc_tree = new JTree();
	    
	    fillPluginCommandTree( pc_tree );


	}    

	JScrollPane jsp = new JScrollPane(pc_tree);
	
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	c.gridwidth = 2;
	gridbag.setConstraints(jsp, c);
	wrapper.add(jsp);

	JButton jb = new JButton("Insert template");
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    insertCommandTemplate();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.gridwidth = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(jb, c);
	wrapper.add(jb);

	list_and_tree_wrapper.removeAll();
	
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	gridbag = new GridBagLayout();
	list_and_tree_wrapper.setLayout(gridbag);
	gridbag.setConstraints(wrapper, c);
	list_and_tree_wrapper.add(wrapper);
	
	list_and_tree_wrapper.revalidate();
	h_split_pane.revalidate();
    }

    private void fillPluginCommandTree( JTree tree )
    {
	DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode();

	Vector coms = mview.getAllCommands();

	Vector plugins = mview.getPluginInfoObjects();
	String[] pnames = new String[ plugins.size() ];
	for(int p=0; p < plugins.size(); p++)
	    pnames[p] = ((PluginInfo) plugins.elementAt(p)).name;
	java.util.Arrays.sort( pnames );
	
	for(int p=0; p < pnames.length; p++)
	{
	    DefaultMutableTreeNode d1 = new DefaultMutableTreeNode( pnames[p] );

	    for(int c=0; c < coms.size(); c++)
	    {
		PluginCommand pc  = (PluginCommand) coms.elementAt(c);

		if(pc.plugin_name.equals(pnames[p]))
		{
		    DefaultMutableTreeNode d2 = new DefaultMutableTreeNode( pc.name );
		    
		    if(pc.args != null)
		    {
			for(int a=0; a < pc.args.length; a+=5)
			{
			    DefaultMutableTreeNode d3 = new DefaultMutableTreeNode( pc.args[a] );
			    
			    DefaultMutableTreeNode d4 = new DefaultMutableTreeNode( "type:" + pc.args[a+1] );
			    d3.add(d4);
			    d4 = new DefaultMutableTreeNode( "comment:" + pc.args[a+4] );
			    d3.add(d4);
			    d4 = new DefaultMutableTreeNode( "default:" + pc.args[a+2] );
			    d3.add(d4);

			    d2.add(d3);

			}
		    }

		    d1.add(d2);
		}
	    }

	    if(d1.getChildCount() > 0)
		dmtn.add(d1);
	}


	// -----------------

	DefaultTreeModel model =  new DefaultTreeModel( dmtn );
	tree.setModel(model);

	tree.putClientProperty("JTree.lineStyle", "Angled");
	
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	renderer.setOpenIcon(null);
	renderer.setClosedIcon(null);
	tree.setCellRenderer(renderer);  
    }

    private void insertCommandTemplate()
    {
	

	// is there a currently selected command in the tree?

	TreePath tp = pc_tree.getSelectionPath();
	DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();

	if(dmtn == null)
	{
	    mview.alertMessage("Templates only exist for commands\nSelect a command in the list.");
 	}
	else
	{
	    int depth = tp.getPathCount();

	    DefaultMutableTreeNode pcnode = dmtn;

	    while(pcnode.getParent() != pcnode.getRoot())
		pcnode = (DefaultMutableTreeNode) pcnode.getParent();

	    
	    String pname = (String) pcnode.getUserObject();
	    String cname = (String) dmtn.getUserObject();

	    //	    System.out.println("plugin=" + pname);
	    
	    StringBuffer res = new StringBuffer();

	    Vector coms = mview.getAllCommands();
	    for(int c=0; c < coms.size(); c++)
	    {
		PluginCommand pc  = (PluginCommand) coms.elementAt(c);
		
		if( pname.equals( pc.plugin_name ) )
		{
		    if( cname.equals( pc.name ))
		    {
			if(pc.args != null)
			{
			    res.append( "String[] args = new String[]\n {\n" );
			    for(int a=0; a < pc.args.length; a+=5)
			    {
				res.append( "  \"" + pc.args[a] + "\", \"" +  pc.args[a+2] + "\",\n" );
			    }
			    res.append( " };\n\n" );
			}
			res.append( "mview.runCommand( \"" + pname + "\", \"" + cname + "\", args );\n\n" );
		    }
		}
	    }

	    code_jta.insert( res.toString(), code_jta.getCaretPosition());
	    
	    
	}

    }

    // -=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--
    
    // undo/redo in the code editor

    protected class CustomUndoableEditListener implements UndoableEditListener
    {
	public void undoableEditHappened(UndoableEditEvent e)
	{
	    //Remember the edit and update the menus
	    undo_man.addEdit(e.getEdit());
	    undo_action.update();
	    redo_action.update();
	}
    }  

    private UndoAction undo_action = new UndoAction();
    private RedoAction redo_action = new RedoAction();

    protected UndoableEditListener undo_handler;

    class UndoAction extends AbstractAction 
    {
        public UndoAction() 
	{
            super("Undo");
            setEnabled(false);
        }
	
        public void actionPerformed(ActionEvent e) 
	{
            try 
	    {
                undo_man.undo();
            } 
	    catch (CannotUndoException ex) 
	    {
                System.out.println("Unable to undo: " + ex);
                ex.printStackTrace();
            }
            update();
            redo_action.update();
        }
	
        protected void update() 
	{
            if(undo_man.canUndo()) 
	    {
                setEnabled(true);
                //putValue(Action.NAME, undo_man.getUndoPresentationName());
            }
            else 
	    {
                setEnabled(false);
                //putValue(Action.NAME, "Undo");
            }
        }
    }

    class RedoAction extends AbstractAction 
    {
        public RedoAction() 
	{
            super("Redo");
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) 
	{
            try 
	    {
                undo_man.redo();
            } 
	    catch (CannotRedoException ex) 
	    {
                System.out.println("Unable to redo: " + ex);
                ex.printStackTrace();
            }
            update();
            undo_action.update();
        }
	
        protected void update() 
	{
            if(undo_man.canRedo()) 
	    {
                setEnabled(true);
                //putValue(Action.NAME, undo_man.getRedoPresentationName());
            }
            else 
	    {
                setEnabled(false);
                //putValue(Action.NAME, "Redo");
            }
        }
    }



    // -=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--=--

    // used for noticing when the code has been edited
    //
    class CodeChangedListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    needs_save = true;
	    code_status = CodeEdited;
	}
	
	private int s;
    }


    boolean debug_load_class = false;
    boolean debug_library = false;
    
    String default_code_header = 
	"import java.util.*;\nimport java.awt.*;\nimport java.io.*;\n//import ExprData;\n" + 
	"public class CustomCodeWrapper extends CodeWrapper\n" +
	"{\n"+
	"  public CustomCodeWrapper(maxdView mview_)\n" +
	"  {\n" +
	"     super(mview_);\n" +
	"  }\n";

    String default_code_body = 
	"\npublic void doIt()\n" +
	"{\n" +
	"  \n" +
	"}\n";

    String default_code_footer = 
	"}\n";

    protected int code_status = 0;

    protected final static int CodeEdited   = 1;   // code has changed since last compile
    protected final static int CodeCompiled = 2;   // .class file is for current code

    protected int wrapper_lines_offset = -10;

    protected String user_class_dir = null;
    protected String compiler_path_str = null;
    protected String compiler_flags_str = null;
    protected String compiler_classpath_str = null;

    protected int compiler_time_limit = 15;

    protected boolean show_method_tree = true;

    //protected boolean show_library = false;

    protected boolean use_jit      = true;
    protected boolean clean_errors = true;

    protected String library_path_str = null;

    protected String load_path_str = null;
    protected String save_path_str = null;

    private   JFrame frame = null;
    private   JSplitPane h_split_pane;

    private   JPanel list_and_tree_wrapper;
    
    private   JTree method_tree = null;;
    private   JTree pc_tree = null;;
    private   JPopupMenu popup;

    protected JCheckBox use_jit_jchkb, clean_errors_jchkb;
    protected JButton run_jb = null;
    protected JButton update_entry_jb, add_to_lib_jb, del_from_lib_jb, rename_entry_jb;
    protected JButton raise_jb, lower_jb;
    protected JScrollPane code_jsp;
    protected JTextField library_path_jtf = null;
    protected JTextField compiler_path_jtf = null;
    protected JTextField compiler_flags_jtf = null;
    protected JTextField compiler_time_limit_jtf = null;
    protected JTextArea  compiler_classpath_jta = null;
    protected JTextArea code_jta = null;
    protected maxdView mview;

    protected boolean needs_save = false;    // has the code text been edited since last run?


    //UserClassLoader loader = null;

    UserClassRunnerThread ucrt = null;

    
    UndoManager undo_man;

}


