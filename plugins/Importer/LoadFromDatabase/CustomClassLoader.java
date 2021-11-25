//package uk.ac.man.bioinf.maxdLoad2;

import java.io.*;
import java.util.*;
import java.util.jar.*;

public class CustomClassLoader extends java.lang.ClassLoader
{
    public CustomClassLoader( Class parent_class )
    {
	parent_cl = parent_class.getClassLoader();
	debug_name = ""; // [D]";
	
	jar_class_cache = new Hashtable();
    }
    
    // ==========================================================

    private String getTopDirectory() { return "."; }

    // ==========================================================

    public void setDebugLevel( int debug_ )
    {
	debug = debug_;
    }

    // ==========================================================

    public String toString()
    {
	int cjf = custom_jarfiles == null ? 0 : custom_jarfiles.size();
	int ccp = custom_classpaths == null ? 0 : custom_classpaths.size();
	
	return ("there are " + cjf + " jar files and " + ccp + " paths in the search list");
    }
    
    // ==========================================================
    
    public void showClassSearchLocations(  )
    {
	System.out.println( "custom loader locations:");
	
	if(custom_classpaths != null)
	    for(int s=0; s < custom_classpaths.size(); s++)
		System.out.println( "path:"  + (String) custom_classpaths.elementAt( s ));
	else
	    System.out.println( "no paths");

	if(custom_jarfiles != null)
	    for(int s=0; s < custom_jarfiles.size(); s++)
		System.out.println( "jarfile:"  + (String) custom_jarfiles.elementAt( s ));
	else
	    System.out.println( "no jarfiles");
    }
    
    // ==========================================================

    public void addClassSearchPath( String path )
    {
	if(custom_classpaths == null)
	    custom_classpaths = new Vector();

	//make sure the path isn't already in the list
	for(int s=0; s < custom_classpaths.size(); s++)
	{
	    if(path.equals( (String) custom_classpaths.elementAt( s )))
		return;
	}
	
	custom_classpaths.insertElementAt( path, 0 );

	if(debug > 0)
	    showClassSearchLocations(  );
    }

    public void removeClassSearchPath( String path )
    {
	if(custom_classpaths == null)
	    return;
	int s=0;
	while(s < custom_classpaths.size())
	{
	    if(path.equals( (String) custom_classpaths.elementAt( s )))
		custom_classpaths.removeElementAt(s);
	    else
		s++;
	}
    }

    // ==========================================================
    
    public void addClassSearchJarFile( String jarfile )
    {
	addClassSearchJarFile( jarfile, true );
    }

    public void addClassSearchJarFile( String jarfile, boolean cache_it )
    {
	if(custom_jarfiles == null)
	    custom_jarfiles = new Vector();

	if(cache_it)
	    cache_jarfiles.add( jarfile );
	else
	    cache_jarfiles.remove( jarfile );

	//make sure the path isn't already in the list
	for(int s=0; s < custom_jarfiles.size(); s++)
	{
	    if(jarfile.equals( (String) custom_jarfiles.elementAt( s )))
		return;
	}

	custom_jarfiles.insertElementAt( jarfile, 0 );

	if(debug > 0)
	    showClassSearchLocations(  );
    }

    public void removeClassSearchJarFile( String jarfile )
    {
	if(custom_jarfiles == null)
	    return;
	int s=0;
	while(s < custom_jarfiles.size())
	{
	    if(jarfile.equals( (String) custom_jarfiles.elementAt( s )))
		custom_jarfiles.removeElementAt(s);
	    else
		s++;
	}
    }

    // ==========================================================

    public void setJarFileCacheMode( String jarfile, boolean cache_it )
    {
	
    }
    
    // ==========================================================

   // ==========================================================
    
    // interface to outside world
    private Class loadClass(String full_name, String class_name) 
    {
	// System.out.println("loadClass(): fn=" + full_name + " cn=" + class_name);

	try
	{
	    if(!full_name.toLowerCase().endsWith(".class"))
		System.out.println("***** " + full_name + " is NOT A CLASS FILE cn=" + class_name);
	    
	    return loadClass(class_name);
	}
	catch(java.lang.ClassNotFoundException cnfe)
	{
	    if(debug > 0)
		System.out.println("loadClass()" + debug_name + ": TOP LEVEL failed, " + cnfe);
	    return null;
	}
    }
    
