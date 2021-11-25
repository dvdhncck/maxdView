import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.sql.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.*;

public class AnnotationLoader extends DatabaseConnection implements ExprData.ExprDataObserver 
{
    final int n_opt_lines = 3;

    public AnnotationLoader(maxdView mview_)
    {
	super(mview_);

	edata = mview.getExprData();

	loadSources();
	cacheInit();
	of = null;
	edata.addObserver(this);
    }

    public void finalize()
    {
	if(connection != null)
	    disconnect();

	edata.removeObserver(this);
    }


    private void initSources()
    {
	source_a = new AnnoSource[3];
	source_a[0] = new AnnoSource(AnnoSource.SourceScript, false,  "local script", "/home/bio/maxd/website/cgi-bin/anno-get.pl", null);
	source_a[1] = new AnnoSource(AnnoSource.SourceFile, true, "local files",  "/home/bio/data/annotation/", null);
	String[] args = { "name=${Gene.Name}" };
	source_a[2] = new AnnoSource(AnnoSource.SourceURL, false, "local web", "http://localhost/maxd/cgi-bin/anno-web-get.pl", args);
    }
    private void loadSources()
    {
	try
	{
	    FileInputStream fis = new FileInputStream(new File(mview.getConfigDirectory() + "anno-source.dat"));
	    ObjectInputStream ois = new ObjectInputStream(fis);

	    try
	    {
		source_a = (AnnoSource[]) ois.readObject();
	    }
	    catch(InvalidClassException fnfe)
	    {
		// no problem, it means that the AnnoSource class has changed
		initSources();
		use_gene_names = false;
		use_probe_name = false;
		ois.close();
		return;
	    }


	    Boolean ug = (Boolean) ois.readObject();
	    use_gene_names = ug.booleanValue();

	    Boolean up = (Boolean) ois.readObject();
	    use_probe_name = up.booleanValue();

	    ois.close();
	    return;
	}
	catch(FileNotFoundException ioe)
	{
	    // no problem, it just means the annotation has not been run before
	    initSources();
	    return;
	}
	catch(ClassNotFoundException fnfe)
	{
	    mview.errorMessage("Cannot understand annotation options file\n  " + fnfe);

	}
	catch(IOException ioe) // other than FileNotFound
	{
	    mview.errorMessage("Cannot load annotation options file\n  " + ioe);
	}
	source_a = new AnnoSource[0];

    }
    private void saveSource()
    {
	try
	{
	    FileOutputStream fos = new FileOutputStream(new File(mview.getConfigDirectory() + "anno-source.dat"));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(source_a);
	    
	    oos.writeObject( new Boolean(use_gene_names) );
	    oos.writeObject( new Boolean(use_probe_name) );

	    oos.flush();
	    oos.close();
	}
	catch(IOException ioe)
	{
	    mview.errorMessage("Cannot save annotation options file\n  " + ioe);
	}

    }
    
    protected void disableSource(AnnoSource as)
    {
	as.active = false;
	if(of != null)
	    updateSourceControls();
    }

    final boolean debug_load = false;
    
    //
    // make the text suitable for inclusion in as part of an HTML body
    //
    //   if the text contains <HTML>...</HTML> remove them
    //   if the text contains <BODY>...</BODY> reomve them
    //
    //   wrap it in some decoration indicating where it come from
    //
    //   if the text appears not to be in HTML form, stick <PRE> ... </PRE>
    //   tags around it
    //

    /*
    private String convertToHTMLBody(String result, String name)
    {

	// return stripHTMLTags(result);

	//
	// check what format the file appears to be 
	// and convert it to HTML
	//
	//String hdr = "<P>Annotation from '" + name + "'</P>";
	String hdr = 
	"<CENTER><P><TABLE WIDTH=\"100%\" BGCOLOR=\"#555555\"><TR><TD><FONT SIZE=\"-1\" COLOR=\"#dddddd\"><NOBR><B>..." + 
	name + "...</B></NOBR></FONT></TD></TR></TABLE></P></CENTER>";

	if(result == null)
	{
	    return hdr + "<P>(no result)</P>";
	}
	
	System.out.println("convertToHTMLBody(): input " + result.length() + " chars");

	System.out.println("convertToHTMLBody(): input=\n\n-----------------------------\n" + result + "\n-----------------------------\n");


	if((result.indexOf("<HTML") < 0))
	{
	    // doesn't have <HTML>...</HTML> tags anywhere
	    //
	    if(result.indexOf("<P>") < 0)
	    {
		// this appears to be plain text...
		//

		//System.out.println("not HTML, wrapping text (" + result.length() + " chars)");
		
		if(result.length() > 10240)
		{
		    result = result.substring(0, 10240) + "\n\nWARNING: annotation truncated to 10K chars<";
		}
		result = "<PRE>" + result + "</PRE>";
	    }
		
	    StringBuffer sbuf = new StringBuffer(result.length() + 45);
	    sbuf.append(hdr);
	    sbuf.append(result);
	    return sbuf.toString();
	}
	else
	{
	    System.out.println("convertToHTMLBody(): '<HTML' at " + result.indexOf("<HTML"));

	    // strip the <HMTL>...</HTML> and <BODY>...</BODY>
	    //
	    // do this by removing everything up to (and including) the closing '>' of '<BODY' 
	    // and everything after (and including) the first '<' of '</BODY'
	    //
	    
	    int body_tag_pos = result.indexOf("<BODY");

	    System.out.println("convertToHTMLBody(): '<BODY' at " + body_tag_pos);

	    if(body_tag_pos < 0)
	    {
		// wierd! no body tag....
		return "<P>convertToHTMLBody(): unable to parse what looks like HTML (no &lt;BODY;&gt tag?)</P>";
	    }
	    // find the next '>' after body_start
	    int body_end = result.indexOf('>', body_tag_pos+4);
	    
	    System.out.println("convertToHTMLBody(): '<BODY' ends at " + body_end);

	    int end_body_tag_pos = result.indexOf("</BODY");
	    
	    System.out.println("convertToHTMLBody(): '</BODY' at " + end_body_tag_pos);

	    if(end_body_tag_pos < 0)
	    {
		// wierd! no end body tag....
		return "<P>convertToHTMLBody(): unable to parse what looks like HTML (no &lt;/BODY;&gt tag?)</P>";
	    }
	    
	    //System.out.println("input is " + result.length() + " chars");

	    StringBuffer sbuf = new StringBuffer(result.length());
	    sbuf.append(hdr);
	    sbuf.append(result.substring(body_end+1, end_body_tag_pos));

	    System.out.println("convertToHTMLBody(): output=\n\n-----------------------------\n" + sbuf.toString() + "\n-----------------------------\n");

	    return sbuf.toString();

	    //System.out.println("stripped from " + body_end+1 + " to  " + end_body_tag_pos);
	}
	
    }

    private String stripHTMLTags(String input)
    {
	if(input == null)
	    return null;

	StringBuffer sbuf = new StringBuffer();
	
	final int n_chars = input.length();
	boolean in_tag = false;
	for(int c=0; c < n_chars; c++)
	{
	    char ch = input.charAt(c);
	    if(ch == '<')
	    {
		in_tag = true;
	    }
	    if(!in_tag)
		sbuf.append(ch);
	    if(ch == '>')
	    {
		in_tag = false;
		if(c > 3)
		    if(input.substring(c-3, c+1).toLowerCase().equals("</p>"))
			sbuf.append("\n");
	    }
	}
	return sbuf.toString();
    }
    */

    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // the dispatcher for load operations
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //

