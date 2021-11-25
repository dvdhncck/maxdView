//package uk.ac.man.bioinf.maxdLoad2;

import java.util.*;
import java.sql.*;
import java.util.jar.*;
import java.io.*;

public class maxdConnection_m2
{


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public maxdConnection_m2( Controller cntrl_, String desired_version_ )
    {
	cntrl = cntrl_;
	desired_version = desired_version_;
    }

    
    public String getHost()
    {
	return host_name;
    }

    public String getUserName()
    {
	return user_name;
    }

    public Connection getConnection()
    {
	return connection;
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public void commit()
    {
	try
	{
	    if( connection != null )
	    {
		connection.commit();
	    }
	}
	catch(java.sql.SQLException sqle)
	{ 
	    System.err.println("Unable to commit...." + sqle);
	}
    }

    public void setAutoCommit( boolean ac_mode )
    {
	try
	{
	    if( connection != null )
	    {
		System.out.println( "Information: switching AutoCommit " + ( ac_mode ? "on" : "off" ) );

		connection.setAutoCommit( ac_mode );
	    }
	}
	catch(SQLException sqle)
	{
	    //alertMessage("Unable to disable autocommit mode");
	}

    }

    public void rollback()
    {
	try
	{
	    connection.rollback();
	    cntrl.infoMessage("Batch transaction cancel, rollback successful");
	}
	catch(SQLException sqle)
	{
	    cntrl.alertMessage("Unable to cancel batch transaction. Some or all of the data may not have been stored properly.");
	}
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private int next_id = 0;

    public String generateUniqueID(String table_name, String column_name)
    {
	String result = "0";
	
	int max_id = 0;
	
	ResultSet rs = null;
	Statement stmt = null;
	
	try
	{
	    stmt = connection.createStatement();
	    stmt.setEscapeProcessing(false);
	    rs = stmt.executeQuery("SELECT MAX(" + qField(column_name) + ") FROM " + 
				   qTable(table_name));
	}
	catch(SQLException sqle)
	{
	    // fallback, try MAX(INT(ID)) ...
	    //
	    try
	    {
		if(stmt != null)
		{
		    rs = stmt.executeQuery("SELECT MAX(INT(" + qField(column_name) + ")) FROM " + 
					   qTable(table_name));
		}
	    }
	    catch(SQLException sqle2)
	    {
		cntrl.alertMessage("generateUniqueID(): Unable to find the biggest ID currently in use\n(for table " + 
			     qTable(table_name) +")\n\nSQL error is:\n" + sqle2);
	    }
	}
	

	if(rs != null)
	{
	    //  System.out.println("got a result set");
	    
	    try
	    {
		while (rs.next()) 
		{
		    String str = rs.getString(1);
		    if(str != null)
		    {
			    try
			    {
				//System.out.println("generateUniqueID(): max id found, is " + str);
				max_id = (new Integer(str).intValue()) + 1;
			    }
			    catch (NumberFormatException nfe)
			    {
				max_id = 0;
				//alertMessage("generateUniqueID(): " + nfe);
			    }
		    }
		    //System.out.println("generateUniqueID(): max id in " + column_name + " is " + max_id);
		}
	    }
	    catch(SQLException sqle)
	    {
		cntrl.alertMessage("generateUniqueID(): " + sqle);
	    }
	    finally
	    {
		try
		{
		    rs.close();
		}
		catch(Exception ex1)
		{
		}
	    }		
	    
	    try
	    {
		result = new String(Integer.toString(max_id));
	    }
	    catch (NumberFormatException nfe)
	    {
		result = "0";
	    }
	}
	else
	{
	    cntrl.alertMessage("generateUniqueID(): table " + qTable(table_name) + " missing?");  
	}


	// clean up.....
	try
	{
	    if(stmt != null)
		stmt.close();
	}
	catch(Exception ex2)
	{
	}
	
	
	return result;
    }
    

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

 
    //
    //
    // converts a string which might contains unicode characters into one which is
    // pure 7-bit ascii by representing the unicode characters using the \uABCD notation
    // (where ABCD is a 4 digit hex value encoding the unicode code)
    //
    // (with a little help from http://www.i18nfaq.com/java.html)
    //
    static final String escapeUnicode( final String str ) 
    {
	if( str == null )
	    return null;

	if( str.length() == 0 )
	    return null;

	final StringBuffer ostr = new StringBuffer();
	final int str_len = str.length();

	for(int i=0; i < str_len; i++) 
	{
	    final char ch = str.charAt( i );

	    if ( (ch >= 0x0020) && (ch <= 0x007e) )
	    {
		// it's a normal printable ASCII character...

		ostr.append( ch );
	    }
	    else 
	    {
		// it's not a normal 7-bit ASCII character, convert it the character
		// code to a hex number and encode it using the \uABCD representation
		// (where ABCD is a 4 digit hex value encoding the unicode code)

		String hex = Integer.toHexString( ch & 0xFFFF );

		ostr.append( "\\u" );

		// pad the hex so that we always generate 4 digit encodings, even for 16 bit codes
		switch( hex.length() )
		{
		case 1:
		    ostr.append( "000" );
		    break;
		case 2:
		    ostr.append( "00" );
		    break;
		case 3:
		    ostr.append( "0" );
		    break;
		}

		// and convert it to uppercase and append it the string 
		ostr.append( hex.toUpperCase( java.util.Locale.ENGLISH ) );
	    }
	}

	return ostr.toString();
    }


    static final String unescapeUnicode( final String str ) 
    {
	final StringBuffer ostr = new StringBuffer();

	final int len = str.length();

	int i1 = 0;

	while( i1 < len )
	{
	    final int i2 = str.indexOf( "\\u", i1 );

	    if (i2 == -1 ) 
	    {
		// no more unicode in the string,  append everything from the previous stop point

		ostr.append( str.substring( i1, len ) );

		// and we are finished

		return ostr.toString();
	    }
	    else
	    {
		// append everything from the previous stop point up to the unicode character

		ostr.append( str.substring(i1, i2) );
		
		try 
		{
		    // extract the 4 characters which are the hex representation of the unicode, and
		    // convert them to the corresponding 32-bit integer, then downgrade this to a Java char
		    // and append it to the buffer

		    ostr.append( (char) Integer.parseInt( str.substring( i2 + 2, i2 + 6 ), 16 ) );
		} 
		catch (NumberFormatException exp) 
		{
		    // something has gone wrong parsing the 4 digit hex value
		    
		    ostr.append( '?' ); 
		}
		
		// and move the pointer past these 6 characters ready to continue scanning the string

		i1 = i2 + 6;
	    }
	}
	
	return ostr.toString() ;
    }
    

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --



    // add a new description row to the database
    // (if the description is longer than N chars, then split
    //  it into a number of chunks and store each of 
    //  them with the same ID, and sequential indices so they can be 
    //  joined together on retrieval)
    //
    //  call with 'desc' == null to just generate
    //  an empty Description entry, used for linking
    //  Property's to
    //
    public final String generateDescriptionID(String desc)
    {
	if( is_version_two )
	    return generateDescriptionIDInVersionTwoDatabase( desc );

	if( is_version_one )
	    return generateDescriptionIDInVersionOneDatabase( desc );

	return null;
    }

    private final  String generateDescriptionIDInVersionTwoDatabase(String desc)
    {
	// System.out.println("saving: " + desc);

	String tidy_desc = tidyText( escapeUnicode( desc ) );

	String[] desc_rows = splitTextIntoChunks( tidy_desc, max_text_property_chunk_length );

	if( desc_rows == null )
	    return null;

	if( desc_rows.length == 0 )
	    return null;
	
	String desc_id = generateUniqueID("AttributeText", "ID");
	

	// most of the statement is constant, only the vars for 'Index' and 'Value' need to change
	final String header_part = 
	    "INSERT INTO " + qTable("AttributeText") + 
	    " (" + qField("ID") + "," + qField(index_field_name) + "," + qField("Value") + ") VALUES (" + qDescID( desc_id ) + ",";

	for(int dr = 0; dr < desc_rows.length; dr++)
	{
	    StringBuffer sbuf = new StringBuffer( header_part );
	    sbuf.append( dr );
	    sbuf.append( "," );
	    sbuf.append( qText( desc_rows[ dr ] ) );
	    sbuf.append( ")\n" );

	    if(executeStatement( sbuf.toString() ) == false)
	    {
		return null;
	    }
	}

	return desc_id;
    }

    //
    // important: need to make sure the split points do not leave the escape character
    //            at the end of a chunk (at the thing it's supposed to be escaping at
    //             the start of the next chunk!)
    //
    //            also: make sure the last chraracter isn't a ' ', because MySQL trims
    //            trailing spaces from VARCHARS which can cause problems when that trailing
    //            space is significant (i.e. there's another word after it, but that word)
    //            has been paritioned into the next chunk
    //
    public static final String[] splitTextIntoChunks( String input, int max_chunk_length )
    {
	if( ( input == null ) || ( input.length() == 0 ) )
	    return null;

	java.util.Vector chunks = new java.util.Vector();

	boolean finished = false;

	final int last_char_index = input.length() - 1;

	int start = 0;

	while( ! finished )
	{
	    int end = start + ( max_chunk_length - 1 );
	    
	    if( end > last_char_index )
		end = last_char_index;

	    while( ( end > start ) && ( ( input.charAt( end ) == '\\' ) || ( input.charAt( end ) == ' ' ) ) )
	    {
		end--;
	    }

	    if( end >= start )
	    {
		chunks.add( input.substring( start, end + 1 ) );
	    }
	    else
	    {
		// this is potentially a serious problem....
		// it seems that there is an unbroken series of escape chars that
		// is longer than 'max_chunk_length'
		// ... let's live dangerously and just ignore them
	    }

	    start = end + 1;

	    finished = ( start > last_char_index );
	}

	
	// double-check that all has gone ok....

	StringBuffer buf = new StringBuffer();
	boolean too_long = false;
	for(int c=0; c < chunks.size(); c++)
	{
	   String chunk = (String) chunks.elementAt( c );
		
	   buf.append( chunk );
	    
	   if( chunk.length() > max_chunk_length )
	       too_long = true;
	   
	}

	if( too_long || ( buf.toString().equals( input ) == false ) )
	{
	    System.err.println( "splitTextIntoChunks(): FAILURE= " + ( too_long ? "TOO LONG" : "MISMATCH" ) );
	
	    for(int c=0; c < chunks.size(); c++)
	    {	
		String chunk = (String) chunks.elementAt( c );
		
		System.out.println( (c+1) + ":<<" + chunk + ">>(" + chunk.length() + ")" );
	    }
	}
	
	return (String[]) chunks.toArray( new String[ chunks.size() ] );
    }

    public final String[] OLD_splitTextIntoChunks( String input, int max_chunk_length )
    {
	String[] chunks = null;

	final int last_c = input.length();

	if((input != null) && (input.length() > max_chunk_length))
	{
	    int start = 0;

	    final int n_chunks = ( last_c / max_chunk_length ) + 1;

	    chunks = new String[n_chunks];

	    for(int dr=0; dr < n_chunks; dr++)
	    {
		int end = start + max_chunk_length;

		if( end > last_c )
		{
		    end = input.length();
		}
		if( start < last_c )
		{
		    chunks[dr] = input.substring(start, end);
		}

		start += max_chunk_length;
	    }

	    return chunks;
	}
	else
	{
	    if((input != null) && (input.length() > 0))
	    {
		chunks = new String[1];
		chunks[0] = input;
		return chunks;
	    }
	    else
	    {
		return null;
	    }
	}

    }

    private final String generateDescriptionIDInVersionOneDatabase(String desc)
    {
	String desc_id = generateUniqueID("Description", "ID");
	
	int n_desc_rows = 1;
	String[] desc_rows = null;

	if((desc != null) && (desc.length() > max_text_property_chunk_length))
	{
	    int start = 0;
	    n_desc_rows = (desc.length() / max_text_property_chunk_length) + 1;
	    desc_rows = new String[n_desc_rows];
	    for(int dr=0; dr < n_desc_rows; dr++)
	    {
		int end = start + max_text_property_chunk_length;
		if(end > desc.length())
		{
		    end = desc.length();
		}
		if(start < desc.length())
		{
		    desc_rows[dr] = desc.substring(start, end);
		}
		start += max_text_property_chunk_length;
	    }
	}
	else
	{
	    if((desc != null) && (desc.length() > 0))
	    {
		n_desc_rows = 1;
		desc_rows = new String[1];
		desc_rows[0] = desc;
	    }
	    else
	    {
		n_desc_rows = 0;
	    }
	}

	String sql = "INSERT INTO " + qTable("Description") + " (" + qField("ID") + ") VALUES (" + qDescID(desc_id) + ")\n";
	
	if(executeStatement(sql) == false)
	{
	    return null;
	}

	for(int dr=0; dr < n_desc_rows; dr++)
	{
	    StringBuffer sbuf = new StringBuffer();

	    sbuf.append("INSERT INTO ");
	    sbuf.append(qTable("TextProperty"));
	    
	    sbuf.append(" ( " + qField(index_field_name) + ", ");
	    sbuf.append(qField("Description_ID") + ", " + qField("Text") + ")\n");
	    
	    sbuf.append("VALUES ( " + dr + ", ");
	    sbuf.append(qDescID(desc_id));
	    sbuf.append(", ");
	    sbuf.append(qText(tidyText(desc_rows[dr])));
	    sbuf.append(" )\n");
	    
	    if(executeStatement(sbuf.toString()) == false)
	    {
		return null;
	    }
	}

	return desc_id;
    }



    public final boolean updateDescription( String desc_id, String desc )
    {
	if(desc_id == null)
	    return false;
	
	if( is_version_two )
	    return updateDescriptionInVersionTwoDatabase( desc_id,  desc);

	if( is_version_one )
	    return updateDescriptionInVersionOneDatabase( desc_id,  desc);

	return false;

    }

    private final boolean updateDescriptionInVersionTwoDatabase( String desc_id, String desc )
    {
	String tidy_desc = tidyText( escapeUnicode( desc ) );

	String[] desc_rows = splitTextIntoChunks( tidy_desc, max_text_property_chunk_length );

	final int n_desc_rows = desc_rows.length;
	
	// remove the old text associated with the Description_ID
	//
	String sql = "DELETE FROM " + qTable("AttributeText") + " WHERE " + qField("ID") + " = " + qDescID(desc_id) + "\n";
	
	if( executeStatement(sql) == false )
	{
	    return false;
	}

	if( desc_rows == null )
	    return true;

	if( desc_rows.length == 0 )
	    return true;

	// most of the statement is constant, only the vars for 'Index' and 'Value' need to change
	final String header_part = 
	    "INSERT INTO " + qTable("AttributeText") + 
	    " (" + qField("ID") + "," + qField(index_field_name) + "," + qField("Value") + ") VALUES (" + qDescID( desc_id ) + ",";
	
	for( int dr=0; dr < n_desc_rows; dr++ )
	{
	    /*
	      if( desc_rows[ dr ].length() > max_text_property_chunk_length )
	      {
	      System.out.println( "alert: chunk too long!\n  max len=" + 
	      max_text_property_chunk_length + "\n  actual len=" + 
	      desc_rows[ dr ].length() + "\n  chunk=<<" + desc_rows[ dr ] + ">>" );
	      }
	    */
	    
	    StringBuffer sbuf = new StringBuffer( header_part );
	    sbuf.append( dr );
	    sbuf.append( "," );
	    sbuf.append( qText( desc_rows[ dr ] ) );
	    sbuf.append( ")\n" );

	    //--------------------------------------
	    //
	    //System.out.println( "chunk<<" + dr + ">>\ntext<<" + desc_rows[ dr ] + ">>(" + 
	    //			desc_rows[ dr ].length() + ")\nsql<<" + sbuf.toString() + ">>\n" );
	    //
	    //--------------------------------------
	    
	    if(executeStatement(  sbuf.toString() ) == false)
	    {
		return false;
	    }
	}

	return true;
    }
    

    private final boolean updateDescriptionInVersionOneDatabase(String desc_id, String desc)
    {
	int n_desc_rows = 1;
	String[] desc_rows = null;

	if((desc != null) && (desc.length() > max_text_property_chunk_length))
	{
	    int start = 0;
	    n_desc_rows = (desc.length() / max_text_property_chunk_length) + 1;
	    desc_rows = new String[n_desc_rows];
	    for(int dr=0; dr < n_desc_rows; dr++)
	    {
		int end = start + max_text_property_chunk_length;
		if(end > desc.length())
		{
		    end = desc.length();
		}
		if(start < desc.length())
		{
		    desc_rows[dr] = desc.substring(start, end);
		}
		start += max_text_property_chunk_length;
	    }
	}
	else
	{
	    if((desc != null) && (desc.length() > 0))
	    {
		n_desc_rows = 1;
		desc_rows = new String[1];
		desc_rows[0] = desc;
	    }
	    else
	    {
		n_desc_rows = 0;
	    }
	}
	
	// remove the old text associated with the Description_ID
	//
	String sql = 
	"DELETE FROM " + qTable("TextProperty") + 
	" WHERE " + qField("Description_ID") + " = " +
	qDescID(desc_id) + "\n";
	
	if(executeStatement(sql) == false)
	{
	    return false;
	}

	for(int dr=0; dr < n_desc_rows; dr++)
	{
	    sql = "INSERT INTO " + 
	    qTable("TextProperty") + " (" + 
	    qField(index_field_name) + ", " + 
	    qField("Description_ID") + ", " + 
	    qField("Text") + ")\n" + 
	    "VALUES ( " + dr + ", " + qDescID(desc_id) + ", " + qText(tidyText(desc_rows[dr])) + ")\n";
	    
	    if(executeStatement(sql) == false)
	    {
		return false;
	    }
	}

	return true;
    }
 
    // recover all of the text and properties assoicated with a Description_ID
    //
    public final String getDescription(String id)
    {
	if((id == null) || (id.length() == 0))
	    return "";
		
	// NOTE: added a call to untidyText() to keep Oracle happy

	if( is_version_two )
	    return unescapeUnicode( untidyText( getDescriptionFromVersionTwoDatabase( id ) ) );

	if( is_version_one )
	    return untidyText( getDescriptionFromVersionOneDatabase( id ) );

	return null;
    }

    // v2.x database uses a slightly different table and field naming scheme to that of v1.x
    //
    private final String getDescriptionFromVersionTwoDatabase( String id )
    {
		
	final String query = 
	    "SELECT " + qField(index_field_name) + ", " + qField("Value") + 
	    " FROM " + qTable("AttributeText") + "\n WHERE " + qField("ID") + " = " + qDescID(id);
	
	ResultSet rs = executeQuery(query);
	
	StringBuffer sbuf = new StringBuffer();

	if(rs != null)
	{
	    Vector text_v = new Vector();
	    Vector index_v = new Vector();

	    try
	    {
		while (rs.next()) 
		{
		    index_v.addElement(new Integer(rs.getInt(1)));
		    text_v.addElement(rs.getString(2));
		    //System.out.println("getMatch(): max id in " + column_name + " is " + max_id);
		}
	    }
	    catch(SQLException sqle)
	    {
		cntrl.alertMessage("getDescription(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }

	    int n_bits = text_v.size();
	    if(n_bits == 1)
		return (String)text_v.elementAt(0);
	    else
	    {
		try
		{
		    // reorder the chunks
		    String[] chunks = new String[n_bits];
		    for(int b=0;b < n_bits; b++)
			chunks[((Integer)index_v.elementAt(b)).intValue()] = (String)text_v.elementAt(b);
		    for(int b=0;b < n_bits; b++)
			sbuf.append(chunks[b]);
		}
		catch(NumberFormatException nfe)
		{
		    cntrl.alertMessage("getDescription(): illegal index");  
		}
	    }
	}
	else
	    cntrl.alertMessage("getDescription(): table missing?");  
	

	//System.out.println("getDescription(): result = [" + sbuf.toString() + "]" );

	return sbuf.toString();
    }


    private final String getDescriptionFromVersionOneDatabase(String id)
    {
	final String query = 
	    "SELECT " + qField(index_field_name) + ", " + qField("Text") + 
	    " FROM " + qTable("TextProperty") + "\n WHERE " + qField("Description_ID") + " = " + qDescID(id);
	
	ResultSet rs = executeQuery(query);
	
	StringBuffer sbuf = new StringBuffer();

	if(rs != null)
	{
	    Vector text_v = new Vector();
	    Vector index_v = new Vector();

	    try
	    {
		while (rs.next()) 
		{
		    index_v.addElement(new Integer(rs.getInt(1)));
		    text_v.addElement(rs.getString(2));
		    //System.out.println("getMatch(): max id in " + column_name + " is " + max_id);
		}
	    }
	    catch(SQLException sqle)
	    {
		cntrl.alertMessage("getDescription(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }

	    int n_bits = text_v.size();
	    if(n_bits == 1)
		return (String)text_v.elementAt(0);
	    else
	    {
		try
		{
		    // reorder the chunks
		    String[] chunks = new String[n_bits];
		    for(int b=0;b < n_bits; b++)
			chunks[((Integer)index_v.elementAt(b)).intValue()] = (String)text_v.elementAt(b);
		    for(int b=0;b < n_bits; b++)
			sbuf.append(chunks[b]);
		}
		catch(NumberFormatException nfe)
		{
		    cntrl.alertMessage("getDescription(): illegal index");  
		}
	    }
	}
	else
	    cntrl.alertMessage("getDescription(): table missing?");  
	
	//
	// ::TODO:: append all [Numeric]Property's
	//

	return sbuf.toString();
    }



    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --



    public String getNameFromID(String id, String table)
    {
	return getFieldFromOtherField( "Name", "ID", qID(id), table);
    }

    public String getIDFromName(String name, String table)
    {
	return getFieldFromOtherField( "ID", "Name", qText(name), table);
    }

    public String getFieldFromID( String id, String field, String table)
    {
	return getFieldFromOtherField( field, "ID", qID(id), table);
    }

    public String getFieldFromOtherField( String dest_field, 
					  String src_field, 
					  String quoted_src_val, 
					  String table)

    {
	String sql = 
	    "SELECT " + qField(dest_field) + " FROM " + qTable(table) + 
	    " WHERE " + qField(src_field) + " = " + quoted_src_val;
	
	ResultSet rs = executeQuery(sql);

	String result = null;

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    result = rs.getString(1);
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getFieldFromOtherField(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	return result;
    }


    public boolean setFieldUsingID( String table,
				    String id, 
				    String field, 
				    String quoted_val )

    {
	String sql = 
	    "UPDATE " + qTable(table) + 
	    " SET " + qField(field) + "=" + quoted_val +
	    " WHERE " + qField("ID") + " = " + qID(id);
	
	return executeStatement(sql);
    }


    public Instance createInstanceUsingName( String name, String table )
    {
	String id = generateUniqueID( table, "ID" );
	
	if( id == null )
	    return null;

	String sql = 
	    "INSERT INTO " + 
	    qTable( table ) + " (" + 
	    qField("Name") + ", " + 
	    qField("ID") + ") VALUES (" +
	    qText( name ) + ", " + 
	    qID( id ) + ")";
	
	
	if( executeStatement( sql ) )
	{
	    return new Instance( name, id );
	}
	else
	{
	    return null;
	}
	
    }

    public Instance getInstanceFromName( String name, String table )
    {
	String sql = 
	    "SELECT " + qField("ID") +
	    " FROM " + qTable( table ) + 
	    " WHERE " + qField("Name") + " = " + qText( name );
	
	ResultSet rs = executeQuery(sql);

	Instance inst = null;

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    inst = new Instance( name, rs.getString(1) );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getInstanceFromName(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	
	return inst;
    }


    final public Instance[] getMostRecentInstancesFromName( final String name, final String table )
    {
	return getInstanceFromName( name, table, true );
    }

    final public Instance[] getLeastRecentInstancesFromName( final String name, final String table )
    {
	return getInstanceFromName( name, table, false );
     }
    
    final public Instance[] getInstanceFromName( final String name, final String table, final boolean sort_most_recent_first )
    {
	String sql = 
	    "SELECT " + qField("ID") +
	    " FROM " + qTable( table ) + 
	    " WHERE " + qField("Name") + " = " + qText( name ) +
	    " ORDER BY " + qField("ID") + ( sort_most_recent_first ? " DESC" : "" );
	
	ResultSet rs = executeQuery(sql);

	Vector inst_v = new Vector();

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    inst_v.add ( new Instance( name, rs.getString(1) ) );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getInstancesFromName(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	
	if( inst_v.size() == 0 )
	    return null;

	return (Instance[]) inst_v.toArray( new Instance[ inst_v.size() ] );
    }



    public final Instance[] getAllInstances( final String table )
    {
	String sql = 
	    "SELECT " + qField("Name") + 
	    ", " + qField("ID") +
	    " FROM " + qTable( table ) + 
	    " ORDER BY " + qField("ID") + " DESC";	

	ResultSet rs = executeQuery(sql);

	Vector inst_v = new Vector();

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    inst_v.add( new Instance( rs.getString(1), rs.getString(2) ) );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getAllInstances(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	
	if( inst_v.size() == 0 )
	    return null;
	else
	    return (Instance[]) inst_v.toArray( new Instance[ inst_v.size() ] );
    }


   public final Instance[] getSomeRecentInstances( final String table, final int max_number_of_instances )
    {
	String sql = 
	    "SELECT " + qField("Name") + 
	    ", " + qField("ID") +
	    " FROM " + qTable( table ) + 
	    " ORDER BY " + qField("ID") + " DESC";	

	ResultSet rs = executeQuery(sql);

	Vector inst_v = new Vector();

	int count = 0;

	if(rs != null)
	{
	    try
	    {
		while ( rs.next() && ( count <  max_number_of_instances ) )
		{
		    inst_v.add( new Instance( rs.getString(1), rs.getString(2) ) );
		    count++;
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getAllInstances(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	
	if( inst_v.size() == 0 )
	    return null;
	else
	    return (Instance[]) inst_v.toArray( new Instance[ inst_v.size() ] );
    }

    public Instance[] getInstancesUsingFKField(String id, String field, String table )
    {
	String sql = 
	    "SELECT " + qField("Name") + ", " + qField("ID") + 
	    " FROM " + qTable(table) + 
	    " WHERE " + qField(field) + " = " + qID(id);
	
	// eg: select Name from Extract where Sample_ID='007'

	ResultSet rs = executeQuery(sql);

	Vector result = new Vector();

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    Instance ii = new Instance( rs.getString(1), rs.getString(2) );
		    result.add( ii );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getInstancesUsingFKField(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	    
	    return (Instance[]) result.toArray( new Instance[0] );
	}
	else
	    return null;
    }

    public Instance getInstanceUsingID(String id, String table )
    {
	String sql = 
	    "SELECT " + qField("Name") + ", " + qField("ID") + 
	    " FROM " + qTable(table) + 
	    " WHERE " + qField("ID") + " = " + qID(id);
	
	// eg: select Name from Extract where Sample_ID='007'

	ResultSet rs = executeQuery(sql);

	Instance result = null;

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    result= new Instance( rs.getString(1), rs.getString(2) );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getInstanceUsingID(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}

	return result;
    }


    public Instance getLinkedInstance( String table, String id, String link_fk_field, String link_table )
    {
	String sql = 
	    "SELECT " + 
	    qTableDotField(link_table,"Name") + ", " + 
	    qTableDotField(link_table,"ID") + 
	    " FROM " + 
	    qTable(table) +  ", " + qTable(link_table) +
	    " WHERE " + 
	    qTableDotField(table, "ID") + " = " + qID(id) +
	    " AND " + 
	    qTableDotField(table, link_fk_field) + " = " + qTableDotField(link_table, "ID");
	

	ResultSet rs = executeQuery(sql);

	Instance result = null;

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    result= new Instance( rs.getString(1), rs.getString(2) );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("getLinkedInstance(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}

	return result;
    }


    public boolean updateFieldUsingID( String id, String field_name, String quoted_field_value, String table )
    {
	String sql = 
	    "UPDATE " + qTable( table ) + " SET " + qField( field_name ) + "=" + quoted_field_value + 
	    " WHERE " + qField( "ID" ) + "=" + qID(id);
	
	return executeStatement( sql );
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    /*
         
    Hybridisation ---(Labelled_Extract_List_ID) ---> LabelledExtractList ---(Labelled_Extract_ID)--> LabelledExtract

                           'link_fk_value'         'link_table_name'       'instance_fk_name'       'instance_table_name'
                            

    
      " get all of the Extracts listed in the ExtractList with the ID $link_fk_value "

       SELECT LabelledExtract.Name, LabelledExtract.ID FROM  LabelledExtractList, LabelledExtract 
       WHERE LabelledExtractList.ID=$link_fk_value
         AND  LabelledExtractList.Labelled_Extract_ID = LabelledExtract.ID
       ORDER BY LabelledExtractList.Index;

    */

    public Instance[] retrieveInstanceList( String link_fk_value, 
					    String link_table_name, 
					    String instance_fk_name, 
					    String instance_table_name )
    {
	Vector inst_v = new Vector();

	String sql = 
	    "SELECT "+ 
	    qTableDotField( instance_table_name, "Name") + "," + 
	    qTableDotField( instance_table_name, "ID" ) +
	    " FROM " + 
	    qTable( link_table_name ) + "," +
	    qTable( instance_table_name ) +
	    " WHERE " + 
	    qTableDotField( link_table_name, "ID") + "=" +  
	    qID(link_fk_value) + 
	    " AND " + 
	    qTableDotField( link_table_name, instance_fk_name ) + "=" +  
	    qTableDotField( instance_table_name, "ID" ) +
	    " ORDER BY " + 
	    qTableDotField( link_table_name, "Index" );

	if(cntrl.debug_all_sql)
	{
	    System.out.println("retrieveInstanceList()");
	    System.out.println( sql );
	}

	ResultSet rs = executeQuery(sql);
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    Instance linked_inst = new Instance( rs.getString(1), rs.getString(2) );
		    
		    // System.out.println( "...." + linked_inst.name + " ( " + linked_inst.id + ")" );

		    inst_v.addElement( linked_inst );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("SQL error whilst try to retrieve InstanceLists\n\n" + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}

	return (Instance[]) inst_v.toArray( new Instance[0] );
    }


    /*
      this  version is slightly different; it used when the instances in the list do not have names.
       (at the moment; only the 'Property' table fits into this category.)
       
       the 'normal' version returns an array of Instance, this version the IDs as an array of Strings
    */

    public String[] retrieveAnonymousInstanceList( String link_fk_value, 
						   String link_table_name, 
						   String instance_fk_name, 
						   String instance_table_name )
    {
	Vector inst_id_v = new Vector();

	String sql = 
	    "SELECT "+ 
	    qTableDotField( instance_table_name, "ID" ) +
	    " FROM " + 
	    qTable( link_table_name ) + "," +
	    qTable( instance_table_name ) +
	    " WHERE " + 
	    qTableDotField( link_table_name, "ID") + "=" +  
	    qID(link_fk_value) + 
	    " AND " + 
	    qTableDotField( link_table_name, instance_fk_name ) + "=" +  
	    qTableDotField( instance_table_name, "ID" ) +
	    " ORDER BY " + 
	    qTableDotField( link_table_name, "Index" );

	if(cntrl.debug_all_sql)
	{
	    System.out.println("retrieveInstanceList()");
	    System.out.println( sql );
	}

	ResultSet rs = executeQuery(sql);
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    // System.out.println( "...." + linked_inst.name + " ( " + linked_inst.id + ")" );

		    inst_id_v.addElement( rs.getString(1) );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("SQL error whilst try to retrieve InstanceLists\n\n" + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}

	return (String[]) inst_id_v.toArray( new String[0] );
    }


  /*
    for example:
        " get all of the LabelledExtractLists with contain the specified LabelledExtract $lex_id "

       SELECT DISTINCT LabelledExtractList.Name, LabelledExtractList.ID FROM  LabelledExtractList, LabelledExtract 
       WHERE LabelledExtractList.Labelled_Extract_ID=$lex_id

           instance_id     = $ex_id
	   instance_table  = LabelledExtract
           link_fk_field   = Labelled_Extract_ID
           link_table_name = LabelledExtractList

	 *NOTE*  that the Instances that are returned do not have 'name' data
          
    */

  public Instance[] retrieveInstanceListsContainingSomeInstance( String instance_id,
								 String link_fk_field, 
								 String link_table_name )
    {
	Vector inst_v = new Vector();

	String sql = 
	    "SELECT DISTINCT " + 
	    qField( "ID" ) +
	    " FROM " + 
	    qTable( link_table_name ) + 
	    " WHERE " + 
	    qField( link_fk_field ) + "=" +  
	    qID(instance_id);

	if(cntrl.debug_all_sql)
	{
	    System.out.println("retrieveInstanceListsContainingSomeInstance()");
	    System.out.println( sql );
	}

	ResultSet rs = executeQuery(sql);
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    Instance inst = new Instance( "[no-name]", rs.getString(1) );
		    
		    inst_v.addElement( inst );
		}
	    }
	    catch(SQLException sqle)
	    {
		System.err.println("SQL error whilst try to retrieve InstanceLists\n\n" + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}

	return (Instance[]) inst_v.toArray( new Instance[0] );
    }


    //  e.g. "ProbeList", "Probe_ID", [ probe_inst_1, probe_inst_2, ... ]
    //
    public String createInstanceList( String list_table_name, String link_fk_name, Instance[] list_contents )
    {
	if( list_contents == null )
	{
	    System.err.println( "createInstanceList(): unable to create an empty list");
	    return null;
	}
	if( list_contents.length == 0 )
	{
	    System.err.println( "createInstanceList(): unable to create an empty list");
	    return null;
	}

	String list_id = generateUniqueID( list_table_name, "ID");
	

	if( list_id == null )
	{
	    System.err.println( "createInstanceList(): unable to create new '" + list_table_name + "'" );
	    return null;
	}

	final String sql_hdr = 
	    "INSERT INTO " + qTable( list_table_name ) + " (" + 
	    qField( "ID" ) + "," + 
	    qField( "Index" ) + "," + 
	    qField( link_fk_name ) + 
	    ") VALUES (" + 
	    qID( list_id ) + ",";
	    

	for(int i=0; i < list_contents.length; i++)
	{
	    if( list_contents[i].id != null )
	    {
		String sql = sql_hdr + qID( String.valueOf( i ) ) +  "," + qID( list_contents[i].id ) + " )";
		
		if(cntrl.debug_all_sql)
		    System.out.println(sql);
		
		if(executeStatement(sql) == false)
		{
		    System.err.println( "createInstanceList(): unable to create new '" + list_table_name + "'" );
		    cntrl.alertMessage( "Unable to create new " + list_table_name );
		    return null;
		} 
	    }
	    else
	    {
		System.err.println( "createInstanceList(): WARNING: null instance not put in list");
	    }
	}
	
	return list_id;
    }

    public String createInstanceList( String list_table_name, String link_fk_name, Instance list_contents )
    {
	Instance[] inst_a = new Instance[1];
	inst_a[0] = list_contents;
	return createInstanceList( list_table_name, link_fk_name, inst_a );
    }


    // e.g. "ProbeList", probe_list_id, "Probe", "Probe_ID", probe_id );
    //
    // if the specified instance_id is already in the list, it is not added again
    //
    // returns the number of instances in the list after the new thing has been added
    //
    //    
    //
    public int addToInstanceList( String list_table_name, String list_id, 
				  String link_table_name, String link_fk_name, 
				  String instance_id )
    {
	// retrieve the current instance list

	Instance[] current_list = retrieveInstanceList( list_id, list_table_name, link_fk_name, link_table_name );

	int new_index_i;

	if(( current_list == null ) || ( current_list.length == 0 ))
	{
	    // list exists but is empty
	    
	    // not a problem....

	    new_index_i = 0;
	}
	else
	{
	    // check whether the instance is already in the list
	    
	    for(int i=0; i < current_list.length; i++)
		if( current_list[ i ].id.equals( instance_id ) )
		    return current_list.length;

	    new_index_i =  current_list.length;
	}

	// add the new instance to the end of the list
	
	final String new_index = String.valueOf( new_index_i );
	
	final String sql = 
	    "INSERT INTO " + qTable( list_table_name ) + " (" + 
	    qField( "ID" ) + "," + 
	    qField( "Index" ) + "," + 
	    qField( link_fk_name ) + 
	    ") VALUES (" + 
	    qID( list_id ) + "," + 
	    qID( new_index ) +  "," + 
	    qID( instance_id ) + " )";
	
	if(cntrl.debug_all_sql)
	    System.out.println(sql);
	
	if(executeStatement(sql) == false)
	{
	    System.err.println( "addToInstanceList(): unable to append new instance to '" + list_table_name + "'" );
	    //cntrl.alertMessage( "Unable to append new instance to " + list_table_name );
	    return -1;
	} 
	
	return ( new_index_i + 1 );
    }


    // e.g. "ProbeList", probe_list_id, "Probe", "Probe_ID", [probe_id1,probe_id2,probe_id3,....] );
    //
    // if any of the the specified instance_id's are already in the list, it is not added again
    //
    // returns the number of instances in the list after the new thing has been added
    //
    public int addToInstanceList( String list_table_name, String list_id, 
				  String link_table_name, String link_fk_name, 
				  Instance[] instances_to_add )
    {
	// retrieve the current instance list

	Instance[] current_list = retrieveInstanceList( list_id, list_table_name, link_fk_name, link_table_name );

	java.util.HashSet ids_in_list = new java.util.HashSet();
	
	int next_index = 0;

	if( current_list != null )
	{
	    for(int i=0; i < current_list.length; i++)
	    {
		ids_in_list.add( current_list[ i ].id );
	    }

	    next_index = current_list.length;
	}

	if(  instances_to_add != null )
	{
	    for( int i = 0; i < instances_to_add.length; i++ )
	    {
		// make sure it's not already in the list
		if( ids_in_list.contains( instances_to_add[ i ].id ) == false )
		{
		    // add it to the list

		    //System.out.println( "addToInstanceList(): adding ID " + instances_to_add[ i ].id + " with index " + next_index );
			
		    final String sql = 
			"INSERT INTO " + qTable( list_table_name ) + " (" + 
			qField( "ID" ) + "," + 
			qField( "Index" ) + "," + 
			qField( link_fk_name ) + 
			") VALUES (" + 
			qID( list_id ) + "," + 
			qID( String.valueOf( next_index ) ) +  "," + 
			qID( instances_to_add[ i ].id ) + " )";
		    
		    if(cntrl.debug_all_sql)
			System.out.println(sql);
		    
		    if(executeStatement(sql) == false)
		    {
			System.err.println( "addToInstanceList(): unable to append new instance to '" + list_table_name + "'" );
			//cntrl.alertMessage( "Unable to append new instance to " + list_table_name );
			return -1;
		    } 
		    
		    next_index++;
		}
	    }
	}
	    
	return next_index;
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public boolean stringsMatch(String s1, String s2)
    {
	if(s1 == null)
	{
	    return (s2 == null);
	}
	else
	{
	    if(s2 == null)
		return false;
	    return s1.equals(s2);
	}
    }
    public boolean descriptionMatches(String d_id, String desc)
    {
	String src_desc = getDescription(d_id);
	
	// System.out.println("\ndescriptionMatches(): testing\n1. '" + src_desc + "'\n2. '" + desc + "'\n");

	if((src_desc != null) && (src_desc.length() == 0))
	    src_desc = null;

	return stringsMatch(src_desc, desc);
    }

    // matches name's to id's (and any other pair of columns)
    //
    public String getMatch(String c1, String s1, String c2, String table)
    {
	ResultSet rs = executeQuery("SELECT " + qField(c2) + " FROM " + qTable(table) +
				    " WHERE " + qField(c1) + " = " + s1);
	String s2 = new String("");

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    String str = rs.getString(1);
		    if(str != null)
		    {
			s2 = str;
		    }
		    //System.out.println("getMatch(): max id in " + column_name + " is " + max_id);
		}
	    }
	    catch(SQLException sqle)
	    {
		cntrl.alertMessage("getMatch(): " + sqle);
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	else
	    cntrl.alertMessage("getMatch(): table missing?");  
	
	return s2;
    }

    
    //
    // converts \" into ", \' into ' and \\ into \
    //
    // everything else is untouched ( including \; )
    //
    public final String untidyText( String str )
    {
	if(str == null)
	    return null;

	if( debug_untidy )
	    System.out.println("untidyText()  src=" + str );

	StringBuffer result = new StringBuffer( str.length() );

	String result_s = null;

	int c = 0;

	//boolean escaped = false;

	// special handling for the '' which is used by Oracle to mean '

	// quick check to see if we are going to have to do any work
	// on the string and exit early if not
	//
	int check = str.indexOf('\\');

	int escape_count = 0;
    
	if( check >= 0 ) 
	{
	    
	    final int str_len = str.length();
	    
	    while( c < str_len )
	    {
		final char ch = str.charAt(c);
		
		switch( ch )
		{
		    case '\\':
			escape_count++;

			//
			// e.g. converting  \\n to \n  
			//
			//    the first \ is not escaped, but the second one is
			//
			
			if( ( escape_count % 2 ) == 1 )
			{
			    result.append(ch);
			}
			break;

		    case '\'':
		    case '\"':
			if( ( escape_count % 2 ) == 1 )
			{
			    // this " or ' was escaped,
			    // replace the current 'last' character 
			    // (which will be an escape char)
			    // with the " or ' char
			    
			    result.setCharAt( result.length() - 1, ch );
			}
			else
			{
			    // not escaped, add it as normal
			    result.append(ch);
			}
			escape_count = 0;
			break;

		    default:
			escape_count = 0;
			result.append(ch);
			break;
		}
		
		c++;
	    }

	    result_s = result.toString();
	}
	else
	{
	    result_s = str;
	}

	if( ! normal_escape_mode_for_single_quotes )
	{
	    if( debug_untidy )
		System.out.println( "untidyText(): before '' translation, src=" + result_s );

	    result_s = globalReplaceSubstring(  result_s, "''", "'" );
	}
	
	//System.out.println( "untidyText():\n   in=" + str + "\n  out=" + result.toString() );

	return result_s;
    }

    // do a single pass over the string and replace all occurences of 'search' with 'replace'
    private final String globalReplaceSubstring( final String src, final String search, final String replace )
    {
	// firstly, check for any occurences of the search string...
	//
	int index = src.indexOf( search );

	// nothng found? nothing to do then
	//
	if( index < 0 )
	    return src;

	String result = src;

	final int search_len = search.length();

	int len = result.length();

	while( index < len )
	{
	    if( result.substring( index ).startsWith( search ) ) 
	    {
		result = result.substring( 0, index ) + replace + result.substring( index + search_len );
		index += search_len;
	    }
	    else
	    {
		index++;
	    }

	    len = result.length();
	}

	return result;
    }

    private final boolean isEscaped( final String src, final int pos )
    {
	//  for example, \x is escaped, but \\x is not escaped, neither is \\\\x but \\\x is.

	// scan backwards to find the longest unbroken string of escape chars before the pos
	// if this turns out to be an even number of chars, the characters at pos is not actually escaped

	int len = 0;
	int epos = pos - 1;

	while( ( epos >= 0 ) && ( src.charAt( epos ) == '\\' ) )
	{
	    len++;
	    epos--;
	}


	//System.out.println( "isEscaped(): test char '" + src.charAt( pos ) + "' at position " + pos + " is " + 
	//		    (( ( len % 2 ) == 1 ) ? "escaped" : "not escaped" ) + "( preceeded by " + len + "\\'s)" );
		    
	
	return( ( len % 2 ) == 1 );
    }


    //
    // converts " into \", ' into \' and \ into \\
    //
    // as of 2.0.9 no longer checks for \n or \t as they will be handled by the unicode conversion
    //
    public final String tidyText( final String str )
    {
	if( str == null )
	    return null;
	
	final String trim_str = str.trim();  

	if( trim_str.length() == 0 )
	    return null;

	boolean even_slash = false;            // have we seen an even or an odd number of slashes?
	boolean last_char_was_slash = false;

	// firstly, scan the string and determine whether any characters need to be escaped
	//
	boolean needs_escape = false;

	int c = 0;

	final int trimed_length = trim_str.length();

	while( ( c < trimed_length ) && ( needs_escape == false ) )
	{
	    char ch = trim_str.charAt( c++ );

	    if( ( ch == '\\' ) || ( ch == '\'' ) || ( ch == '\"' )  )
		needs_escape = true;
	}


	if( needs_escape == false )
	    return trim_str;


	// if we are here, then one or more characters need escaping
	//

	StringBuffer result = new StringBuffer( trimed_length );

	c = 0;

	while( c < trimed_length )
	{
	    final char ch = trim_str.charAt( c );
	    
	    switch(ch)
	    {
	    case '\\':
	    case  '"':
		result.append( '\\' );
		result.append( ch );
		break;
		
	    case '\'':
		if( normal_escape_mode_for_single_quotes )
		{
		    result.append("\\\'");
		}
		else
		{
		    result.append("\'\'");
		}
		break;
		
	    default:
		result.append(ch);
	    }
	    

	    // last_char_was_not_an_escape = ( ch != '\\' );

	    c++;
	}
	
	// System.out.println( "tidyText():\n   in=" + str + "\n  out=" + result.toString() );
	
	return result.toString();
    }


    // ===================================================================================


    static final boolean debug_untidy = false;

    public final void testTextEscaping( )
    {
	final String[] test = { "this is 'quoted'", 
				"another \"quote\" test", 
				"an \\\"escaped quote\\\" test", 
				"one \\\\ and // two",
				"a futher \\\\\"escaped quote\\\\\" test", 
				"the same, with single \\\\\'escaped quotes\\\\\'", 
				"this is already \'escaped\'", 
				"this is also \\\"escaped\\\"", 
				"and this has both / and \\ and even \tA\tTAB!", 
				"but this \\' contains \\' \'s",
				"but this \\; is the legendary problem semicolon",
				"so does this \\ but they are \\ escaped \\",
				"single \\, double \\\\, triple \\\\, quad \\\\\\\\, quint \\\\\\\\\\.",
				"and what about \n carriage returns (\\n) ?" };

	for(int t=0; t < test.length; t++ )
	{
	    String tidied = tidyText( escapeUnicode( test[ t ] ) );
	    String untidied = unescapeUnicode( untidyText( tidied ) );
	    //String untidied = untidyText( unescapeUnicode( tidied ) );

	    if( untidied.equals( test[ t ] ) == false )
	    {
		System.out.println("     raw=[" + test[ t ] + "]" );
		System.out.println("  tidied=[" + tidied + "]" );
		System.out.println("untidied=[" + untidied + "]" );

		System.exit(-1);
	    }
	    else
	    {
		//System.out.println("well tidied=[" + tidied + "]" );
	    }
	}

	System.out.println("All character escaping tests passed");
    }

    //
    // the follow versions of untidyText, tidyText, globalReplaceSubstring, isEscaped
    // have been replaced by the revised versions above in maxdLoad2.0.9
    //

/*   
    //
    // expand any escaped chars into their internal representation
    //
    public String untidyText(String str)
    {
	if(str == null)
	    return null;

	//System.out.println("untidyText()  src=" + str );

	StringBuffer result = new StringBuffer(str.length());
	
	int c = 0;

	//boolean escaped = false;

	// special handling for the '' which is used by Oracle to mean '

	if( ! normal_escape_mode_for_single_quotes )
	{
	    str = globalReplaceSubstring(  str, "''", "'" );
	}
	
	final int str_len = str.length();

	while( c < str_len )
	{
	    final char ch = str.charAt(c);
	    
	    switch( ch )
	    {
	    case '\\':
	    case '\'':
	    case '\"':
	    case '\n':
	    case '\t':
		if( isEscaped( str, c ) == false )
		{
		    result.append(ch);
		}
		else
		{
		}
		break;
	    default:
		 result.append(ch);
		 break;
	    }

	    c++;
	}

	//System.out.println( "untidyText():\n   in=" + str + "\n  out=" + result.toString() );

	return result.toString();
    }

	
    // do a single pass over the string and replace all occurences of 'search' with 'replace'
    private String globalReplaceSubstring( final String src, final String search, final String replace )
    {
	String result = src;

	int index = 0;

	final int search_len = search.length();

	int len = result.length();

	while( index < len )
	{
	    if( result.substring( index ).startsWith( search ) ) 
	    {
		result = result.substring( 0, index ) + replace + result.substring( index + search_len );
		index += search_len;
	    }
	    else
	    {
		index++;
	    }

	    len = result.length();
	}

	return result;
    }

    private boolean isEscaped( final String src, final int pos )
    {
	//  for example, \x is escaped, but \\x is not escaped, neither is \\\\x but \\\x is.

	// scan backwards to find the longest unbroken string of escape chars before the pos
	// if this turns out to be an even number of chars, the characters at pos is not actually escaped

	int len = 0;
	int epos = pos - 1;

	while( ( epos >= 0 ) && ( src.charAt( epos ) == '\\' ) )
	{
	    len++;
	    epos--;
	}


	//System.out.println( "isEscaped(): test char '" + src.charAt( pos ) + "' at position " + pos + " is " + 
	//		    (( ( len % 2 ) == 1 ) ? "escaped" : "not escaped" ) + "( preceeded by " + len + "\\'s)" );
		    
	
	return( ( len % 2 ) == 1 );
    }


    public void testTextEscaping()
    {
	final String[] test = { "this is 'quoted'", 
				"this is already \'escaped\'", 
				"this \\ contains \\ \'s",
				"but this \\ \' contains \\ \' \'s",
				"so does this \\ but they are \\ escaped \\",
				"and what about \n carriage returns (\\n) ?" };

	for(int t=0; t < test.length; t++ )
	{
	    String tidied = tidyText( test[ t ] );
	    String untidied = untidyText( tidied );

	    if( tidied.equals( untidied ) == false )
	    {
		System.out.println("     raw=" + test[ t ] );
		System.out.println("  tidied=" + tidied );
		System.out.println("untidied=" + untidied );
	    }
	}
    }


    //
    // remove any illegal characters from the string
    // by escaping them with a '\'
    //
    //
    // note: when this code is used for storing AttrDescs, there may already be
    //       some escaped characters in the string, anything which is special
    //       to the AttrDesc format ( i.e. ';' and '=' ) will have been escaped
    //
    //       e.g   thing1=val1;thing2=partA\;partB\;partC
    //
    //       this code should escape these escapes so that they will be preserved
    //       when the string is subsequently retrieved
    //
    //       e.g   thing1=val1;thing2=partA\\;partB\\;partC    [this is what is stored]
    //
    //       there might also be escape characters used innocently in the strings too,
    //     
    //       e.g   the factional value is 2\3
    //
    //       and these should also be escaped
    //
    public final String tidyText( final String str )
    {
	if( str == null )
	    return null;
	
	String trim_str = str.trim();  

	if( trim_str.length() == 0 )
	    return null;

	boolean even_slash = false;            // have we seen an even or an odd number of slashes?
	boolean last_char_was_slash = false;

	// firstly, scan the string and determine whether any characters need to be escaped
	//
	boolean needs_escape = false;

	int c = 0;

	final int trimed_length = trim_str.length();

	while( ( c < trimed_length ) && ( needs_escape == false ) )
	{
	    char ch = trim_str.charAt( c++ );

	    if( ( ch == '\\' ) || ( ch == '\'' ) || ( ch == '\"' ) || ( ch == '\n' ) || ( ch == '\t' ) )
		needs_escape = true;
	}


	if( needs_escape == false )
	    return trim_str;


	// if we are here, then one or more characters need escaping
	//

	// have to be careful that the string isn't already tidied
	// ??????

	//String unescaped_str = untidyText(trim_sstr);
	
	//StringBuffer result = new StringBuffer( unescaped_str.length() );

	StringBuffer result = new StringBuffer( trimed_length );

	c = 0;

	while( c < trimed_length )
	{
	    final char ch = trim_str.charAt( c );
	    
	    switch(ch)
	    {
	    case '\\':
	    case  '"':
		//case '\n':
		//case '\t':
		result.append( '\\' );
		result.append( ch );
		break;
		
	    case '\'':
		if( normal_escape_mode_for_single_quotes )
		{
		    result.append("\\\'");
		}
		else
		{
		    result.append("\'\'");
		}
		break;
		
	    default:
		result.append(ch);
	    }
	    

	    // last_char_was_not_an_escape = ( ch != '\\' );

	    c++;
	}
	
	// System.out.println( "tidyText():\n   in=" + str + "\n  out=" + result.toString() );
	
	return result.toString();
    }
*/

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

/*
    public AttrDesc getAttrDesc( String table_name )
    {
	if( is_version_two )
	{
	    if( table_name_to_attr_desc_ht == null )
	    {
		if( getAttrDescs() == false )
		{
		    return null;
		}
	    }
	    
	    if( table_name_to_attr_desc_ht == null )
	    {
		System.err.println( "WARNING: no attributes are defined for '" + table_name + "'" );

		return null;
	    }
	    
	    AttrDesc attr_desc_for_table = (AttrDesc) table_name_to_attr_desc_ht.get ( table_name );
	    
	    if(  attr_desc_for_table == null )
		// there are no AttrDescs for this table
		return null;
	    else
		return  attr_desc_for_table;

	}
	else
	{
	    return null;
	}
    }
*/
/*
    public boolean getAttrDescs()
    {
	return getAttrDescs( new AttrDescIO( cntrl ) );
    }
*/
/*
    public boolean getAttrDescs( AttrDescIO adio )
    {
	if( maxd_properties.attr_desc_xml == null )
	{
	    table_name_to_attr_desc_ht = new java.util.Hashtable ();
	 
	    System.err.println("WARNING: no attributes are defined in this version " + maxd_properties.database_version + " database");
	}
	else
	{
	    try
	    {
		// each table name is mapped to a single AttrDesc ( which is typically of type AttrDesc.Group )

		table_name_to_attr_desc_ht = adio.loadAttrDescs( maxd_properties.attr_desc_xml );
	    }
	    catch(  AttrDescIO.ParseException pe )
	    {
		String message = pe.toString();

		System.out.println("getAttrDescs(): parse error: '" + pe.toString() + "'" );

		cntrl.alertMessage( message );
		    
		return false;

	    }
	}
	return true;
    }
*/

    private java.util.Hashtable table_name_to_attr_desc_ht = null;
    
/*   
    public boolean updateAttrDescXML( String new_xml_str )
    {
	// System.out.println("updateAttrDescXML()\n  " + new_xml_str );

	// get the current AttributeID (if any)

	String get_id_sql = "SELECT " + qField("Attr_Desc_Description_ID") + " FROM " + qTable("maxdProperties");
	
	String id = null;
	
	ResultSet rs = executeQuery(get_id_sql);
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    id = rs.getString( 1 );
		}
	    }
	    catch(SQLException sqle)
	    {
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}

	if( id == null )
	{
	    // generate a new id and store it
	    id = generateDescriptionID( new_xml_str );

	    System.out.println( "Creating a new description ID...." );

	    String store_attr_desc_sql = 
		"UPDATE " + qTable("maxdProperties") + " SET " + 
		qField("Attr_Desc_Description_ID") + " = " + qID(id);
	    
	    if( executeStatement( store_attr_desc_sql ) == false )
	    {
		System.err.println("Unable to store description text");
		
		return false;
	    }
	}
	else
	{
	    // update the text of the existing id
	    if ( updateDescription( id, new_xml_str ) == false )
	    {
		System.err.println("Unable to update description text");
		
		return false;
	    }
	}

	// now re-create the AttrDesc information

	maxd_properties.attr_desc_xml = new_xml_str;

	return ( getAttrDescs() == true );

    }
    */

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public int total_bytes_sent = 0;
    public int total_queries_made = 0;
    public int total_inserts_made = 0;
    
    public synchronized ResultSet executeQuery(String sql)
    {
	if((sql == null) || (sql.length() == 0))
	   return null;
	

	try
	{
	    // System.out.println("executeQuery(): trying to exec:\n  " + sql);
	    
	    Statement stmt = connection.createStatement();
	    stmt.setEscapeProcessing(false);
	    ResultSet rs = stmt.executeQuery(sql);

	    total_bytes_sent += sql.length();
	    total_queries_made++;
	    
	    //System.out.println("executeQuery(): ResultSet retrieved ok");
	    
	    /*
	      ResultSetMetaData rsmd = rs.getMetaData();
	      
	      int cc = rsmd.getColumnCount();
	      System.out.println("               " + cc + " columns");
	      for(int ccc=0;ccc<cc;ccc++)
	      System.out.println("                " + rsmd.getColumnName(ccc+1));
	    */
	    
	    // TODO:
	    // keep track of all open ResultSets and the SQL that was used
	    // to create them, so 'cursor leaks' can be detected
	    //
	    // associate all ResultSets with the Statements that created them....

	    //  still_open_rs.add( rs, sql );
	    
	    return rs;
	}
	catch(SQLException sqle)
	{
	    String emesg = (sqle.toString().length() > 256) ? sqle.toString().substring(0,255) : sqle.toString();
	    String sql_short = (sql.length() > 256) ? sql.substring(0,255) : sql;
	    
	    cntrl.alertMessage("executeQuery(): Unable to execute SQL query:\n  '" + sql_short + "'\nerror: " + emesg);
	    
	}
	catch(Exception ex)
	{
	    System.err.println("** Unexpected exception in Controller.executeQuery() **\n" + ex);
	}

	return null;
    }

    public synchronized boolean executeStatement(String sql)
    {
	return executeStatement( sql, true );
    }
    
    public synchronized boolean executeStatement(String sql, boolean report_errors )
    {
	if((sql == null) || (sql.length() == 0))
	   return true;

	{
	    try
	    {
	        // System.out.println("executeStatement(): trying to exec:\n  " + sql);
	      
		Statement stmt = connection.createStatement();
		stmt.setEscapeProcessing(false);
		stmt.executeUpdate(sql);

		total_bytes_sent += sql.length();
		total_inserts_made++;

		if( ( keep_sql_log == true ) && ( log_file != null ) )
		{
		    try
		    {
			log_file_writer.write(sql);
		    }
		    catch(IOException ioe)
		    {
			if(first_failure_of_log_file)
			{
			    cntrl.alertMessage("executeQuery(): Unable to write SQL to log file");
			    first_failure_of_log_file = false;
			}
		    }
		}

		// 'interbase' docs suggest that explictly finalizing the 
		// statement object is a "good idea"

		stmt.close();
		stmt = null;

		return true;
	    }
	    catch(SQLException sqle)
	    {
		if( report_errors )
		{
		    String sql_short = (sql.length() > 256) ? sql.substring(0,255) : sql;
		    String emesg = (sqle.toString().length() > 256) ? sqle.toString().substring(0,255) : sqle.toString();
		    
		    cntrl.alertMessage("executeQuery(): Unable to execute SQL update:\n  '" + sql_short + "'\nerror: " + emesg);
		}
	    }

	    return false;
	}
    }

    // TODO: could keep a table of all currently not-closed ResultSets for cntrl.debugging
    //
    //       could also test for multiple closings of the same ResultSet (/)
    //

    private boolean already_warned_about_result_set = false;

    public final void closeResultSet( final ResultSet rs )
    {
	if(rs != null)
	{
	    try
	    {
		Statement stmt = null;

		try
		{
		    stmt = rs.getStatement();
		}
		catch( Exception ex) // for example:  org.postgresql.Driver.notImplemented 
		{
		}
		catch( Error ex) // AbstractMethodError
		{
		}
 
		rs.close();

		if(stmt != null)
		    stmt.close();

		//  still_open_rs.remove( rs );
	    }
	    catch(SQLException sqle)
	    {
		if(!already_warned_about_result_set)
		{
		    already_warned_about_result_set = true;
		    System.err.println("warning: unable to close ResultSet");
		}
	    }
	}
    }


    public synchronized void disconnect()
    {
	performDatabaseSpecificEpilogue( getHost() );

	try
	{
	    if( connection != null )
		connection.close();
	}
	catch(SQLException sqle)
	{
	    cntrl.errorMessage("Unable to close connection");
	}
    }

    public synchronized int attemptConnect( String host, 
					    String driver_file, String driver_name, 
					    String account_name, String password )
    {
	boolean connected = false;

	String current_connection_url =  host;

	Class dc = null;


	/*
	   things that could possibly go wrong:

	     database:

	       database not specified
	       network not reachable
	       host name/ip doesn't exist
	       database name doesn't exist

	     driver file:

	       driver file not specified
	       file/directory doesn't exist
	       file/directory not readable
	       
	     driver name:

	       driver name not specified
	       not found in driver file
	       driver not loadable
	       driver not a class
	       driver not initialisable
	       
             user name/password

	       required, but not provided
	       user name wrong
	       password wrong



	*/


	// ============================================================
	// some sanity checking on the host, driver file and driver name params
	// ============================================================

	cntrl.getConnectionManager().reportStatus("Starting to connect...");


	if((current_connection_url == null) || (current_connection_url.length() == 0))
	{
	    cntrl.getConnectionManager().reportStatus("....failed (no 'Database' specfied)");

	    cntrl.alertMessage("Unable to connect: No 'Database' has been specified.");

	    return -1;
	}

	if((driver_name == null) || (driver_name.length() == 0))
	{
	    cntrl.getConnectionManager().reportStatus("....failed (no 'Driver Name' specfied)");

	    cntrl.alertMessage("Unable to connect: No 'Driver Name' has been specified.");

	    return -1;
	}

	if(cntrl.debug_connect)
	{
	    System.out.println("\n       host: '" + current_connection_url + "'");
	    System.out.println("driver file: '" + driver_file + "'");
	    System.out.println("driver name: '" + driver_name + "'");
	}

	
	// ============================================================
	// 
	// special driver/JARfile checking
	//


	cntrl.getConnectionManager().reportStatus("...seeking '" + driver_name + "'" );


	final boolean search_for_driver_name_in_jar_file = false;

	if(search_for_driver_name_in_jar_file)
	{
	    if((driver_file != null) && (driver_file.length() != 0))
	    {
		if(driver_file.toLowerCase().endsWith(".jar"))
		{
		    cntrl.getConnectionManager().reportStatus("....searching for the driver in JAR file");

		    CustomClassLoader ccl = new CustomClassLoader( getClass() );
		    
		    try
		    {
			String match = ccl.findBestMatchInJARFile( driver_name + ".class", driver_file );
			
			if(match == null)
			{
			    cntrl.getConnectionManager().reportStatus("...no match found");
			}
			else
			{
			    cntrl.getConnectionManager().reportStatus("...best match is '" + match + "'");
			    
			    if( !match.equals(driver_name + ".class") )
				cntrl.getConnectionManager().reportStatus("   (which is NOT an exact match)");
			}
		    }
		    catch(java.io.IOException ioe)
		    {
			cntrl.getConnectionManager().reportStatus("...unable to load JAR file (" + ioe + ")" );
		    }
		}
	    }
	}

	// ============================================================
	// first try the built-in class loader, which will only work if the JDBC driver
	// classes are in the CLASSPATH
	// ============================================================
	
	if(cntrl.debug_driver)
	{
	    cntrl.getConnectionManager().reportStatus("...loading driver from classpath");

	    String classpath = System.getProperty("java.class.path");
	    
	    if(classpath == null)
		System.err.println("...classpath is not set");
	    else
		System.err.println("...classpath is '" + classpath + "'");
	}

	try
	{
		    
	    dc = getClass().getClassLoader().loadClass( driver_name );

	    if(cntrl.debug_driver)
	    {
		if(dc != null)
		{
		    cntrl.getConnectionManager().reportStatus("...potential driver found in classpath");
		}
		else
		{
		    cntrl.getConnectionManager().reportStatus("...driver not found in classpath");
		}
	    }
	}

	catch(Exception e)
	{
	    if(cntrl.debug_driver)
	    {
		cntrl.getConnectionManager().reportStatus("...driver not found in classpath");
	    }
	}



	// ============================================================
	// 
	// if that has failed, try a URLClassLoader.....
	// 
	// as of 1.8.0.b2, java.net.URLClassLoader is used, much better all round
	// as it doesn't make such a mess of loading JAR files and it works
	// properly with the Oracle drivers
	// ============================================================

	if(dc == null)
	{
	    cntrl.getConnectionManager().reportStatus("...trying to load driver using URLClassLoader");

	    if((driver_file == null) || (driver_file.length() == 0))
	    {
		cntrl.getConnectionManager().reportStatus("...failed (no 'Driver File' specified)");
		    
		cntrl.alertMessage("Unable to connect:\n  No 'Driver File' has been specified (and the driver is not in the classpath)");

		return -1;
	    }


	    try
	    {
		String fname = driver_file;
		
		// append a trailing '\' if the file is a directory
		if(!fname.toLowerCase().endsWith(".jar") && !fname.toLowerCase().endsWith(".zip"))
		{
		    if(!fname.endsWith( File.separator ))
			fname += File.separator;
		}
		
		
		cntrl.getConnectionManager().reportStatus("...loading driver from '" + fname + "'...");

		// -------------------

		File test_file = new File( driver_file );
		String thing = test_file.isDirectory() ? "directory" : "file";
		    
		if(!test_file.exists())
		{
		    cntrl.getConnectionManager().reportStatus("...failed (" + thing + " does not exist)");
		    
		    cntrl.alertMessage("Unable to connect: 'Driver File' does not exist.");

		    return -1;
		}
		else
		{
		   
		    if(!test_file.canRead())
		    {
			cntrl.getConnectionManager().reportStatus("...failed (" + thing + " is not readable)");

			cntrl.alertMessage("Unable to connect: 'Driver File' is not readable.");
			
			return -1;
		    }
		    else
		    {
			if(cntrl.debug_driver)
			    cntrl.getConnectionManager().reportStatus("...this " + thing + " exists and is readable");
		    }
		}

		test_file = null;

		// -------------------

		//java.net.URL[] urls = new java.net.URL[1];

		//urls[0] = new java.net.URL( fname );

		System.out.println("seeking '" + driver_name + "' in '" + fname + "'" );

		maxdView.CustomClassLoader ccl = (maxdView.CustomClassLoader) getClass().getClassLoader();
	
		ccl.addPath( fname );

		if(cntrl.debug_driver)
		{
		    cntrl.getConnectionManager().reportStatus("maxdView.CustomClassLoader: attempting to find class '" + driver_name + "'");
		}

		dc = ccl.findClass( driver_name );
		
		if(dc != null)
		{
			cntrl.getConnectionManager().reportStatus("...found '" + driver_name + "'" );
		}
		else
		{
			cntrl.getConnectionManager().reportStatus("...'" + driver_name + "' not found" );
		}
	    }
	    catch( java.net.MalformedURLException murle )
	    {
		if(cntrl.debug_driver)
		    cntrl.getConnectionManager().reportStatus("...driver file not found");
	    }
	}
	
	// ==================================================================
	// as a fallback, the old-style CustomClassLoader can still be used
	// (the URLClassLoader doesn't appear to work on Linux with JDK1.2.2)
	// ==================================================================
	/*
	if(dc == null)
	{
	    cntrl.getConnectionManager().reportStatus("...trying to load driver using CustomClassLoader");

	    CustomClassLoader ccl = new CustomClassLoader( getClass() );

	    if(driver_file.toLowerCase().endsWith(".jar"))
		ccl.addClassSearchJarFile( driver_file );
	    else
		ccl.addClassSearchPath( driver_file );
	    
	    if(cntrl.debug_driver)
	    {
		cntrl.getConnectionManager().reportStatus("CustomClassLoader: attempting to find class '" + driver_name + "'");
	    }

	    dc = ccl.findClass( driver_name );
	    
	    if(dc != null)
	    {
		cntrl.getConnectionManager().reportStatus("...found '" + driver_name + "'" );
	    }
	    else
	    {
		cntrl.getConnectionManager().reportStatus("...'" + driver_name + "' not found" );
	    }
	    
	}
	*/

	if(dc == null)
	{
	    String msg = null;

	    File testfile = new File( driver_file );

	    if( testfile.exists() )
	    {
		if( testfile.canRead() )
		{
		    msg = "Unable to load the specified JDBC driver.\n\n" + 
			"Check that the 'Driver Name' and 'Driver File' have been specified correctly.";
		    if ( driver_file != null )
			msg += "\n\n(failed to load '" + driver_name + "' from '" + driver_file + "')";
		}
		else
		{
		    msg = "Unable to open the specified JDBC driver '" + driver_name + "'.\n";
		    if ( driver_file != null )
			msg += "\nThe file exists, but cannot be opened. Check that the file permissions allow this file to be read.";
		}
	    }
	    else
	    {
		msg = "The specified 'Driver File' does not exist.";
	    }

	    cntrl.getConnectionManager().reportStatus( msg );
	    cntrl.alertMessage( msg );

	    return -1;
	}
	
	// make an instance of this class
	java.sql.Driver driver = null;
	
	try
	{
	    driver = (java.sql.Driver) dc.newInstance();
	    
	    if(driver != null)
	    {
		java.sql.DriverManager.registerDriver(driver);
		
		cntrl.getConnectionManager().reportStatus( "...driver has been registered" );
	
		cntrl.getConnectionManager().reportStatus( "...driver is version " + driver.getMajorVersion() + 
							   "." +  driver.getMinorVersion() );;
		
	    }
	    else
	    {
		cntrl.getConnectionManager().reportStatus( "...unable to instantiate driver, reason unknown.");

		cntrl.alertMessage("Unable to instantiate driver, reason unknown.");

		return -1;
	    }
	    
	}
	catch(Exception e)
	{
	    cntrl.getConnectionManager().reportStatus("unable to instantiate driver:\n\n  " + e.toString() );

	    // alertMessage("Unable to instantiate JDBC Driver");

	    return -1;
	}
	

	// ============================================================
	// (new version ends)
	// ============================================================


	try
	{
	    boolean has_account_name = ((account_name != null) && (account_name.length() > 0));
	    boolean has_password = ((password != null) && (password.length() > 0));
	    
	    if(cntrl.debug_connect)
		cntrl.getConnectionManager().reportStatus("using " + (has_account_name ? "a name" : "no name") + 
							  " and " + (has_password ? "a password" : "no password"));
	    
	    cntrl.getConnectionManager().reportStatus( "...attempting to connect to database");

	    Properties acct_props = new Properties();
	    if(has_account_name)
		acct_props.setProperty("user", account_name);
	    if(has_password)
		acct_props.setProperty("password", password);
	    
	    connection = driver.connect( current_connection_url, acct_props) ;

	    if(connection != null)
	    {
		cntrl.getConnectionManager().reportStatus( "...connected ok");

		if(cntrl.debug_connect)
		    cntrl.getConnectionManager().reportStatus("connected to " + current_connection_url);
		
		connected = true;
		 
		host_name = host;

		performDatabaseSpecificPrologue( host_name );

		int result = getDatabaseProperties();
		
		if(result == 1) // no database found
		{
		    cntrl.alertMessage("This database appears to be empty.\n" + 
				       "Use 'maxdLoad2' to initialise it and load some data." );
		    return -1;
		}

		if(maxd_properties.database_version.startsWith( desired_version ) == false ) // wrong database version
		{
		    cntrl.alertMessage( "This database uses an out-of-data schema.\n" + 
					"Use 'maxdLoad2' to update it then try to connect again." );
		    return -1;
		}
		
	    }
	    else
	    {
		// this is because the wrong sort of driver has been used
		
		final String error_msg = 
		    "Unable to initiate connection.\n\n" + 
		    "This could be because there is an error in the 'Database' field\n" + 
		    "or the wrong JDBC driver is being used.";
		
		cntrl.getConnectionManager().reportStatus( "..." + error_msg );

		cntrl.alertMessage(error_msg);
	    }
	    
	}
/*
	catch( java.net.UnknownHostException uhe)
	{
		final String error_msg = 
		    "Unable to connect to the database server - the host name cannot be resolved.\n\n" + 
		    "Make sure that name of the machine that is hosting the database has been\n" + 
		    "specified correctly in the 'Database' field.";
		
		cntrl.getConnectionManager().reportStatus( "..." + error_msg );

		cntrl.alertMessage(error_msg);
	}
*/
	catch(java.sql.SQLException sqle)
	{
	    while(sqle != null)
	    {
		int e_number = sqle.getErrorCode();
		
		String emesg = sqle.getMessage();
		
		String info = "";
		
		if( emesg.indexOf("UnknownHostException") > 0 )
		    info = "The host name cannot be resolved.\n\n" + 
			"Make sure that name of the machine that is hosting the database has been" + 
			" specified correctly in the 'Database' field.\n\n";

		if((emesg.indexOf("invalid")>0) && (emesg.indexOf("arguments")>0))
		    info = "Username requires a password?\n\n";
		
		String report_msg = info + sqle + "\n\n(Error code: " + e_number + ")";

		cntrl.getConnectionManager().reportStatus( "...Unable to connect:\n " + report_msg );
		
		cntrl.alertMessage( "Unable to connect to the database.\n\n" + report_msg );
		
		sqle = sqle.getNextException();
	    }
	}
    
	if(connected)
	{
	    user_name = account_name;

	    return 1;	
	}
	else
	{
	    return -1;
	}
    }
    


    public class maxdProperties
    {
	public String database_url;

	public String database_real_name;   // the 'official' name of the databse

	public String driver_path;
	public String driver_name;
	
	public String database_name;         // the 'user-friendly' name of the databse
	public String database_version;     
	public String database_location;     // user-friendly description of location
	public String database_is_copy_of;   // is this db a mirror?
	public String database_copied_date; 

	public String attr_desc_xml;         // the XML describing the attributes supported in this database

	public int identifier_len;

	public Hashtable translations;

	public maxdProperties() 
	{ 
	    translations = new Hashtable(); 
	}
	
    }

    public maxdProperties maxd_properties;

    public int getMaxIdentifierLength()
    {
	return maxd_properties.identifier_len;
    }

    public maxdProperties getProperties( ) { return maxd_properties; }

    // ------------------------------------------------------------------------------------------
    //
    // is there a maxdSQL database on the current host?
    //

    public boolean checkDatabaseExists( String name )
    {
	// attempt to connect to this database
	
	String sql = "CONNECT " + name ;
	
	return (executeStatement(sql) == false);
	
    }

    // ------------------------------------------------------------------------------------------

    private boolean workOutQuoteMode()
    {
	for(int tmode=0; tmode < 3; tmode++)
	    for(int fmode=0; fmode < 3; fmode++)
	    {
		quote_table_mode = tmode;
		quote_field_mode = fmode;
		
		String test_sql = "SELECT " + qField("Database_Name") + " FROM " + qTable("maxdProperties");

		if(cntrl.debug_login)
		    System.out.println("check: " + test_sql);

		try
		{
		    Statement stmt = connection.createStatement();
		    // stmt.setEscapeProcessing(false);
		    ResultSet rs = stmt.executeQuery(test_sql);
		    
		    // ResultSet rs = executeQuery(test_sql);

		    if(cntrl.debug_login)
			System.out.println("passed: " + test_sql);

		    // result data itself is not needed...

		    try
		    {
			if(rs != null)
			    rs.close();
			if(stmt != null)
			    stmt.close();
		    }
		    catch( Exception ex )
		    {
			if(cntrl.debug_login)
			    System.out.println("cannot close? " + ex);
		    }

		    //stmt.close();

		    return true;
		}
		catch(SQLException sqle)
		{
		    if(cntrl.debug_login)
			System.out.println("failed: " + test_sql + "\n\n(" + sqle + ")\n");
		}
	    }
	
	return false;
    }
    
 
    // ------------------------------------------------------------------------------------------

    // return codes: 0=database ok,  1=database not found, 2=wrong database version

    private int getDatabaseProperties()
    {
	// attempt to automatically figure out the quoting convention
	// 
	// (mainly because some versions of MyMSQL are non-ansi compliant)
	//
	boolean found = false;

	if(cntrl.debug_login)
	    System.out.println("\ngetDatabaseProperties() starts");

	maxd_properties = new maxdProperties();
	
	maxd_properties.database_url = cntrl.getPref("database.host");

	if(!workOutQuoteMode())
	{
	    return 1;
	}
	
	//
	// get then name and check the version number is ok
	//

	String get_props_v1_sql = 
	    "SELECT " + 
	    qField("Database_Name") + ", " + 
	    qField("Database_maxd_Version") + ", " + 
	    qField("Database_Location") + ", " + 
	    qField("Is_Copy_Of") + ", " + qField("Copied_Date") + ", " + 
	    qField("Identifer_Len") +     // NOTE this misspelling of Identif(i)er
	    " FROM " + qTable("maxdProperties");

	ResultSet rs = executeQuery( get_props_v1_sql );
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    //int pos = 1;

		    if(rs.getString(1) != null)
		    {
			maxd_properties.database_name  = new String( rs.getString(1) );

			//System.out.println("Name:" + maxd_properties.database_name);
		    }
		    else
		    {
			cntrl.alertMessage("Unable to determine maxd database name.\n\n" + 
					   "This version of maxdLoad can only connect to version '" +  desired_version + "' databases");
			return 2;
		    }
		    
		    String version_str = rs.getString(2);

		    if( ( version_str != null ) && ( version_str.length() > 0 ) )
		    {
			if(cntrl.debug_login)
			    System.out.println("getDatabaseProperties() version_str=" + version_str );

			setDatabaseVersion ( version_str );
			
			if(cntrl.debug_login)
			    System.out.println("getDatabaseProperties() version:" + maxd_properties.database_version + 
					       "\n                       desired:" + desired_version );
			
			// we don't want to stop at this point, otherwise the translations won't be read in
			// and it will be impossible to do an updateDatabase if that is neccessary

			/*
			if(maxd_properties.database_version.startsWith( desired_version ) == false )
			{
			    cntrl.alertMessage("Database is version '" + maxd_properties.database_version 
					       + "'.\n\nConnections can only be made to version '" +  desired_version + "' databases.");
			    return 2;
			}
			*/
		    }
		    else
		    {
			cntrl.alertMessage("Unable to determine maxd database version.");
			return 2;
		    }
		    
		    if(rs.getString(3) != null)
		    {
			maxd_properties.database_location = new String(rs.getString(3));
			//System.out.println("Location:" + maxd_properties.database_location);
		    }		
		    
		    if(rs.getString(4) != null)
		    {
			maxd_properties.database_is_copy_of = new String(rs.getString(4));
			//System.out.println("Is Copy Of:" + maxd_properties.database_is_copy_of);
		    }
		    if(rs.getString(5) != null)
		    {
			maxd_properties.database_copied_date = new String(rs.getString(5));
			//System.out.println("Copied Date:" + maxd_properties.database_copied_date);
		    }
		    try
		    {
			if(rs.getString(6) != null)
			{
			    maxd_properties.identifier_len = (Integer.valueOf(rs.getString(6))).intValue();
			}
/*
			if(rs.getString(7) != null)
			{
			    maxd_properties.long_text_len = (Integer.valueOf(rs.getString(7))).intValue();
			}
*/
		    }
		    catch(NumberFormatException nfe)
		    {
			cntrl.alertMessage("Warning: unable to determine maximum field sizes\nIs this really a maxdSQL 1.2 database");
		    }
		}
	    }
	    catch(java.sql.SQLException sqle)
	    {
		cntrl.alertMessage("Unable to find database properties. Is this really a maxd database?");
	    }
	    finally
	    {
		closeResultSet(rs);
	    }
	}
	else
	{
	    cntrl.alertMessage("Unable to get database information.\nHas a maxdSQL database been set up on this host?");
	    return 1;
	}

	
	readTranslations();

	//
	// get the 'offical' quoting conventions for the database
	//
	String get_quotes_sql = 
	"SELECT " + 
	qField("Table_Quote_Mode") + ", " + qField("Field_Quote_Mode") + ", " + 
	qField("Text_Quote_Mode") + ", " + qField("ID_Quote_Mode") + 
	" FROM " + qTable("maxdProperties");
	
	rs = executeQuery(get_quotes_sql);
	    
	try
	{
	    while (rs.next()) 
	    {
		quote_table_mode = parseQuoteName(rs.getString(1));
		quote_field_mode = parseQuoteName(rs.getString(2));
		quote_text_mode  = parseQuoteName(rs.getString(3));
		quote_id_mode    = parseQuoteName(rs.getString(4));

		/*
		System.out.println("Quoting conventions for this database");
		System.out.println("Table: " + getQuoteName(quote_table_mode));
		System.out.println("Field: " + getQuoteName(quote_field_mode));
		System.out.println(" Text: " + getQuoteName(quote_text_mode));
		System.out.println("   ID: " + getQuoteName(quote_id_mode));
		*/
	    }
	}
	catch(java.sql.SQLException sqle)
	{
	    cntrl.alertMessage("Unable to find database properties. Is this really a maxd database?");
	}
	finally
	{
	    closeResultSet(rs);
	}


	
	if( is_version_two )
	{
	    //System.out.println( "getDatabaseProperties() getting Attr Desc XML" );
	
	    String attr_desc_url_description_id = null;
			
	    String get_v2_props_sql = "SELECT " + qField("Attr_Desc_Description_ID") +  " FROM " + qTable("maxdProperties");
	    
	    rs = executeQuery( get_v2_props_sql );
	
	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			attr_desc_url_description_id  = rs.getString(1);
		    }
		}
		catch(java.sql.SQLException sqle)
		{
		    cntrl.alertMessage("Unable to load the attribute definitions. Is this really a maxd version 2 database?");
		    return 2;
		}
		finally
		{
		    closeResultSet(rs);
		}
	    }

	    if( attr_desc_url_description_id != null )
	    {
		maxd_properties.attr_desc_xml = getDescription( attr_desc_url_description_id );

		//System.out.println( "getDatabaseProperties() [initialiser] = " + maxd_properties.attr_desc_xml );
	    }
	    else
	    {
		cntrl.infoMessage( "No attribute definitions exist in this database" );
	    }
	}
	else
	{
	    maxd_properties.attr_desc_xml = null;
	}


	// fill in the maximum lengths using the 2 values from the maxdProps
	
	max_number_length = 15;

	max_general_name_length = maxd_properties.identifier_len;
	max_gene_name_length = maxd_properties.identifier_len;
	max_spot_name_length = maxd_properties.identifier_len;
	max_probe_name_length = maxd_properties.identifier_len;
	
	//max_long_text_len = maxd_properties.long_text_len;
	
	//max_contact_email_length = maxd_properties.long_text_len;
	//max_contact_address_length = maxd_properties.long_text_len;
	
	//max_publication_name_length = maxd_properties.long_text_len;
	
	//max_identifier_database_id_length =  maxd_properties.identifier_len;
	//max_identifier_entry_id_length =  maxd_properties.identifier_len;
	
	//max_property_name_length  = max_long_text_len;
	//max_property_value_length = max_long_text_len;

//	max_property_unit_length = 10;

	return 0;
    }

    public void readTranslations()
    {
	//
	// get any translations that are used by this database
	//
	
	String get_names_sql = 
	"SELECT " + qField("maxd_Name") + ", " + qField("Actually_Called") + " FROM " + qTable("maxdTranslations");

	ResultSet rs = executeQuery(get_names_sql);
	    
	try
	{
	    while (rs.next()) 
	    {
		if((rs.getString(1) != null) && (rs.getString(2) != null))
		{
		    maxd_properties.translations.put(rs.getString(1), rs.getString(2));

		    //System.out.println("Translate: " + rs.getString(1) + " to " +  rs.getString(2));
		}
	    }
	}
	catch(java.sql.SQLException sqle)
	{
	}
	finally
	{
	    closeResultSet(rs);
	}

    }
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public void performDatabaseSpecificPrologue( String db_ident_string )
    {
	normal_escape_mode_for_single_quotes = true;

	if( db_ident_string.startsWith( "jdbc:oracle" ))
	{
	    normal_escape_mode_for_single_quotes = false;
	}
    }
    
    public void performDatabaseSpecificEpilogue( String db_ident_string )
    {
	normal_escape_mode_for_single_quotes = true;
	
	/*
	  if( db_ident_string.startsWith( "jdbc:oracle" ))
	  {
	  executeStatement("set escape on");
	  
	  System.out.println("oracle mode: escaping is OFF");
	  }
	*/
    }

    //
    // special handling of different quoting conventions
    //

    public final static int QuoteNone = 0;
    public final static int QuoteSingle = 1;
    public final static int QuoteDouble = 2;

    private final char[] quote_char = { ' ', '\'', '\"' };

    // variable depending on database
    private int quote_table_mode = QuoteDouble;
    private int	quote_field_mode = QuoteNone;
    private int	quote_text_mode  = QuoteSingle;
    private int	quote_id_mode  = QuoteSingle;     // assume IDs are VARCHARS not INTEGERS

    private boolean normal_escape_mode_for_single_quotes = true;

    // convert name to symbol
    private final int parseQuoteName(String name)
    {
	if(name == null)
	    return QuoteNone;
	if(name.equals("single"))
	    return QuoteSingle;
	if(name.equals("double"))
	    return QuoteDouble;
	return QuoteNone;	
    }
    // convert symbol to name
    private final String getQuoteName(int sym)
    {
	final String[] names = { "none", "single", "double" };
	return names[sym];
    }

    public final String translate(String name)
    {
	try
	{
	    String t_name = (String) maxd_properties.translations.get(name);
	    return (t_name == null) ? name : t_name;
	}
	catch(java.lang.NullPointerException npe)
	{
	    return name;
	}
    }

    public final String qTable(String name)
    {
	return quote_char[quote_table_mode] + translate(name) + quote_char[quote_table_mode];
    }

    public final String qField(String name)
    {
	return quote_char[quote_field_mode] + translate(name) + quote_char[quote_field_mode];
    }
    
    public final String qTableDotField(String tname, String fname)
    {
	return (quote_char[quote_table_mode] + translate(tname) + quote_char[quote_table_mode] + 
		"." +
	        quote_char[quote_field_mode] + translate(fname) + quote_char[quote_field_mode]);
    }

    public final String qText(String name)
    {
	// why isn't the 'name' passed through tidyText() ?
	return quote_char[quote_text_mode] + name + quote_char[quote_text_mode];
    }

    public final String qID(String id)
    {
	return quote_char[quote_id_mode] + id + quote_char[quote_id_mode];
    }

    public final String qDescID(String id)
    {
	return id;
    }

    public String index_field_name = new String("Index");

    public void setQuoteModes( int table, int field, int text, int id )
    {
	quote_table_mode = table;
	quote_field_mode = field;
	quote_text_mode  = text;
	quote_id_mode    = id;
    }

    public void setDatabaseVersion( String database_version )
    {
	maxd_properties.database_version = database_version;

	is_version_one = is_version_two = false;
	
	if( maxd_properties.database_version != null )
	{
	    if(maxd_properties.database_version.startsWith("1."))
	    {
		is_version_one = true;
	    }
	    if(maxd_properties.database_version.startsWith("2."))
	    {
		is_version_two = true;
	    }
	}
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private String log_file_name;
    private File log_file = null;
    private FileWriter log_file_writer = null;
    private boolean first_failure_of_log_file;

    private void openLogFile()
    {
	first_failure_of_log_file = true;

	try
	{
	    // generate a new file name for the log files
	    int index = 0;

	    boolean already_used = true;

	    while(already_used)
	    {
		index++;
		already_used = false;
		
		log_file_name = new String("maxdLoad.log." + index);

		File test = new File(log_file_name);
		if(test.exists())
		    already_used = true;
	    }

	    // System.out.println("using " + log_file_name + " as log file name");
	   
	    log_file = new File(log_file_name);
	    log_file_writer = new FileWriter(log_file);
	}
	catch(IOException ioe)
	{
	    log_file = null;
	    keep_sql_log = false;
	}
	catch(sun.applet.AppletSecurityException ase)
	{
	    log_file = null;
	    keep_sql_log = false;
	}
    }

    private void closeLogFile()
    {
      try
      {
	if(log_file != null)
	{
	  log_file_writer.close();
	  if(log_file.length() == 0)
	  {
	    //System.out.println("nothing in log file, deleting it");
	    log_file.delete();
	  }
	}
      }
      catch(IOException ioe)
	{
	  log_file = null;
	}
      catch(sun.applet.AppletSecurityException ase)
	{
	  log_file = null;
	}
    }
    
    public void enableLog( boolean enable ) 
    { 
	keep_sql_log = enable; 

	
    }

    private boolean keep_sql_log;

    // ===================================================================================================
    // ===================================================================================================
    // ===================================================================================================


    private Controller cntrl;

    private String host_name;
    private String user_name;

    private String desired_version;

    private boolean is_version_one, is_version_two;

    private java.sql.Connection connection;
    //
    // these values are determine by the attributes of maxdProperties
    // (which are loaded after connection is established)
    //

    public int max_general_name_length;
    public int max_general_id_length;
	   
    public int max_number_length = 15; // fixed by SQL INTEGER 
	   
    public int max_gene_name_length;
    public int max_spot_name_length;
    public int max_probe_name_length;
	   
//    public int max_long_text_len;
	   
//    public int max_contact_email_length;
//    public int max_contact_address_length;
	   
//    public int max_publication_name_length;
	   
//    public int max_identifier_database_id_length;
//    public int max_identifier_entry_id_length;
	   
    public final int max_text_property_chunk_length = 255;  // fixed

//    public int max_property_name_length;
//    public int max_property_value_length;
//    public int max_property_unit_length = 10;         // fixed

}