    public Class findClass(String class_name) 
    {
	
	if(debug > 3)
	    System.out.println("findClass()" + debug_name + ": request for cn='" + class_name + "'");
	
	
	Class c = null;
	
	
	// try the parent loader
	if(c == null)
	{
	    if(debug > 6)
		System.out.println("findClass()" + debug_name + ": trying parent");
	    
	    String sys_name = class_name;
	    
	    try
	    {
		c = parent_cl.loadClass(class_name);
	    }
	    catch(java.lang.ClassNotFoundException cnfe)
	    {
	    }
	    
	    if(debug > 6)
		if(c != null)
		    System.out.println("findClass()" + debug_name + ": class '" + class_name + "' loaded by parent loader");
	}
	
	// is it a core .class ?
	if(c == null)
	{
	    if(debug > 6)
		    System.out.println("findClass()" + debug_name + ": trying top-dir");
	    
	    c = loadDataAndDefineClass( getTopDirectory() + class_name, class_name);
	    
	    if(debug > 6)
		if(c != null)
		    System.out.println("findClass()" + debug_name + ": class '" + class_name + 
				       "' loaded from top level directory");
	}
	
	// is it in any of the added paths?
	if(c == null)
	{
	    if(custom_classpaths != null)
	    {
		// if a full qualified class name is specified (i.e. 'org.something.Package.Module.ClassX')
		// then the '.'s must be converted to file separators in order to find the class
		//
		// make sure we dont replace the first '.' if it is being
		// used as a relative path (rather than as a fully qualified class name)
		// (e.g.  './plugins/Example/ExampleClass')
		    
		String first = class_name.substring(0, 1);
		
		String real_class_name = class_name.replace('.', File.separatorChar);
		
		real_class_name = first + real_class_name.substring(1);
		
		for(int s=0; s < custom_classpaths.size(); s++)
		{
		    String path = (String) custom_classpaths.elementAt( s );
		    
		    if(debug > 6)
			System.out.println("findClass()" + debug_name + ": trying bonus path: " + path);
		    
		    c = loadDataAndDefineClass( path+real_class_name, class_name);
		    
		    if(c != null)
		    {
			if(debug > 6)
			    System.out.println("findClass()" + debug_name + ": found in bonus path: " + path);
			break;
		    }
		    
		    if(debug > 6)
			if(c != null)
			    System.out.println("findClass()" + debug_name + ": class '" + 
					       class_name + "' loaded from bonus path: " + path);
		}
	    }
	}
	
	// is it in any of the added jarfiles?
	if(c == null)
	{
	    if(custom_jarfiles != null)
	    {
		for(int s=0; s < custom_jarfiles.size(); s++)
		{
		    String jarfile = (String) custom_jarfiles.elementAt( s );
		    
		    if(debug > 6)
			System.out.println("findClass()" + debug_name + ": trying jar: " + jarfile);
		    
		    c = loadDataAndDefineClass( jarfile, class_name);
		    
		    if(c != null)
		    {
			if(debug > 6)
			    System.out.println("findClass()" + debug_name + ": found in jar: " + jarfile);
			break;
		    }
		    
		    if(debug > 6)
			if(c != null)
			    System.out.println("findClass()" + debug_name + ": class '" + 
					       class_name + "' loaded from jar: " + jarfile);
		}
	    }
	}
	
	/*
	// is it a known plugin directory ?
	if(c == null)
	{
	    
	    String plugin_name = class_name;
	    
	    final int inner_class = class_name.indexOf('$');
	    
	    if(inner_class >= 0)
	    {
		plugin_name = plugin_name.substring( 0, inner_class );
	    }
	    
	    
	    String pdir = getPluginDirectory(plugin_name);
	    
	    if(pdir != null)
	    {
		if(debug > 6)
		    System.out.println("findClass()" + debug_name + ": trying plugin info for '" + plugin_name + "'");
		
		String file_name = getTopDirectory() + pdir + class_name;
		c = loadDataAndDefineClass( file_name, class_name);
	    }
	    else
	    {
		if(debug > 6)
		    System.out.println("findClass()" + debug_name + ": no plugin info for '" + plugin_name + "'");
	    }
	    
	    if(c != null)
	    {
		if(debug > 6)
		    if(c != null)
			System.out.println("findClass()" + debug_name + ": class '" + class_name + 
					   "' loaded as maxdView plugin class (LIB)");
		
	    }
	}
	*/

	/*
	// use the 'plugin_current_directory' which is set during the plugin scanning phase
	//
	if(c == null)
	{
	    if(plugin_current_directory != null)
	    {
		if(debug > 6)
		    System.out.println("findClass()" + debug_name + ": trying plugin_current_directory '" + plugin_current_directory + "'");
		
		String file_name = getTopDirectory() + plugin_current_directory + class_name;
		c = loadDataAndDefineClass(file_name, class_name);
		
		if(c != null)
		{
		    if(debug > 6)
			if(c != null)
			    System.out.println("findClass()" + debug_name + ": class '" + class_name + 
					       "' loaded as maxdView plugin class (PCD)");
		}
	    }
	}
	
	if(c == null)
	{
	    final String err_msg = "findClass()" + debug_name + ": ***  for '" + class_name + "' all load methods FAILED";
	    
	    if(debug > 0)
		System.out.println(err_msg);
	    if(system_options[DebugPlugins])
		alertMessage(err_msg);
	    
	}
	*/

	return c;
    }
    