    public final String loadAnnotationFromCache(int spot_id)
    {
	String spot_name =  edata.getSpotName(spot_id);
	String probe_name = edata.getProbeName(spot_id);
	String[] gn = edata.getGeneNames(spot_id);
	String gene_name = (gn != null) ? gn[0] : null;
	
	StringBuffer result = new StringBuffer();

	for(int source_id=0; source_id< source_a.length; source_id++)
	{
	    if(source_a[source_id].active)
	    {
		try
		{
		    SourceResult sres = null;
		    switch(source_a[source_id].mode)
		    {
		    case AnnoSource.SourceFile:
			sres = loadAnnotationFromFile(null, spot_id, false, source_a[source_id], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceScript:
			sres = loadAnnotationFromScript(null, spot_id, false, source_a[source_id], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceURL:
			sres = loadAnnotationFromURL(null, spot_id, false, source_a[source_id], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceJDBC:
			sres = loadAnnotationFromJDBC(null, spot_id, false, source_a[source_id], spot_name, probe_name, gene_name);
			break;
		    }
		    
		    if(sres != null)
		    {
			result.append(sres.data);
		    }
		}
		catch(AnnoArgVarNoValueException aavnve)
		{
		    disableSource(source_a[source_id]);
		    return null;
		}
		catch(AnnoArgVarNotUsedException aavnue)
		{
		    disableSource(source_a[source_id]);
		    return null;
		}
		catch(AnnoArgVarException aave)
		{
		    disableSource(source_a[source_id]);
		    return null;
		}
	    }
	}

	return result.toString();
    }

    //
    // load anno for a given spot_id, using Gene name(s), Probe name or both
    //
    /*

    public final String getNamesForSpot(int spot_id)
    {
	// make sure annotation for each name is only loaded only
	Hashtable loaded_names = new Hashtable();

	String name = null;
	if(!use_gene_names && !use_probe_name)
	    name = "(no source names selected)";
	else
	{
	    if(use_gene_names)
	    {
		String[] gn = edata.getGeneNames(spot_id);
		if(gn != null)
		{
		    for(int g=0; g < gn.length; g++)
		    {
			if(loaded_names.get(gn[g]) == null)
			{
			    loaded_names.put(gn[g], "x");
			    
			    if(name == null)
				name = gn[g];
			    else
				name += ", " + gn[g];
			}
		    }
		}
	    }
	    
	    if(use_probe_name)
	    {
		String pname = edata.getProbeName(spot_id);
		if(loaded_names.get(pname) == null)
		{
		    if(name == null)
			name = pname;
		    else
			name += "," + pname;
		}
	    }
	}
	
	return name;
    }
    */

    
    
    //
    //   the idea is to load any annotation for the spot, probe and gene(s) in the spot
    //   exactly once 
    //
    //   this is complicated by the fact the AnnoSources can refer to more than one
    //   type of name, eg 
    //          
    //           "id1=${Probe.Name} id2c=${Spot.COLUMN} id2r=${Spot.ROW}"
    //
    //   this source should only be evaluated once
    //
    //   if the source refers to gene names(s), eg
    // 
    //           "var1=${Gene.EMBL_ID} var2=${Probe.Name}"  
    //
    //   then it must be evaluated multiple times (once for each gene name)
    //

    private Vector ann_request_v = null;
    private Vector active_thread_v = null;

    public final int getNumPartsExpected(int spot_id, boolean reload)
    {
	// final AnnoCacheEntry result = reload ? null : cacheLoad(edata.getSpotName(spot_id));
	//if(result == null)
	{
	    int n_parts = 1;
	    for(int s=0; s< source_a.length; s++)
		if(source_a[s].active)
		    n_parts++;

	    // System.out.println("!! getNumPartsExpected() will load " + n_parts + " parts");

	    return n_parts;
	}
	/*
	else
	{
	    System.out.println("!! getNumPartsExpected() will uncache " + result.parts.length + " parts");

	    return result.parts.length;
	}
	*/
    }

    private class AnnLoadRequest
    {
	public int source_id;
	public int part;
	public AnnotationViewer av;
	public int spot_id;
	public boolean reload;
	// public AnnoCacheEntry cache_entry;
    }

    private synchronized void addThread(AnnLoaderThread alt)
    {
	active_thread_v.addElement(alt);
    }

    private synchronized void removeThread(AnnLoaderThread alt)
    {
	active_thread_v.removeElement(alt);
    }

    private synchronized boolean loaderThreadNeeded()
    {
	return ((ann_request_v.size() > 0) && (active_thread_v.size() < autoload_max_active_threads));
    }

    private synchronized void makeLoaderThread()
    {
	new AnnLoaderThread().start();
    }
    
    private synchronized void addRequest(AnnLoadRequest alr)
    {
	ann_request_v.addElement(alr);
    }

    private synchronized void cancelAllRequests()
    {
	ann_request_v.removeAllElements();
    }

    private synchronized AnnLoadRequest getNextPendingRequest()
    {
	if(ann_request_v.size() > 0)
	{
	    AnnLoadRequest alr = (AnnLoadRequest) ann_request_v.elementAt(0);
	    // System.out.println("ALT: handling request for source id=" + alr.source_id + ", " + ann_request_v.size() + " remain");
	    ann_request_v.removeElementAt(0);
	    return alr;
	}
	else
	{
	    return null;
	}
    }

    private class AnnLoaderThread extends Thread
    {
	public AnnLoaderThread()
	{
	    addThread(this);
	}

	public final void run()
	{
	    // System.out.println("ALT: thread spawned, now there are " + active_thread_v.size());

	    boolean thread_can_live = true;
	    int sleep_count = 0;
	    while(thread_can_live)
	    {
		AnnLoadRequest alr = getNextPendingRequest();
		if(alr != null)
		{
		    loadOnePart(alr);
		    
		    if( loaderThreadNeeded() )
		    {
			    // spawn another thread...
			makeLoaderThread();
		    }
		}
		else
		{
		    try
		    {
			Thread.sleep(250);
			// System.out.println("ALT: no requests, sleeping...");
			if(++sleep_count > 20)
			{
			    thread_can_live = false;

			}

		    }
		    catch(InterruptedException ie)
		    {
		    }
		}
	    }
	    removeThread(this);
	    // System.out.println("ALT: slept for a long time, dying...");
	}
    }

    public final void requestLoadAnnotationInParts(AnnotationViewer av, int spot_id, boolean reload)
    {
	/*
	if(!reload)
	{
	    final AnnoCacheEntry result = cacheLoad(spot_name);
	    if(result != null)
	    {
		System.out.println("!! found " + result.parts.length + " parts in the cache");

		// found in the cache...return the parts
		for(int p=0; p < result.parts.length; p++)
		{
		    if(av != null)
			av.displayAnnotationPart(p, result.names[p] + "  (cached)",  result.parts[p]);
		}

		// don't load anything!
		return;
	    }
	}
	else
	{
	    cacheRemove(spot_name);
	}
	*/

		
	if(active_thread_v == null)
	{
	    active_thread_v = new Vector();
	}

	if(ann_request_v == null)
	{
	    ann_request_v = new Vector();
	}
	
	int parts = getNumPartsExpected(spot_id, reload);

	AnnLoadRequest alr = new AnnLoadRequest();
	alr.source_id = -1; 
	alr.av = av; 
	alr.spot_id = spot_id; 
	alr.reload = reload;
	alr.part = 0;

	addRequest(alr);

	int part = 1;
	for(int s=0; s< source_a.length; s++)
	{
	    if(source_a[s].active)
	    {
		alr = new AnnLoadRequest();
		alr.source_id = s; 
		alr.av = av; 
		alr.spot_id = spot_id; 
		alr.reload = reload;
		alr.part = part++;

		addRequest(alr);
	    }
	}
	
	if(loaderThreadNeeded())
	{
	    makeLoaderThread();
	}
    }
    
    private class SourceResult
    {
	public String data;
	public boolean cached;

	public SourceResult(String d, boolean c) { data = d; cached = c; }
    }

    public final void loadOnePart(AnnLoadRequest alr)
    {
	//
	// NOTE: doesn't allow mixed names yet.....
	//

	if(alr.source_id < 0) // -1 means 'built-in' names and attrs source
	{
	    // names and attrs in part 0
	    String data = getTagAttrData( alr.spot_id );
	    
	    if(alr.av != null)
		alr.av.displayAnnotationPart(0, "Names and Attributes", data);
	}
	else
	{
	    String spot_name =  edata.getSpotName(alr.spot_id);
	    String probe_name = edata.getProbeName(alr.spot_id);
	    String[] gn = edata.getGeneNames(alr.spot_id);
	    String gene_name = (gn != null) ? gn[0] : null;
	    
	    try
	    {
		if(source_a[alr.source_id].active)
		{
		    SourceResult sres = null;
		    switch(source_a[alr.source_id].mode)
		    {
		    case AnnoSource.SourceFile:
			sres = loadAnnotationFromFile(alr.av, alr.spot_id, alr.reload, source_a[alr.source_id], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceScript:
			sres = loadAnnotationFromScript(alr.av, alr.spot_id, alr.reload, source_a[alr.source_id], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceURL:
			sres = loadAnnotationFromURL(alr.av, alr.spot_id, alr.reload, source_a[alr.source_id], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceJDBC:
			sres = loadAnnotationFromJDBC(alr.av, alr.spot_id, alr.reload, source_a[alr.source_id], spot_name, probe_name, gene_name);
			break;
		    }

		    if(sres != null)
		    {
			if(alr.av != null)
			{
			    if(sres.cached)
				alr.av.displayAnnotationPart(alr.part, source_a[alr.source_id].name+ "  (cached)", sres.data);
			    else
				alr.av.displayAnnotationPart(alr.part, source_a[alr.source_id].name, sres.data);
			}
		    }
		    else
		    {
			if(alr.av != null)
			    alr.av.displayAnnotationPart(alr.part, source_a[alr.source_id].name+ "  (disabled)", "(no result)");
		    }


		    
		}
	    }
	    catch(AnnoArgVarNoValueException aavnve)
	    {
		if(alr.av != null)
		    alr.av.displayAnnotationPart(alr.part, source_a[alr.source_id].name, "(no values for one or more arguments)");
	    }
	    catch(AnnoArgVarNotUsedException aavnue)
	    {
		//System.out.println(source_a[s].name + " not used for this thing");
		if(alr.av != null)
		    alr.av.displayAnnotationPart(alr.part, source_a[alr.source_id].name, "(no arguments found)");
	    }
	    catch(AnnoArgVarException aave)
	    {
		mview.errorMessage("Error in annotation source '" + source_a[alr.source_id].name + "'\n  " + aave + "\nSource disabled");
		
		disableSource(source_a[alr.source_id]);

		if(alr.av != null)
		    alr.av.displayAnnotationPart(alr.part, source_a[alr.source_id].name+ "  (disabled)", "(no result)");
	    }
	}
    }

    //public String[] loadAnnotationInParts(AnnotationViewer av, int spot_id, boolean reload)
    //{
	/*
	  //
	// NOTE: doesn't allow mixed names yet.....
	//
	String ann = null;

	int n_parts = getNumPartsExpected();
	
	String[] result = new String[n_parts];

	StringBuffer result_sb = new StringBuffer();

	// names and tags in part 0
	result_sb.append("<HTML><BODY>\n");
	result_sb.append( getTagAttrData( spot_id ));
	result_sb.append("\n</BODY></HTML>");		
	result[0] = result_sb.toString();

	String spot_name =  edata.getSpotName(spot_id);
	String probe_name = edata.getProbeName(spot_id);
	String[] gn = edata.getGeneNames(spot_id);
	String gene_name = (gn != null) ? gn[0] : null;
	int part = 1;

	for(int s=0; s< source_a.length; s++)
	{
	    try
	    {
		if(source_a[s].active)
		{
		    switch(source_a[s].mode)
		    {
		    case AnnoSource.SourceFile:
			result[part] = loadAnnotationFromFile(av, spot_id, source_a[s], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceScript:
			result[part] = loadAnnotationFromScript(av, spot_id, source_a[s], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceURL:
			result[part] = loadAnnotationFromURL(av, spot_id, source_a[s], spot_name, probe_name, gene_name);
			break;
		    case AnnoSource.SourceJDBC:
			result[part] = loadAnnotationFromJDBC(av, spot_id, source_a[s], spot_name, probe_name, gene_name);
			break;
		    }
		    part++;
		}
	    }
	    catch(AnnoArgVarNoValueException aavnve)
	    {
		System.out.println(source_a[s].name + " arg var has no value for spot:" + spot_id);
		result[part] = "(no values for one or more arguments)";
	    }
	    catch(AnnoArgVarNotUsedException aavnue)
	    {
		//System.out.println(source_a[s].name + " not used for this thing");
	    }
	    catch(AnnoArgVarException aave)
	    {
		mview.errorMessage("Error in annotation source '" + source_a[s].name + "'\n  " + aave + "\nSource disabled");
		
		disableSource(source_a[s]);
	    }
	}
	
	return result;
	*/
	/*
	// spot

	result_sb = new StringBuffer();
	result_sb.append(loadAnnotation(av, spot_id, sname, null, null, reload));
	result[] = result_sb.toString();

	// probe
	result_sb.append(loadAnnotation(av, spot_id, null,  pname, null, reload));

	// gene(s)
	String[] gn = edata.getGeneNames(spot_id);
	Hashtable loaded_names = new Hashtable();  // make sure annotation for each gene name is only loaded only
	if(gn != null)
	{
	    for(int g=0; g < gn.length; g++)
	    {
		if(loaded_names.get(gn[g]) == null)
		{
		    result_sb.append(loadAnnotation(av, spot_id, null, null, gn[g], reload));
		    loaded_names.put(gn[g], "x");
		}
	    }
	}

	result_sb.append("\n</BODY></HTML>");		
	
	return result_sb.toString();
	*/
    //}

    /*
    //
    // load anno for the specified probe name
    //
    public String loadProbeAnnotation(String name)
    {
 	return loadAnnotation(null, true, name, false);
    }
    public String loadProbeAnnotation(String name, boolean reload)
    {
 	return loadAnnotation(null, true, name, reload);
    }

    public String loadProbeAnnotation(AnnotationViewer av, String name)
    {
	return loadAnnotation(av, true, name, false);
    }

    //
    // load anno for the specified gene name
    //
    public String loadGeneAnnotation(String name)
    {
 	return loadAnnotation(null, false, name, false);
    }
    public String loadGeneAnnotation(String name, boolean reload)
    {
 	return loadAnnotation(null, false, name, reload);
    }

    public String loadGeneAnnotation(AnnotationViewer av, String name)
    {
	return loadAnnotation(av, false, name, false);
    }
    */

    /*
    public String loadAnnotation(AnnotationViewer av, int spot_id,
				 String spot_name, String probe_name, String gene_name,
				 boolean reload)
    {
	boolean found = false;
	int o = 0;

	String result = null;
	String cache_name = null;

	StringBuffer result_sb = new StringBuffer();

	if(spot_name != null)
	{
	    //System.out.println("loadAnnotation(): spot " + spot_name);
	    cache_name = spot_name;
	}
	if(probe_name != null)
	{
	    //System.out.println("loadAnnotation(): probe " + probe_name);
	    cache_name = probe_name;
	}
	if(gene_name != null)
	{
	    //System.out.println("loadAnnotation(): gene " + gene_name);
	    cache_name = gene_name;
	}
	
	if(cache_name != null)
	{
	    if(reload)
		cacheRemove(cache_name);
	    
	    result = cacheLoad(cache_name);
	    if(result != null)
	    {
		return result;
	    }
	}


	// wasn't in the cache.....

	if(source_a.length == 0)
	{
	    mview.alertMessage("There are no annotation sources defined.\n Use the \"Options\" button (in the Annotation Viewer) to set them");
	}

	for(int s=0; s< source_a.length; s++)
	{
	    try
	    {
		if(source_a[s].active)
		{
		    // System.out.println("---checking source '" + source_a[s].name + "' ---------");
		    
		    switch(source_a[s].mode)
		    {
		    case AnnoSource.SourceFile:
			result_sb.append(convertToHTMLBody(loadAnnotationFromFile(av, spot_id, source_a[s], spot_name, probe_name, gene_name), source_a[s].name));
			break;
		    case AnnoSource.SourceScript:
			result_sb.append(convertToHTMLBody(loadAnnotationFromScript(av, spot_id, source_a[s], spot_name, probe_name, gene_name), source_a[s].name));
			break;
		    case AnnoSource.SourceURL:
			result_sb.append(convertToHTMLBody(loadAnnotationFromURL(av, spot_id, source_a[s], spot_name, probe_name, gene_name), source_a[s].name));
			break;
		    case AnnoSource.SourceJDBC:
			result_sb.append(convertToHTMLBody(loadAnnotationFromJDBC(av, spot_id, source_a[s], spot_name, probe_name, gene_name), source_a[s].name));
			break;
		    }
		   
		    
		}
	    }
	    catch(AnnoArgVarNoValueException aavnve)
	    {
		System.out.println(source_a[s].name + " arg var has no value for spot:" + spot_id);
		result_sb.append("(no values for one or more arguments)");
	    }
	    catch(AnnoArgVarNotUsedException aavnue)
	    {
		//System.out.println(source_a[s].name + " not used for this thing");
	    }
	    
	    catch(AnnoArgVarException aave)
	    {
		mview.errorMessage("Error in annotation source '" + source_a[s].name + "'\n  " + aave + "\nSource disabled");
		
		disableSource(source_a[s]);
	    }

	}

	result = result_sb.toString();

	if(result != null)
	{
	    if(debug_load)
	    {
		System.out.println(result.length() + " chars loaded");
		
		//System.out.println("------------\n" + result.toString() + "\n-------------");
	    }
	    
	    cacheStore(cache_name, result);
	    return result;
	}


	return "Not found!";
    }
    */

    private String getTagAttrData( int spot_id )
    {
	StringBuffer res = new StringBuffer();
	res.append("<HTML><BODY><TABLE WIDTH=\"95%\" CELLPADDING=\"1\" CELLSPACING=\"1\">");

	String sname = mview.getExprData().getSpotName( spot_id );
	if(sname != null)
	{
	    res.append("<TR VALIGN=TOP><TD ALIGN=RIGHT><B>Spot</B>&nbsp;</TD><TD>" + sname + "</TD></TR>");

	    ExprData.TagAttrs sta = mview.getExprData().getSpotTagAttrs();
	    
	    addTagAttrs( sname, sta, res);
	}

	String pname = mview.getExprData().getProbeName( spot_id );
	if(pname != null)
	{
	    res.append("<TR VALIGN=TOP><TD ALIGN=RIGHT><B>Probe</B>&nbsp;</TD><TD>" + pname + "</TD></TR>");

	    ExprData.TagAttrs pta = mview.getExprData().getProbeTagAttrs();
	    
	    addTagAttrs( pname, pta, res);
	}
	
	String[] gnames = mview.getExprData().getGeneNames( spot_id );
	if(gnames != null)
	{
	   ExprData.TagAttrs gta = mview.getExprData().getGeneTagAttrs();
	   
	   for(int g=0; g < gnames.length; g++)
	   {
	       res.append("<TR VALIGN=TOP><TD ALIGN=RIGHT><B>Gene</B>&nbsp;</TD><TD>" + gnames[g] + "</TD></TR>");

	       if(gnames[g] != null)
	       {
		   addTagAttrs( gnames[g], gta, res);
	       }
	   }
	}
	res.append("</TABLE></BODY></HTML>");

	return res.toString();
    }

    private void addTagAttrs( String tname,  ExprData.TagAttrs ta, StringBuffer sb)
    {
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    sb.append("<TR VALIGN=TOP><TD ALIGN=RIGHT><FONT SIZE=\"-1\"");
	    String v = ta.getTagAttr(tname, a);
	    if(v == null)
		sb.append("COLOR=\"#444499\"><B>" + ta.getAttrName(a) + "</B></FONT>&nbsp;</TD><TD><FONT SIZE=\"-1\" COLOR=\"#993333\">(no value)</FONT></TD></TR>");
	    else
		sb.append("COLOR=\"#2222FF\"><B>" + ta.getAttrName(a) + "</B></FONT>&nbsp;</TD><TD>" + ta.getTagAttr(tname, a) + "</TD></TR>");
	}
    }
    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // loader: file
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    public final SourceResult loadAnnotationFromFile(AnnotationViewer av, int spot_id, boolean reload, AnnoSource as, 
						     String spot_name, String probe_name, String gene_name) throws AnnoArgVarException
    {
	String[] file_args = replaceVariables(as.args, spot_id, spot_name, probe_name, gene_name);
	
	String args_str = "";
	if(file_args != null)
	    for(int a=0; a < file_args.length; a++)
	    {
		args_str += file_args[a];
	    }

	String file_name = new String(as.code + File.separatorChar + args_str);

	String safe_name = file_name.replace(File.separatorChar, '_');
		
	// check the cache....
	if(!reload)
	{
	    AnnoCacheEntry ce = cacheLoad(as.code, safe_name);
	    if(ce != null)
		return new SourceResult(ce.data, true);
	}

	if(anno_debug)
	    System.out.println("looking for file " + safe_name);

	try 
	{ 

	    File file = new File(safe_name);
	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	    
	    // what happens for really big files??
	    //
	    int file_length = (int) file.length();
	    
	    byte b[] = new byte[file_length];
	    
	    if(bis.read(b, 0, file_length) == file_length)
	    {
		if(debug_load)
		    System.out.println("     - loadAnnotationFromFile() load  is good (" + file_length + " bytes)");

		String str = new String(b);

		// write to cache....
		cacheStore(new AnnoCacheEntry(as.code, safe_name, str));

		return new SourceResult(str, false);
	    }
	    else
	    {
		mview.errorMessage("Annotation file '" + safe_name + "' could not be loaded.\nSource disabled");

		disableSource(as);
		
		return null;
	    }
	    
	}
	catch (IOException e) 
	{
            //mview.errorMessage("Annotation file '" + file_name + "' could not be loaded\n(Error: " + e + ")");
	    return null;
	}
    }
    
    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // loader: script
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    public final SourceResult loadAnnotationFromScript(AnnotationViewer av, int spot_id, boolean reload, AnnoSource as, 
					   String spot_name, String probe_name, String gene_name) throws AnnoArgVarException
    {
	// remove any '/'s from the gene name....
	//String safe_name = name.replace(File.separatorChar, '_');

	String[] args = replaceVariables(as.args, spot_id, spot_name, probe_name, gene_name);
	
	String args_str = "";
	if(args != null)
	{
	    for(int a=0; a < args.length; a++)
	    {
		if(a > 0)
		    args_str += " ";
		args_str += args[a];
	    }
	}

	// check the cache....
	if(!reload)
	{
	    AnnoCacheEntry ce = cacheLoad(as.code, args_str);
	    if(ce != null)
		return new SourceResult(ce.data, true);
	}

	String exec_cmd = as.code + " " + args_str;

	StringWriter sw = new StringWriter();
	    
	try
	{
	    if(anno_debug)
		System.out.println("running '" + exec_cmd + "'");
	    
	    Runtime rt = Runtime.getRuntime();
	    Process process = rt.exec(exec_cmd);
	    
	    // connect to the pipe
	    //
	    //System.out.println("writing '" + name +  "' to STDIN");
	    
	    /*
	    OutputStream os  = process.getOutputStream();
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
	    
	    bw.write(name);
	    bw.write('\n');
	    bw.flush();
	    os.close();
	    */

	    InputStream is = process.getInputStream();
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    
	    //System.out.println("waiting for completion");
	    
	    try
	    {
		process.waitFor();
	    }
	    catch (InterruptedException ie)
	    {
	    }
	
	    if(process.exitValue() != 0)
	    {
		if(anno_debug)
		    System.out.println("script exited abnormally");
		
		// collate the error messages into a String
		//
		sw = new StringWriter();
		
		// record the error stream
		InputStream errors = process.getErrorStream();
		
		if(anno_debug)
		    System.out.println("collating error stream");
		
		try
		{
		    int ch = errors.read();
		    while(ch >= 0)
		    {
			sw.write(ch);
			ch = errors.read();
		    }
		}
		catch(IOException ioe)
		{
		    
		}
		
		// bring up an alert box with the error message in it...
		disableSource(as);
		mview.errorMessage("Source: " + as.name + "\nError Output:\n\n" + sw.toString() + "\nSource disabled");
		return null;
	    }
	    
	    //process = null;
	    
	    //System.out.println("collating output stream");
	    
	    try
	    {
		int ch = br.read();
		while(ch >= 0)
		{
		    if(ch >= 0)
			sw.write(ch);
		    ch = br.read();
		}
	    }
	    catch(IOException ioe)
	    {
		
	    }
	    
	    //	mview.informationMessage("Output:\n\n" + sw.toString() + "\n\n");
	    
	    
	}
	catch (IOException e)
	{
	    disableSource(as);
	    mview.errorMessage("Source: " + as.name + "\nCouldn't run program\n  " + e + "\nSource disabled");
	    return null;
	}
	catch (Exception e)
	{
	    disableSource(as);
	    mview.errorMessage("Source: " + as.name + "\nCouldn't run program\n  " + e + "\nSource disabled");
	    return null;
	}
	catch (Error e)
	{
	    disableSource(as);
	    mview.errorMessage("Source: " + as.name + "\nCouldn't run program\n  " + e + "\nSource disabled");
	    return null;
	}

	// write to cache....
	String data = sw.toString();;

	cacheStore(new AnnoCacheEntry(as.code, args_str, data));

	return new SourceResult(data, false);
    }
 
    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // loader: URL
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    public final SourceResult loadAnnotationFromURL(AnnotationViewer av, int spot_id, boolean reload, AnnoSource as, 
					String spot_name, String probe_name, String gene_name) throws AnnoArgVarException
    {
	String[] url_args = replaceVariables(as.args, spot_id, spot_name, probe_name, gene_name);
	
	String args_str = "";
	if(url_args != null)
	    for(int a=0; a < url_args.length; a++)
	    {
		if(a > 0)
		    args_str += " ";
		args_str += url_args[a];
	    }
	
	if(anno_debug)
	    System.out.println("trying: url='" + as.code + "', args='" + args_str + "'");
	
	// check the cache....
	if(!reload)
	{
	    AnnoCacheEntry ce = cacheLoad(as.code, args_str);
	    if(ce != null)
		return new SourceResult(ce.data, true);
	}

	StringBuffer result = new StringBuffer();

	try
	{
	    if(debug_load)
		System.out.println(as.code);
	    
	    URL db = new URL(as.code);
	    URLConnection uc = db.openConnection();
	    
	    uc.setDoOutput(true);
	    
	    if(args_str.length() > 0)
	    {
		PrintWriter out = new PrintWriter(uc.getOutputStream());
		out.print(args_str);
		out.close();
	    }
	    
	    BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
	    String input_line;
	    
	    while ((input_line = in.readLine()) != null) 
	    {
		result.append(input_line);
		result.append("\n");
	    }
	    in.close();
	    
	    if(debug_load)
		System.out.println("\n[eof, " + result.toString().length() + " chars received]");
	}

	catch (MalformedURLException murle)
	{
	   disableSource(as);
	   
	   mview.errorMessage("Source: " + as.name + ": malformed URL (" + murle + ")\nSource disabled.");
	   as.active = false;
	   if(of != null)
	       updateSourceControls();

	   return null;
	}

	catch (IOException ioe)
	{
	   disableSource(as);
	   mview.errorMessage("Source: " + as.name + ": IO exception (" + ioe + ")\nSource disabled.");

	   return null;
	}

	// write to cache....
	String data = result.toString();;

	cacheStore(new AnnoCacheEntry(as.code, args_str, data));

	return new SourceResult(data, false);
    }

    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // loader: JDBC
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    public final SourceResult loadAnnotationFromJDBC(AnnotationViewer av, int spot_id, boolean reload, AnnoSource as, 
					 String spot_name, String probe_name, String gene_name) throws AnnoArgVarException
    {
	String jdbc_root = as.code;

	if(!isConnected())
	{
	    attemptConnection(); // establishConnection(jdbc_root, null, null);
	}

	// retrieve the data....

	if(!isConnected())
	{
	    if(av != null)
	    {
		disableSource(as);
		mview.errorMessage("Source: " + as.name + ": not connected\nSource disabled");
		return null;
	    }
	}
	else
	{
	    // System.out.println("connected: name=" + name);
	    
	    // generate the quey

	    String sql = "SELECT " + qField("Name") + "," + qField("Short_Description") + " FROM " + 
	    qTable("Probe") + " WHERE " + qField("Name") + " = " + qText(edata.getProbeName(spot_id));

	    // System.out.println(sql);

	    ResultSet rs = executeQuery(sql);
	 
	    String result = "";

	    try
	    {
		while (rs.next()) 
		{
		    result += "Name: " + rs.getString(1) + "\n";
		    result += "Short Description: " + rs.getString(2) + "\n";
		}

		return new SourceResult(result, false);
	    }	   

	    catch(SQLException sqle)
	    {
		result += "No matches";
	    }
	}
	/*
	try
	{ 
	    Thread.sleep(1000);
	}
	catch (InterruptedException e)
	{
	}
	*/

	if(av != null)
	{
	    disableSource(as);
	    mview.errorMessage("Source: " + as.name + ": JDBC not found\nSource disabled");
	}

	return null;
    }

    
    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // data cache 
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    public final void cacheStore(AnnoCacheEntry ce)
    {
	// get the table for the specified source location
	Hashtable loc_ht = (Hashtable) cache_hash.get(ce.source_loc);
	if(loc_ht == null)
	{
	    loc_ht = new Hashtable();
	    cache_hash.put(ce.source_loc, loc_ht);
	}
	
	// update cache stats...
	//   is there an existing entry?
	AnnoCacheEntry old_ce = (AnnoCacheEntry) loc_ht.get(ce.source_args);
	if(old_ce != null)
	{
	    // old data is no longer in cache
	    cache_size -= old_ce.data.length();
	}
	else
	{
	    // no existing entry
	    cache_entries++;
	}
	// incr cache size for new data
	cache_size += ce.data.length();

	// and store data
	loc_ht.put(ce.source_args, ce);

	// System.out.println("---> cache SAVE: <<" + ce.source_loc + ":" + ce.source_args + ">>");
    }
    
    public final AnnoCacheEntry cacheLoad(String s_loc, String s_args)
    {
	// get the table for the specified source location
	Hashtable loc_ht = (Hashtable) cache_hash.get(s_loc);
	if(loc_ht == null)
	{
	    cache_misses++;
	    return null;
	}
	AnnoCacheEntry res = (AnnoCacheEntry) loc_ht.get(s_args);
	if(res == null)
	{
	    // System.out.println("---> cache miss: <<" + s_loc + ":" + s_args + ">>");
	    cache_misses++;
	    return null;
	}
	else
	{
	    // System.out.println("---> cache LOAD:  <<" + s_loc + ":" + s_args + ">>");
	    cache_hits++;
	    return res;
	}
    }

    /*
    public void cacheRemove(String name)
    {
	cache_hash.remove(name);
	return;
    }	
    */

    public final void cacheEmpty()
    {
	cache_hash = new Hashtable();
	cache_entries = 0;
	cache_size = 0;
    }

    public final void cacheInit()
    {
	cache_hash = new Hashtable();
	cache_size = 0;
	cache_hits   = 0;
	cache_misses = 0;
	cache_entries = 0;
    }
    
    final boolean debug_cache = false;

    int cache_hits   = 0;
    int cache_misses = 0;
    long cache_size = 0;
    long cache_entries = 0;

    Hashtable cache_hash = null;

    public final void cacheSaveAsFile()
    {
	JFileChooser jfc = mview.getFileChooser();
	int val = jfc.showSaveDialog(null);
	
	if(val == JFileChooser.APPROVE_OPTION) 
	{
	    File file = jfc.getSelectedFile();
	    
	    if(file.exists())
	    {
		if(mview.infoQuestion("File exists, overwrite?", "Yes", "No") == 1)
		    return;
	    }

	    try
	    {
		FileOutputStream fos = new FileOutputStream(file);
		//BufferedOutputStream bos = new BufferedOutputStream(fos);
		//GZIPOutputStream gos = new GZIPOutputStream(bos);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
	   
		oos.writeObject( new Long(cache_entries) );
		oos.writeObject( new Long(cache_size) );
		oos.writeObject( cache_hash );

		oos.flush();
		oos.close();

		mview.successMessage("Cache data (" + cache_entries + " entries) written to " + file.getName() + "'");
	    }
	    catch(java.io.IOException ioe)
	    {
		mview.alertMessage("File '" + file.getName() + "' cannot be opened");
		ioe.printStackTrace();
		return;
	    }
	}		
    }

    public final void cacheLoadFromFile()
    {
	JFileChooser jfc = mview.getFileChooser();
	int val = jfc.showOpenDialog(null);
	
	if(val == JFileChooser.APPROVE_OPTION) 
	{
	    File file = jfc.getSelectedFile();
	    
	    try
	    {
		FileInputStream fis = new FileInputStream(file);
		//BufferedInputStream bis = new BufferedInputStream(fis);
		//GZIPInputStream gis = new GZIPInputStream(bis);
		ObjectInputStream ois = new ObjectInputStream(fis);
	    
		try
		{
		    Long cv = (Long) ois.readObject();
		    cache_entries = cv.longValue();
		    cv = (Long) ois.readObject();
		    cache_size = cv.longValue();
		    
		    cache_hash = (Hashtable) ois.readObject();
		    
		    mview.successMessage("Cache data (" + cache_entries + " entries) read from '" + file.getName() + "'");
		    updateCacheStatsLabels();
		}
		catch(InvalidClassException fnfe)
		{
		    mview.alertMessage("File '" + file.getName() + "' doesn't appear to be a cache file");
		}
		catch(ClassNotFoundException cnfe)
		{
		    mview.alertMessage("File '" + file.getName() + "' doesn't appear to be a cache file");
		}
	    }
	    catch(java.io.IOException ioe)
	    {
		mview.alertMessage("File '" + file.getName() + "' cannot be opened");
		ioe.printStackTrace();
	    }
	}
    }

    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // status check for cache entries
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    
    /*
    public boolean isCached(String name)
    {
	return (cacheLoad(name) != null);
    }
    */


    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // parsing variables from command strings
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //

    protected void setupVarsAndVals()
    {
    }

    //
    // replace vars from NameTagAttrs....
    //
    protected String[] replaceVariables(String[] args, int spot_id, String spot_name, String probe_name, String gene_name) throws AnnoArgVarException
    {
	//System.out.println("replaceVariables(): start is " + params);
	if(args == null)
	    return null;

	String[] res = new String[args.length];
	
	for(int a=0; a < args.length; a++)
	{
	    String pp = args[a];
	    String tmp = args[a];
	    while(tmp != null)
	    {
		tmp = replaceFirstVariable(tmp, spot_id, spot_name, probe_name, gene_name);
		if(tmp != null)
		    pp = tmp;
		//System.out.println("tmp is " + tmp);
	    }
	    //System.out.println("replaceVariables(): end is " + pp);
	    res[a] = pp;
	}
	return res;
    }
    
    
    private class AnnoArgVarException extends Exception
    {
	private String err;
	
	public AnnoArgVarException()
	{
	}
	public AnnoArgVarException(String err_)
	{
	    err = (err_.length() > 40) ? err_.substring(0, 39) : err_;
	    
	}
	
	public String toString() { return err == null ? "" : err; }
    }

    private class AnnoArgVarNotUsedException extends AnnoArgVarException
    {
    }

    private class AnnoArgVarNoValueException extends AnnoArgVarException
    {
	public AnnoArgVarNoValueException(String err_) { super(err_); }
    }

    // varibale format:  ${var_name}
    //
    protected String replaceFirstVariable(String str, int spot_id, String spot_name, String probe_name, String gene_name) throws AnnoArgVarException
    {
	// System.out.println("replaceFirstVariable(): s=" + spot_name + " p=" + probe_name + " g=" + gene_name);
	
	String res = "";
	
	int p1 = str.indexOf("${");
	
	if(p1 >= 0)
	{
	    //System.out.println("        ${ found at char pos " + p);
	    
	    String var_rem = str.substring(p1+2);
	    
	    int p2 = var_rem.indexOf('}');
	    
	    boolean unmatched = true;
	    boolean used = false;
	    
	    if(p2 == 0)
	    {
		throw new AnnoArgVarException("missing variable name at '" + str.substring(p1) + "'");
	    }

	    if(p2 >= 0)
	    {
		boolean recognised = false;

		String var = var_rem.substring(0, p2);
		
		int skip_len = 0;
		String val = null;
		
		// found a variable identifier, attempt translate....
		
		if(var.startsWith("Probe."))
		{
		    recognised = true;

		    if(probe_name != null)
		    {
			used = true;
			
			if(unmatched && (var.equals("Probe.Name")))
			{
			    val = probe_name;
			    unmatched = false;
			}
			
			// test all spot name attrs
			if(unmatched)
			{
			    ExprData.TagAttrs pta = edata.getProbeTagAttrs();
			    int attr_id  = pta.getAttrID(var.substring(6));
			    if(attr_id >= 0)
			    {
				val = pta.getTagAttr( probe_name, attr_id );
				unmatched = false;
			    }
			}
		    }
		}
		
		if(var.startsWith("Gene."))
		{
		    recognised = true;

		    if(gene_name != null)
		    {
			used = true;
			
			if(unmatched && (var.equals("Gene.Name")))
			{
			    val = edata.getGeneName(spot_id);
			 unmatched = false;
			}
			// test all gene name attrs for the specified Gene name
			if(unmatched)
			{
			    ExprData.TagAttrs gta = edata.getGeneTagAttrs();
			    int attr_id  = gta.getAttrID(var.substring(5));
			    if(attr_id >= 0)
			    {
				val = gta.getTagAttr( gene_name, attr_id );
				unmatched = false;
			    }
			}
		    }
		}

		if(var.startsWith("Spot."))
		{
		    recognised = true;
		    
		    if(spot_name != null)
		    {
			used = true;
			
			if(unmatched && (var.equals("Spot.Name")))
			{
			    val = edata.getSpotName(spot_id);
			    unmatched = false;
			}
			
			// test all spot name attrs
			if(unmatched)
			{
			    ExprData.TagAttrs sta = edata.getSpotTagAttrs();
			    int attr_id  = sta.getAttrID(var.substring(5));
			    if(attr_id >= 0)
			    {
				val = sta.getTagAttr( spot_name, attr_id );
				unmatched = false;
			    }
			}
		    }
		}
		
		if(!recognised)
		{
		    throw new AnnoArgVarException("unrecognised variable '" + var + "'");
		}

		if(used)
		{
		    // include any text before the variable
		    if(p1 > 0)
			res = str.substring(0,p1);
		    
		    if(unmatched)
		    {
			throw new AnnoArgVarException("unrecognised variable '" + var + "'");
		    }
		    else
		    {
			if((val == null) || (val.length() == 0))
			{
			    throw new AnnoArgVarNoValueException("variable '" + var + "' has no value");
			}
			else
			{
			    res += val;
			    
			    // append any text after the variable
			    res += str.substring(p1+2+p2+1);
			    
			    return res;
			}
		    }
		}
		else
		{
		    // System.out.println("replaceFirstVariable(): not used");
		    throw new AnnoArgVarNotUsedException();
		}

	    }
	    else
	    {
		throw new AnnoArgVarException("missing '}' after '" + str.substring(p1) + "'");
	    }
	    
	}
	return null;
    }

    /*
    protected String replaceFirstVariable(String str, int spot_id)
    {
	System.out.println("replaceFirstVariable(): " + str);

	String res = "";

	int p = str.indexOf('%');

	if(p >= 0)
	{
	    //System.out.println("        % found at char pos " + p);

	    String var = str.substring(p+1);
	    int skip_len = 0;
	    String val = null;

	    //if(!str.substring(p,p+2).equals("%%"))
	    {
		// found a variable identifier, translate....

		if((val == null) && (var.startsWith("Probe.Name")))
		{
		    val = edata.getProbeName(spot_id);
		    skip_len = 10;
		}
		
		if((val == null) && (var.startsWith("Probe.")))
		{
		    // check each of the probe name attrs
		    String attr_name = var.substring(6);
		    int attr_id = edata.getProbeTagAttrs().getAttrID(attr_name);
		    if(attr_id >= 0)
		    {
			val = edata.getProbeTagAttrs().getTagAttr( edata.getProbeName(spot_id), attr_id );
			if((val != null) && (val.length() > 0)
			{
			    skip_len = 6 + attr_name.length();
			}
		    }
		}

		
		if((val == null) && (var.startsWith("Gene.Name")))
		{
		    val = edata.getGeneName(spot_id);
		    skip_len = 9;
		}
		
		if((val == null) && (var.startsWith("Spot.Name")))
		{
		    val = edata.getSpotName(spot_id);
		    skip_len = 9;
		}
		
		// include any text before the variable
		if(p > 0)
		    res = str.substring(0,p);

		if(val != null)
		{
		    res += val;

		    // append any text after the variable
		    res += str.substring(p + 1 + skip_len);

		    return res;
		}

		
	    }
	}
	return null;
    }
    */

    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // the options dialog frame
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //

    public final void createOptionsDialog()
    {
	if(of == null)
	    of = new OptionsFrame();
	updateCacheStatsLabels();
	of.setVisible(true);
	of.toFront();
	//mview.locateWindowAtCenter(of);
    }

    private void updateCacheStatsLabels()
    {
	// update any things that want updating
	//

	cache_entries_label.setText(new Long(cache_entries).toString());
	
	long hr_pc = (cache_hits * 100);
	if((cache_hits + cache_misses) > 0)
	    hr_pc /= (cache_hits + cache_misses);
	cache_hr_label.setText(new Long(hr_pc).toString() + " %");
	
	long cs_scaled = cache_size;
	String cs_unit = " chars";
	if(cs_scaled > 4096)
	{
	    cs_scaled /=  1024;
	    cs_unit = (cs_scaled == 1) ? " Kchar" : " Kchars";
	    
	    if(cs_scaled > 1024)
	    {
		cs_scaled /= 1024;
		cs_unit = (cs_scaled == 1) ? " Mchar" : " Mchars";
	    }
	}
	cache_size_label.setText(new Long(cs_scaled).toString() + cs_unit);

	/*
	if(cache_mode > 0)
	{
	    
	    if(cache_mode == 1)
	    {
		cache_entries_label.setText(new Integer(n_cache_lines).toString() + " lines");
		
		int used_pc = (cache_lines_used * 100) / n_cache_lines;
		cache_used_label.setText(new Integer(used_pc).toString() + " %");
		
		cache_used_lines_label.setText(cache_lines_used + " lines");
		
		//int name_clash_pc = (cache_lines_used > 0) ? (cache_name_clashes * 100) / cache_lines_used : 0;
		cache_clashes_label.setText(new Integer(cache_name_clashes).toString());

	    }
	    else
	    {
		cache_entries_label.setText(cache_hash.size()  + " lines");
		cache_used_label.setText("");
		cache_used_lines_label.setText("");
		cache_clashes_label.setText("");
	    }

	    
	}
	else
	{
	    cache_hr_label.setText("");
	    cache_entries_label.setText("");
	    cache_used_label.setText("");
	    cache_size_label.setText("");
	    cache_used_lines_label.setText("");
	    cache_clashes_label.setText("");
	}
	*/
    }

    private boolean ignore_gui_updates = false;

    // set the data for all sources based on the current GUI values
    //
    public final void updateSources()
    {
	if(!ignore_gui_updates)
	{
	    final int n_srcs = source_a.length;
	    for(int s=0; s < n_srcs; s++)
	    {
		source_a[s].active    = active_jchkb[s].isSelected();
		source_a[s].name      = name_jtf[s].getText();
		source_a[s].mode      = mode_jcb[s].getSelectedIndex();
		if(source_a[s].args == null)
		    source_a[s].args = new String[1];
		source_a[s].args[0]   = args_jtf[s].getText();
		source_a[s].code      = code_jtf[s].getText();
		//source_a[s].for_probe = probe_jchkb[s].isSelected();
		//source_a[s].for_gene  = gene_jchkb[s].isSelected();

	    }
	    //System.out.println("sources updated....");
	}
    }

    // set the GUI values based on the current data
    //
    public final void updateSourceControls()
    {
	ignore_gui_updates = true;

	final int n_srcs = source_a.length;
	for(int s=0; s < n_srcs; s++)
	{
	    active_jchkb[s].setSelected(source_a[s].active);
	    mode_jcb[s].setSelectedIndex(source_a[s].mode);
	    args_jtf[s].setText(source_a[s].args == null ? "" : source_a[s].args[0]);
	    name_jtf[s].setText(source_a[s].name);
	    code_jtf[s].setText(source_a[s].code);
	}
	//System.out.println("controls updated....");

	ignore_gui_updates = false;
    }

    private void addControls()
    {
	final int n_srcs = source_a.length;
	
	active_jchkb = new JCheckBox[n_srcs];
	code_jtf     = new JTextField[n_srcs];
	args_jtf     = new JTextField[n_srcs];
	name_jtf     = new JTextField[n_srcs];
	mode_jcb     = new JComboBox[n_srcs];
	
	delete_jb = new JButton[n_srcs];
	raise_jb  = new JButton[n_srcs];
	lower_jb  = new JButton[n_srcs];
	
	//probe_jchkb = new JCheckBox[n_srcs];
	//gene_jchkb = new JCheckBox[n_srcs];

	String[] mode_names = { "File", "Script", "URL" };
		
	int line = 0;

	source_panel.removeAll();

	GridBagLayout source_gridbag = new GridBagLayout();
	source_panel.setLayout(	source_gridbag);

	for(int s=0; s < n_srcs; s++)
	{
	    // filler between sources
	    {
		JLabel dummy = new JLabel(" ");
		dummy.setBackground(Color.red);
		dummy.setMinimumSize(new Dimension(10,30));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		source_gridbag.setConstraints(dummy, c);
		source_panel.add(dummy);
		
	    }
	    line++;
	    
	    int col = 0;
	    {
		JLabel label = new JLabel("Name ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		source_gridbag.setConstraints(label, c);
		source_panel.add(label);
	    }
	    {
		name_jtf[s] = new JTextField(20);
		name_jtf[s].setToolTipText("The name used for this annotation source");

		name_jtf[s].getDocument().addDocumentListener(new CustomChangeListener());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		source_gridbag.setConstraints(name_jtf[s], c);
		source_panel.add(name_jtf[s]);
	    }

	    // ---------

	    col = 2;
	    {
		delete_jb[s] = new JButton(delete_ii);
		delete_jb[s].setToolTipText("Delete this source");
		delete_jb[s].addActionListener(new CustomActionListener(s, 0)); 
		GridBagConstraints c = new GridBagConstraints();
		delete_jb[s].setPreferredSize(new Dimension(20,20));
		c.gridx = col++;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		source_gridbag.setConstraints(delete_jb[s], c);
		source_panel.add(delete_jb[s]);
	    }
	    {
		if(s > 0)
		{
		    raise_jb[s] = new JButton(raise_ii);
		    raise_jb[s].setToolTipText("Move this source up the search order");
		    raise_jb[s].addActionListener(new CustomActionListener(s, 1)); 
		    GridBagConstraints c = new GridBagConstraints();
		    raise_jb[s].setPreferredSize(new Dimension(20,20));
		    c.gridx = col++;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.WEST;
		    //if((s + 1) == n_srcs)
		    //c.gridwidth = 2;
		    source_gridbag.setConstraints(raise_jb[s], c);
		    source_panel.add(raise_jb[s]);
		}
	    }
	    {
		if((s + 1) < n_srcs)
		{
		    lower_jb[s] = new JButton(lower_ii);
		    lower_jb[s].setToolTipText("Move this source down the search order");
		    lower_jb[s].addActionListener(new CustomActionListener(s, 2)); 
		    GridBagConstraints c = new GridBagConstraints();
		    lower_jb[s].setPreferredSize(new Dimension(20,20));
		    c.gridx = col++;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.WEST;
		    //if(s == 0)
		    //c.gridwidth = 2;
		    source_gridbag.setConstraints(lower_jb[s], c);
		    source_panel.add(lower_jb[s]);
		}
	    }

	    // ---------

	    {
		JLabel label = new JLabel("Active ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line+1;
		c.anchor = GridBagConstraints.EAST;
		source_gridbag.setConstraints(label, c);
		source_panel.add(label);
	    }
	    {
		active_jchkb[s] = new JCheckBox();
		active_jchkb[s].setToolTipText("Enable or disable this source");
		active_jchkb[s].addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateSources();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line+1;
		c.anchor = GridBagConstraints.WEST;
		source_gridbag.setConstraints(active_jchkb[s], c);
		source_panel.add(active_jchkb[s]);
	    }
	    {
		JLabel label = new JLabel("Type ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line+2;
		c.anchor = GridBagConstraints.EAST;
		source_gridbag.setConstraints(label, c);
		source_panel.add(label);
	    }
	    {
		mode_jcb[s] = new JComboBox(mode_names);
		mode_jcb[s].setToolTipText("Choose the type of this source");
		mode_jcb[s].addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateSources();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line+2;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1.0;
		//c.weighty = 1.0;
		
		source_gridbag.setConstraints(mode_jcb[s], c);
		source_panel.add(mode_jcb[s]);
	    }
	    {
		JLabel label = new JLabel("Location ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line+3;
		c.anchor = GridBagConstraints.EAST;
		source_gridbag.setConstraints(label, c);
		source_panel.add(label);
	    }
	    {
		code_jtf[s] = new JTextField(20);
		code_jtf[s].setToolTipText("The location of the script, URL or directory use for this source");
		code_jtf[s].getDocument().addDocumentListener(new CustomChangeListener());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line+3;
		c.gridwidth = 4;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		c.weightx = 4.0;
		//c.weighty = 1.0;
		
		source_gridbag.setConstraints(code_jtf[s], c);
		source_panel.add(code_jtf[s]);
	    }
	    {
		JLabel label = new JLabel("Params ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line+4;
		c.anchor = GridBagConstraints.EAST;
		source_gridbag.setConstraints(label, c);
		source_panel.add(label);
	    }
	    {
		args_jtf[s] = new JTextField(20);
		args_jtf[s].setToolTipText("The arguments to be passed to the script or URL");
		args_jtf[s].getDocument().addDocumentListener(new CustomChangeListener());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line+4;
		c.gridwidth = 4;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		c.weightx = 4.0;
		//c.weighty = 1.0;
		
		source_gridbag.setConstraints(args_jtf[s], c);
		source_panel.add(args_jtf[s]);
	    }
	    
	    if((s+1) == n_srcs)
	    {
		JLabel dummy = new JLabel();
		dummy.setMinimumSize(new Dimension(10,10));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		//c.gridwidth = 4;
		//c.weightx = .0;
		//c.weighty = 1.0;
		//c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		source_gridbag.setConstraints(dummy, c);
		source_panel.add(dummy);
		
	    }


	    line += 6;
	}

	updateSourceControls();
		
	source_panel.updateUI();
	source_scroller.updateUI();
    }

    class CustomChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    updateSources();
	}
    }
    class CustomActionListener implements ActionListener 
    {
	int source, action;

	public CustomActionListener(int s, int a)
	{
	    source = s; action = a;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    switch(action)
	    {
	    case 0:
		deleteSource(source);
		break;
	    case 1:
		raiseSource(source);
		break;
	    case 2:
		lowerSource(source);
		break;
	    }
	}
    }

    public class OptionsFrame extends JFrame
    {
	public OptionsFrame()
	{
	    super("Annotation Options");

	    mview.decorateFrame( this );

	    int line = 0;

	    JPanel panel = new JPanel();
	    getContentPane().add(panel);
	    GridBagLayout gridbag = new GridBagLayout();
	    panel.setLayout(gridbag);
	    
	    JTabbedPane tabbed_pane = new JTabbedPane();
	    tabbed_pane.setPreferredSize(new Dimension(450, 350));

	    // --------------------------------------------------------------------
	    // ====================================================================
	    //
	    // the name selection panel
	    //
	    // ====================================================================
	    // --------------------------------------------------------------------

	    /*
	    {
		JPanel names_panel = new JPanel();
		GridBagLayout names_gridbag = new GridBagLayout();
		names_panel.setLayout(names_gridbag);

		int line = 0;

		JLabel label = new JLabel("Which names to use for annotation loading");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		//c.anchor = GridBagConstraints.EAST;
		//c.weightx = 1.0;
		//c.weighty = 2.0;
		names_gridbag.setConstraints(label, c);
		names_panel.add(label);
		line++;

		JCheckBox names_jchkb = new JCheckBox("Gene name(s)");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		//c.anchor = GridBagConstraints.EAST;
		//c.weightx = 1.0;
		//c.weighty = 2.0;
		names_gridbag.setConstraints(names_jchkb, c);
		names_panel.add(names_jchkb);
		line++;

		JCheckBox probe_names_jchkb = new JCheckBox("Probe name(s)");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		//c.anchor = GridBagConstraints.EAST;
		//c.weightx = 1.0;
		//c.weighty = 2.0;
		names_gridbag.setConstraints(probe_names_jchkb, c);
		names_panel.add(probe_names_jchkb);
		line++;

		tabbed_pane.addTab(" Names ", names_panel);
	    }

	    */

	    // --------------------------------------------------------------------
	    // ====================================================================
	    //
	    // the data source panel
	    //
	    // ====================================================================
	    // --------------------------------------------------------------------

	    {
		JPanel source_wrapper = new JPanel();
		source_wrapper.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		GridBagLayout source_gridbag = new GridBagLayout();
		source_wrapper.setLayout(source_gridbag);

		raise_ii = new ImageIcon(mview.getImageDirectory() + "raise.gif");
		lower_ii = new ImageIcon(mview.getImageDirectory() + "lower.gif");
		delete_ii = new ImageIcon(mview.getImageDirectory() + "delete.gif");
	   
		
		source_panel = new JPanel();
		source_scroller = new JScrollPane(source_panel);
		{
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 1;
		    c.anchor = GridBagConstraints.CENTER;
		    c.fill = GridBagConstraints.BOTH;
		    c.weightx = 1.0;
		    c.weighty = 1.0;

		    source_gridbag.setConstraints(source_scroller, c);
		    source_wrapper.add(source_scroller);
		}

		addControls();

		{
		    JPanel button_panel = new JPanel();
		    button_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		    GridBagLayout button_gridbag = new GridBagLayout();
		    button_panel.setLayout(button_gridbag);
		    
		    {
			final JButton jb = new JButton(" Add new source ");
			button_panel.add(jb);
			jb.setToolTipText("Add new source source to the end of current collection");

			jb.addActionListener(new ActionListener() 
			    {
				public void actionPerformed(ActionEvent e) 
				{
				    addSource();
				}
			    });
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = GridBagConstraints.CENTER;
			//c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			//c.weighty = 1.0;
			button_gridbag.setConstraints(jb, c);
		    }
		    {
			final JCheckBox jb = new JCheckBox(" Debug ");
			button_panel.add(jb);
			jb.setToolTipText("Shows extra debuging output");

			jb.addActionListener(new ActionListener() 
			    {
				public void actionPerformed(ActionEvent e) 
				{
				    anno_debug = jb.isSelected();
				}
			    });
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 1;
			c.gridy = 0;
			c.anchor = GridBagConstraints.EAST;
			//c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			//c.weighty = 1.0;
			button_gridbag.setConstraints(jb, c);
		    }
		    {
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 2;
			//c.anchor = GridBagConstraints.CENTER;
			//c.weightx = 1.0;
			source_gridbag.setConstraints(button_panel, c);
			source_wrapper.add(button_panel);
			c.fill = GridBagConstraints.HORIZONTAL;
		    }
		}

		tabbed_pane.addTab(" Sources ", source_wrapper);
	    }

	    // --------------------------------------------------------------------
	    // ====================================================================
	    //
	    // the cache information panel
	    //
	    // ====================================================================
	    // --------------------------------------------------------------------

	    {
		JPanel cache_panel = new JPanel();
		GridBagLayout cache_gridbag = new GridBagLayout();
		cache_panel.setLayout(cache_gridbag);

		/*
		cache_data_timer = new Timer(1000, new ActionListener() 
		    {
			public void actionPerformed(ActionEvent evt) 
			{
			    updateCacheStatsLabels();
			}
		    });
		cache_data_timer.start();
		*/

		{
		    JPanel cache_func_panel = new JPanel();
		    cache_func_panel.setBorder(BorderFactory.createTitledBorder(" Commands "));
		    GridBagLayout cache_func_gridbag = new GridBagLayout();
		    cache_func_panel.setLayout(cache_func_gridbag);

		    // the all important empty button...
		    //
			
		    JButton jcb = new JButton("Empty cache");
		    jcb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				cacheEmpty();
				updateCacheStatsLabels();
			    }
			});
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 0;
		    c.weighty = 1;
		    cache_func_gridbag.setConstraints(jcb, c);
		    cache_func_panel.add(jcb);

		    jcb = new JButton("Save cache");
		    jcb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				cacheSaveAsFile();
			    }
			});
		    c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 1;
		    c.weighty = 1;
		    cache_func_gridbag.setConstraints(jcb, c);
		    cache_func_panel.add(jcb);

		    jcb = new JButton("Load cache");
		    jcb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				cacheLoadFromFile();
			    }
			});
		    c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 2;
		    c.weighty = 1;
		    cache_func_gridbag.setConstraints(jcb, c);
		    cache_func_panel.add(jcb);

		    // -------------------------------------
		    // add this panel
		    //
		    c = new GridBagConstraints();
		    cache_panel.add(cache_func_panel);
		    c.gridx = 0;
		    c.gridy = 1;
		    c.weightx = c.weighty = 1.0;
		    c.fill = GridBagConstraints.BOTH;
		    cache_gridbag.setConstraints(cache_func_panel, c);

		}


		JPanel cache_stats_panel = new JPanel();
		{
		    line = 0;

		    cache_stats_panel.setBorder(BorderFactory.createTitledBorder(" Statistics "));
		    GridBagLayout cache_stats_gridbag = new GridBagLayout();
		    cache_stats_panel.setLayout(cache_stats_gridbag);

		    JLabel label  = new JLabel("Entries:  ");
		    GridBagConstraints c = new GridBagConstraints();
		    cache_stats_panel.add(label);
		    c.gridx = 0;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.EAST;
		    c.weightx = c.weighty = 1.0;
		    cache_stats_gridbag.setConstraints(label, c);

		    cache_entries_label = new JLabel();
		    
		    c = new GridBagConstraints();
		    cache_stats_panel.add(cache_entries_label);
		    c.gridx = 1;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.WEST;
		    c.weightx = c.weighty = 1.0;
		    cache_stats_gridbag.setConstraints(cache_entries_label, c);
		    line++;


		    label  = new JLabel("Size:  ");
		    c = new GridBagConstraints();
		    cache_stats_panel.add(label);
		    c.gridx = 0;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.EAST;
		    c.weightx = c.weighty = 1.0;
		    cache_stats_gridbag.setConstraints(label, c);

		    cache_size_label  = new JLabel();
		    c = new GridBagConstraints();
		    cache_stats_panel.add(cache_size_label);
		    c.gridx = 1;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.WEST;
		    c.weightx = c.weighty = 1.0;
		    cache_stats_gridbag.setConstraints(cache_size_label, c);

		    line++;

		    label  = new JLabel("Hit rate:  ");
		    c = new GridBagConstraints();
		    cache_stats_panel.add(label);
		    c.gridx = 0;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.EAST;
		    c.weightx = c.weighty = 1.0;
		    cache_stats_gridbag.setConstraints(label, c);

		    cache_hr_label  = new JLabel();
		    c = new GridBagConstraints();
		    cache_stats_panel.add(cache_hr_label);
		    c.gridx = 1;
		    c.gridy = line;
		    c.anchor = GridBagConstraints.WEST;
		    c.weightx = c.weighty = 1.0;
		    cache_stats_gridbag.setConstraints(cache_hr_label, c);

		    line++;
		    
		    updateCacheStatsLabels();

		    JButton jcb = new JButton("Update");
		    jcb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				updateCacheStatsLabels();
			    }
			});
		    c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = line;
		    c.gridwidth = 2;
		    cache_stats_gridbag.setConstraints(jcb, c);
		    cache_stats_panel.add(jcb);