    private Class loadDataAndDefineClass(String file_name, String class_name)
    {
	byte data[] = null;
	Class c = null;
	String real_file_name = file_name;
		    
	try
	{
	    if(real_file_name.toLowerCase().endsWith(".jar"))
	    {
		data = loadClassDataFromJAR(real_file_name, class_name);
	    }
	    else
	    {
		//if(!real_file_name.toLowerCase().endsWith(".class"))
		//    System.out.println("***** " + class_name + " is NOT A CLASS FILE fn=" + file_name);
		
		data = loadClassData(real_file_name);
	    }
	}
	
	catch (IOException ioe)
	{
	    if(debug > 2)
		System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				   class_name + " fn=" + real_file_name + 
				   " load FAILED: IOException");
	    return null;
	}
	catch (java.lang.NoClassDefFoundError ncde)
	{
	    if(debug > 2)
		System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				   class_name + " fn=" + real_file_name + 
				   " load FAILED: NoClassDefFoundError");
	    return null;
	}
	catch(Throwable th)
	{
	    if(debug > 2)
		System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				   class_name + " fn=" + real_file_name + 
				   " load FAILED due to " + th.toString());
	    return null;
	}
	
	try
	{
	    if(data != null)
		c = defineClass(class_name, data, 0, data.length);
	    
	    if(debug > 3)
	    {
		if(c == null)
		    System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				       class_name + " fn=" + real_file_name + 
				       " define FAILED: Unknown reason");
		else
		    System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				       class_name + " fn=" + real_file_name + 
				       " DEFINED");
	    }
	    
	    return c;
	}
	catch (java.lang.NoClassDefFoundError ncde)
	{
	    if(debug > 2)
		System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				   class_name + " fn=" + real_file_name + 
				   " define FAILED: NoClassDefFoundError");
	}
	catch(Throwable th)
	{
	    if(debug > 2)
		System.out.println("CustomClassLoader" + debug_name + " loadDataAndDefineClass(): cn="  + 
				   class_name + " fn=" + real_file_name + 
				   " define FAILED: " + th.toString());
	}
	
	
	return null;
    }
    
    
    private byte[] loadClassData(String name) throws IOException
    {
	String proper_name = name.toLowerCase().endsWith(".class") ? name : name + ".class";
	
	{
	    
	    File file = new File(proper_name);
	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	    
	    // what happens for really big classes??
	    //
	    int file_length = (int) file.length();
	    
	    byte b[] = new byte[file_length];
	    
	    if(bis.read(b, 0, file_length) == file_length)
	    {
		if(debug > 8)
		    System.out.println("  - loadClassData()" + debug_name + " '" + 
				       proper_name + "' loaded, " + file_length + " bytes");
	    }
	    
	    //System.out.println("  loadClassData() loaded '" + (name) + "'");
	    
	    return b;
	    
	}	    
    }
    
    // ====================================================================
    // ====================================================================
    //
    //  jar handling
    //
    //    either all entries are loaded into a cache the first time
    //      the jar file is encountered, (default)
    //    or
    //      load each entry on demand
    //
    // ====================================================================
    // ====================================================================
    
    private byte[] loadClassDataFromJAR(String jarname, String name) throws IOException
    {
	if(cache_jarfiles.contains(jarname))
	{
	    // has this jarfile already been loaded into the cache?
	    //
	    if((currently_cached_jarfile_name==null) || (!jarname.equals(currently_cached_jarfile_name)))
		// load it
		cacheAllClassesInJarFile(jarname);
	    
	    // do lookup
	    return findClassInJarCache(name);
	}
	else
	{
	    // this jarfile has been specifed as non-cachable
	    //
	    return loadFileFromJAR(jarname, name);
	}
    }
    
    private byte[] loadFileFromJAR(String jarname, String name) throws IOException
    {
	File jarfile = new File(jarname);
	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(jarfile));
	
	JarInputStream jis = new JarInputStream(bis);
	
	if(jis == null)
	{
	    if(debug > 0)
		System.out.println("loadClassDataFromJAR()" + debug_name + ": couldn't create input stream for  '" + jarname + "'");
	    return null;
	}
	
	boolean found = false;
	
	JarEntry je = null;
	
	//String file_name   = zname.replace('.', File.separatorChar);
	String proper_name = name.toLowerCase().endsWith(".class") ? name : (name + ".class");
	
	while(!found)
	{
	    je = jis.getNextJarEntry();
	    
	    // System.out.println(je.getName());
	    
	    if(je != null)
	    {
		String entry_name = je.getName();
		entry_name = entry_name.replace('/', '.');
		entry_name = entry_name.replace('\\', '.');
		if(entry_name.equals(proper_name))
		{
		    found = true;
		    // System.out.println("'" + fname + "' FOUND!");
		}
	    }
	    
	    if(je == null)
		break;
	}
	
	if(!found)
	{
	    if(debug > 0)
		System.out.println("loadClassDataFromJAR()" + debug_name + ": couldn't find entry for  '" + proper_name + "' (" + name + ")");
	    return null;
	}
	else
	{
	    // probably dont know how long the file entry is...
	    // (JAR files dont appear to store the uncompressed sizes...)
	    
	    
	    int file_length = (int) je.getSize();
	    if(file_length == -1)
		file_length = (int) jarfile.length();
	    
	    if(debug > 4)
		System.out.println("loadClassDataFromJAR()" + debug_name + ": loading " + 
				   file_length + " bytes for " + 
				   "'" + proper_name + "'");
	    
	    byte data_bytes[] = new byte[ file_length ];
	    
	    int len = 0;
	    int total_len = 0;
	    while(len != -1)
	    {
		len = jis.read(data_bytes, total_len, file_length-total_len);
		if(len > 0)
		    total_len += len;
		if((file_length-total_len) == 0)
		    len = -1;
	    }
	    
	    if(total_len == 0)
	    {
		if(debug > 0)
		    System.out.println("loadClassDataFromJAR()" + debug_name + ": couldn't read data for  '" + proper_name + "' (" + name + ")");
		return null;
	    }
	    
	    if(total_len < file_length)
	    {
		// the array is too big, trim to correct lenth
		
		byte[] trimmed = new byte[total_len];
		for(int b=0; b < total_len; b++)
		    trimmed[b] = data_bytes[b];
		data_bytes = trimmed;
	    }
	    
	    if(debug > 4)
		System.out.println("loadClassDataFromJAR()" + debug_name + ": loaded " + total_len + 
				   " bytes for '" + proper_name + "' (" + name + ")");
	    
	    return data_bytes;
	}
    }
    
    private byte[] findClassInJarCache(String classname)
    {
	String proper_name = classname.toLowerCase().endsWith(".class") ? classname : (classname + ".class");
	
	//System.out.println("findClassInJarCache() lookup:" + proper_name);
	byte[] cl = (byte[]) jar_class_cache.get(proper_name);
	
	return cl;
    }

    private void cacheAllClassesInJarFile(String jarname) throws IOException
    {
	File jarfile = new File(jarname);
	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(jarfile));
	
	JarInputStream jis = new JarInputStream(bis);
	
	if(jis == null)
	{
	    if(debug > 0)
		System.out.println("cacheAllClassesInJarFile()" + debug_name + ": couldn't create input stream for  '" + jarname + "'");
	    return;
	}
	
	boolean ended = false;
	
	JarEntry je = null;
	
	jar_class_cache.clear();
	
	while(!ended)
	{
	    je = jis.getNextJarEntry();
	    
	    // System.out.println(je.getName());
	    
	    if(je != null)
	    {
		String entry_name = je.getName();
		entry_name = entry_name.replace('/', '.');
		entry_name = entry_name.replace('\\', '.');
		
		int file_length = (int) je.getSize();
		if(file_length == -1)
		    file_length = (int) jarfile.length();
		
		if(debug > 6)
		    System.out.println("cacheAllClassesInJarFile()" + debug_name + ": loading " + 
				       file_length + " bytes for " + 
				       "'" + entry_name + "'");
		
		byte data_bytes[] = new byte[ file_length ];
		
		int len = 0;
		int total_len = 0;
		while(len != -1)
		{
		    len = jis.read(data_bytes, total_len, file_length-total_len);
		    if(len > 0)
			total_len += len;
		    if((file_length-total_len) == 0)
			len = -1;
		}
		
		if(total_len == 0)
		{
		    if(debug > 0)
			System.out.println("cacheAllClassesInJarFile()" + debug_name + 
					   ": couldn't read data for  '" + entry_name + "'");
		    
		}
		
		if(total_len < file_length)
		{
		    // the array is too big, trim to correct lenth
		    
		    byte[] trimmed = new byte[total_len];
		    for(int b=0; b < total_len; b++)
			trimmed[b] = data_bytes[b];
		    data_bytes = trimmed;
		}
		
		if(total_len > 0)
		{
		    jar_class_cache.put( entry_name, data_bytes );
		    
		    if(debug > 1)
			System.out.println("cacheAllClassesInJarFile()" + debug_name + ": loaded " + total_len + 
					   " bytes for '" + entry_name + "'");
		}
	    }
	    else
	    {
		ended = true;
	    }
	}
	
	//System.out.println("cacheAllClassesInJarFile()" + jar_class_cache.size() + " entries cached");
	
	currently_cached_jarfile_name = jarname;
	
    }

    // ==========================================================
    // handy debug function:


    public String findBestMatchInJARFile( String class_name, String jar_name ) throws IOException
    {
	File jar_file = new File(jar_name);
	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(jar_file));
	JarInputStream jis = new JarInputStream(bis);
	
	// get a list of all classes in the JAR file

	if(jis == null)
	{
	    System.out.println("findBestMatchInJARFile(): couldn't create input stream for '" + jar_name + "'");
	    return null;
	}
	
	boolean ended = false;
	
	JarEntry je = null;
	
	java.util.Vector jar_contents = new java.util.Vector();

	while(!ended)
	{
	    je = jis.getNextJarEntry();
	    
	    // System.out.println(je.getName());
	    
	    if(je != null)
	    {
		String entry_name = je.getName();
		entry_name = entry_name.replace('/', '.');
		entry_name = entry_name.replace('\\', '.');
		
		jar_contents.addElement( entry_name );
	    }
	    else
	    {
		ended = true;
	    }
	}

	if( jar_contents.size() == 0 )
	{
	    System.out.println("findBestMatchInJARFile(): JAR file '" + jar_name + "' appears to be empty");
	    return null;
	}

	// do some fuzzy matching

	String best_match = (String) jar_contents.elementAt(0);
	int    best_error = fuzzyMatch( class_name, best_match );

	//System.out.println( class_name + " vs " + best_match + " = " + best_error);

	for(int n=1; n < jar_contents.size(); n++)
	{
	    String match = (String) jar_contents.elementAt(n);
	    int error  =  fuzzyMatch( class_name, match );

	    //System.out.println( class_name + " vs " + match + " = " + error);

	    if(error < best_error)
	    {
		best_error = error;
		best_match = match;
	    }
	}
	
	return best_match;
    }

    public int fuzzyMatch( String s1_in, String s2_in )
    {
	int[] s1_char_count = new int[26];
	
	String s1 = s1_in.toLowerCase();

	for(int c=0; c < s1.length(); c++)
	{
	    int cc = charCode( s1.charAt(c) );
	    if(cc >= 0)
		s1_char_count[cc]++;
	}

	int[] s2_char_count = new int[26];
	
	String s2 = s2_in.toLowerCase();

	for(int c=0; c < s2.length(); c++)
	{
	    int cc = charCode( s2.charAt(c) );
	    if(cc >= 0)
		s2_char_count[cc]++;
	}

	int error = 0;

	for(int l=0; l < 26; l++)
	{
	    int diff = (s1_char_count[l] - s2_char_count[l]);
	    if(diff < 0)
		diff = -diff;
	    error += diff;
	}
	
	return error;
    }

    // avoids all the nastiness of unicode, radix etc.
    private int charCode( char ch )
    {
	final char[] chars = { 'a','b','c','d','e','f','g','h','i','j','k','l','m',
			       'n','o','p','q','r','s','t','u','v','w','x','y','z' };
	
	for(int c=0; c < 26; c++)
	    if( ch == chars[c])
		return c;
	return -1;
    }

    // ==========================================================
    
    public int debug = 0;  // 100 ==full
    
    private ClassLoader parent_cl;
    
    private String current_path = null;
    
    private String debug_name = "";
    
    private Hashtable jar_class_cache;

    private String currently_cached_jarfile_name = null;

    private Vector custom_classpaths = null;
    private Vector custom_jarfiles = null;
    
    private HashSet cache_jarfiles = new HashSet();

     
}