		    // -------------------------------------
		    // add this panel
		    //
		    c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 0;
		    c.gridheight = 2;
		    c.weightx = 1.0;
		    c.weighty = 3.0;
		    c.fill = GridBagConstraints.BOTH;
		    cache_gridbag.setConstraints(cache_stats_panel, c);
		    cache_panel.add(cache_stats_panel);
		}


		tabbed_pane.addTab(" Cache ", cache_panel);
	    }
	    
	    // --------------------------------------------------------------------
	    // ====================================================================
	    //
	    // the autoload panel
	    //
	    // ====================================================================
	    // --------------------------------------------------------------------

	    {
		JPanel autoload_panel = new JPanel();
		GridBagLayout autoload_gridbag = new GridBagLayout();
		autoload_panel.setLayout(autoload_gridbag);
		autoload_panel.setBorder(BorderFactory.createEmptyBorder(5,25,5,25));
		
		line = 0;

		JLabel label = new JLabel("Autoload attempts to fill the annotation cache");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.SOUTH;
		c.gridwidth = 2;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);
		line++;
		
		label = new JLabel("using multiple threads running in the background.");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		//c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 2;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);
		line++;

		autoload_state_label = new JLabel("State: (idle)");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 2;
		c.weighty = 0.6;
		autoload_gridbag.setConstraints(autoload_state_label, c);
		autoload_panel.add(autoload_state_label);
		line++;

		
		autoload_enable_jchkb = new JCheckBox("Enable autoload");

		autoload_enable_jchkb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				autoload = autoload_enable_jchkb.isSelected();
				if(autoload == true)
				{
				    priority_slider.setEnabled(false);
				    autoload_spot_index = 0;
				    if(autoload_thread == null)
				    {
					autoload_thread = new AutoLoadThread();
					autoload_thread.start();
				    }
				}
				else
				{
				    priority_slider.setEnabled(true);
				}
			    }
			});

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		//c.anchor = GridBagConstraints.EAST;
		//c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 2;
		autoload_gridbag.setConstraints(autoload_enable_jchkb, c);
		autoload_panel.add(autoload_enable_jchkb);
		line++;

		
		autoload_apply_filter_jchkb = new JCheckBox("Apply filter");
		autoload_apply_filter_jchkb.setSelected(autoload_apply_filter);
		autoload_apply_filter_jchkb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    autoload_apply_filter = autoload_apply_filter_jchkb.isSelected();
			}
		    });

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		//c.anchor = GridBagConstraints.EAST;
		//c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 2;
		autoload_gridbag.setConstraints(autoload_apply_filter_jchkb, c);
		autoload_panel.add(autoload_apply_filter_jchkb);
		line++;

		// ===========================================================

		label = new JLabel("Priority");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.SOUTH;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);
		line++;

		priority_slider = new JSlider(JSlider.HORIZONTAL, Thread.MIN_PRIORITY,Thread.MAX_PRIORITY,Thread.MIN_PRIORITY);
		//priority_slider.setPaintTicks(true);
		//priority_slider.setMajorTickSpacing(1);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.gridwidth = 2;
		//c.anchor = GridBagConstraints.SOUTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.weighty = 1.0;
		autoload_gridbag.setConstraints(priority_slider, c);
		autoload_panel.add(priority_slider);
		line++;

		label = new JLabel("Low");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);

		label = new JLabel("High");
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);

		// ===========================================================

		label = new JLabel("Max. Active Threads");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.SOUTH;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);
		line++;

		threads_slider = new JSlider(JSlider.HORIZONTAL, 1, 16, autoload_max_active_threads);
		//priority_slider.setPaintTicks(true);
		//priority_slider.setMajorTickSpacing(1);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.gridwidth = 2;
		//c.anchor = GridBagConstraints.SOUTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.weighty = 1.0;
		autoload_gridbag.setConstraints(threads_slider, c);
		autoload_panel.add(threads_slider);
		line++;

		label = new JLabel("1");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);

		label = new JLabel("16");
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		autoload_gridbag.setConstraints(label, c);
		autoload_panel.add(label);

		// ===========================================================

		JButton cancel_pending = new JButton("Cancel all pending loads");
		cancel_pending.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				cancelAllRequests();
			    }
			});

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0;
		c.gridwidth = 2;
		autoload_gridbag.setConstraints(cancel_pending, c);
		autoload_panel.add(cancel_pending);
		line++;

		// ===========================================================

		tabbed_pane.addTab(" Autoload ", autoload_panel);
	    }

	    // --------------------------------------------------------------------
	    // ====================================================================
	    //
	    // tabbed panel
	    //
	    // ====================================================================
	    // --------------------------------------------------------------------

	    {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		//c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 2.0;
		gridbag.setConstraints(tabbed_pane, c);
		panel.add(tabbed_pane);
	    }

	    // --------------------------------------------------------------------
	    // ====================================================================
	    //
	    // buttons
	    //
	    // ====================================================================
	    // --------------------------------------------------------------------

	    {
		JPanel button_panel = new JPanel();
		button_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		GridBagLayout button_gridbag = new GridBagLayout();
		button_panel.setLayout(button_gridbag);
		
		{
		    final JButton jb = new JButton(" Close ");
		    button_panel.add(jb);
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				saveDefaultOptions();
				if(cache_data_timer != null)
				{
				    cache_data_timer.stop();
				    cache_data_timer = null;
				}
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
		    final JButton jb = new JButton(" Reset ");
		    button_panel.add(jb);
		    jb.setEnabled(false);
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
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
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				mview.getHelpTopic("AnnotationLoader");
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

		{
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 1;
		    //c.gridwidth = 2;
		    c.anchor = GridBagConstraints.CENTER;
		    c.weightx = 1.0;
		    //c.weighty = 2.0;
		    gridbag.setConstraints(button_panel, c);
		    panel.add(button_panel);
		}
	    }
	    pack();
	    mview.locateWindowAtCenter(this);
	}
    }
    
    // --------------------------------------------------------------------
    // ====================================================================
    //
    // sources
    //
    // ====================================================================
    // --------------------------------------------------------------------

    protected void addSource()
    {
	AnnoSource src = new AnnoSource(AnnoSource.SourceFile, false,"(New Source)", "", null);

	AnnoSource[] source_a_new = new AnnoSource[source_a.length+1];

	for(int s=0; s< source_a.length; s++)
	    source_a_new[s] = source_a[s];

	// add the new one at the end
	source_a_new[source_a.length] = src;
	
	source_a = source_a_new;

        addControls();
    }

    protected void deleteSource(int s)
    {
	if(mview.infoQuestion("Really delete '" + source_a[s].name + "' ?", "Yes", "No") == 0)
	{
	    AnnoSource[] source_a_new = new AnnoSource[source_a.length-1];
	    
	    int new_p = 0;
	    for(int ss=0; ss< source_a.length; ss++)
		if(ss != s)
		    source_a_new[new_p++] = source_a[ss];
	    
	    source_a = source_a_new;
	    
	    addControls();
	}
    }

    protected void lowerSource(int s)
    {
	swapSources(s, s+1);
    }
    protected void raiseSource(int s)
    {
	swapSources(s, s-1);
    }

    protected void swapSources(int s1, int s2)
    {
	AnnoSource tmp = source_a[s1];
	source_a[s1] = source_a[s2];
	source_a[s2] = tmp;

	updateSourceControls();
    }

    // get the default values from the app properties
    private void getDefaultOptions()
    {

	final String[] fallback_data = 
	{ "/home/dave/bio/data/annotation/",
	  "http://www.bioinf.man.ac.uk/maxd/annotation/",
	  "jdbc:postgresql://localhost/maxd"
	};
	final String[] fallback_on = 
	{ "yes",
	  "no", 
	  "no"
	};
	final String[] fallback_use = 
	{ "file",
	  "url", 
	  "jdbc"
	};
	
	/*
	def_opt_data     = new String[n_opt_lines];
	def_opt_use      = new int[n_opt_lines];
	def_opt_on       = new boolean[n_opt_lines];

	String[] def_opt_on_str   = new String[n_opt_lines];
	String[] def_opt_use_str  = new String[n_opt_lines];
	
	// get the default values from the app properties
	//
	for(int ol=0; ol < n_opt_lines; ol++)
	{
	    
	    def_opt_on_str[ol]   = mview.getProperties().getProperty("annlo.opt" + ol + ".on",   fallback_on[ol]);
	    def_opt_use_str[ol]  = mview.getProperties().getProperty("annlo.opt" + ol + ".use",  fallback_use[ol]);
	    def_opt_data[ol]     = mview.getProperties().getProperty("annlo.opt" + ol + ".data", fallback_data[ol]);
	}

	// parse the string values
	//
	for(int ol=0; ol < n_opt_lines; ol++)
	{
	    def_opt_on[ol] = def_opt_on_str[ol].equals("yes");
	    
	    def_opt_use[ol] = 0;
	    if(def_opt_use_str[ol].equals("url"))
		def_opt_use[ol] = 1;
	    if(def_opt_use_str[ol].equals("jdbc"))
		def_opt_use[ol] = 2;
	}
	*/

    }

    private void saveDefaultOptions()
    {
	/*
	final String[] use_name = { "file", "url", "jdbc" };
	
	for(int ol=0; ol < n_opt_lines; ol++)
	{
	    if(ol > 0)
		mview.getProperties().put("annlo.opt" + ol + ".on",  (opt_on_jchkb[ol].isSelected() ? "yes" : "no"));
	    mview.getProperties().put("annlo.opt" + ol + ".use",  use_name[opt_use_jcb[ol].getSelectedIndex()]);
	    mview.getProperties().put("annlo.opt" + ol + ".data", opt_data_jtf[ol].getText());
	}
	*/	

	saveSource();
	
    }

    // --------------------------------------------------------------------
    // ====================================================================
    //
    // autoload
    //
    // ====================================================================
    // --------------------------------------------------------------------

    final boolean debug_autoload = false;

    class AutoLoadThread extends Thread
    {
	public AutoLoadThread()
	{
	}

	public void run()
	{
	    while((autoload == true))
	    {
		setPriority(priority_slider.getValue());
		
		autoload_max_active_threads = threads_slider.getValue();

		//System.out.println("priority is " + priority_slider.getValue());

		// String name = edata.getProbeName(autoload_spot_index);

		// autoload_state_label.setText("State: loading " + autoload_spot_index + " of " + edata.getNumSpots());

		if(debug_autoload)
		    System.out.println("autoload: checking spot " + autoload_spot_index);
			
		if(autoload_spot_index < edata.getNumSpots())
		{
		    if(!autoload_apply_filter || !edata.filter(autoload_spot_index))
			requestLoadAnnotationInParts(null, autoload_spot_index, false);
		    autoload_spot_index++;
		}
		else
		{
		    // autoload = false;
		}

		autoload_state_label.setText(ann_request_v.size() + " pending load requests, " +
					     active_thread_v.size() + " active threads");
		
		updateCacheStatsLabels();

		/*
		if(!isCached(name))
		{
		    // this data is not cached, is it's cache line empty?
		    int line = cacheLine(name);
		    if(cache_tag[line] == null)
		    {
			if(debug_autoload)
			    System.out.println("autoload: cache line is free, loading...");
			loadAnnotation(null, name);
		    }
		    else
		    {
			if(debug_autoload)
			    System.out.println("autoload: cache line is in use");
		    }
		}
		else
		{
		    if(debug_autoload)
			System.out.println("autoload: already cached");
		}
		*/

		yield();
	    }
	    
	    if(debug_autoload)
	    {
		if(autoload == false)
		    System.out.println("autoload: disabled");
		if(autoload_spot_index >= edata.getNumSpots())
		    System.out.println("autoload: visited all spots");
	    }

	    autoload_state_label.setText("State: (idle)");
	    autoload = false;
	    autoload_enable_jchkb.setSelected(false);
	    autoload_thread = null;
	}

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    //  only interested in (gene/probe) name updates
    //
    public final void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.NameChanged:
	case ExprData.ElementsAdded:
	    if((autoload == true) && (autoload_thread == null))
	    {
		if(due.spot >= 0)
		    autoload_spot_index = due.spot;
		else
		    autoload_spot_index = 0;

		autoload_thread = new AutoLoadThread();
		autoload_thread.start();
	    }
	    break;
	}
    }

    public final void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public final void measurementUpdate(ExprData.MeasurementUpdateEvent sue)
    {
    }
    public final void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    // 
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    // gubbins
    //
    // --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  --=--   --=--   --=--   --=--  
    //
    protected ExprData edata;

    OptionsFrame of = null;

    /*
    String[]  def_opt_data = null;
    int[]     def_opt_use  = null;
    boolean[] def_opt_on   = null;
    */
    


    private AnnoSource[]  source_a = null;

    private boolean use_gene_names = true;
    private boolean use_probe_name = false;

    private boolean anno_debug = false;

    private JCheckBox[]  active_jchkb;
    private JTextField[] code_jtf;
    private JTextField[] args_jtf;
    private JTextField[] name_jtf;
    private JComboBox[]  mode_jcb;
    private JScrollPane  source_scroller;
    private JPanel       source_panel;
    private JButton[] 	 raise_jb;
    private JButton[] 	 lower_jb;
    private JButton[] 	 delete_jb;
    //private JCheckBox[] probe_jchkb;
    //private JCheckBox[] gene_jchkb;

    private ImageIcon raise_ii;
    private ImageIcon lower_ii;
    private ImageIcon delete_ii;
    
    private JLabel cache_hr_label;
    private JLabel cache_size_label;
    private JLabel cache_entries_label;

    /*
    private JCheckBox[]   opt_on_jchkb = null;
    private JComboBox[]   opt_use_jcb = null;
    private JTextField [] opt_data_jtf = null;
    */

    protected JCheckBox autoload_enable_jchkb = null;
    protected JCheckBox autoload_apply_filter_jchkb = null;
    protected boolean autoload = false;
    protected boolean autoload_apply_filter = true;
    protected AutoLoadThread autoload_thread = null;
    protected int autoload_spot_index = 0;
    protected Timer cache_data_timer = null;
    protected int autoload_max_active_threads = 5;
    JSlider priority_slider = null;
    JSlider threads_slider = null;
    JLabel autoload_state_label = null;
}
