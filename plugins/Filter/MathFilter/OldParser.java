import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.io.*;

public class Parser
{
    //
    //
    // ===================================================================================================
    //
    //

    public static void main( String args[] )
    {
	String str = ( (args != null) && (args.length==1 ) )? args[0] : "1 + 2 * 3 + 4" ;

	Parser my_parser =  new Parser(  );

/*
	my_parser.addVariable( "e1", 1 );
	my_parser.addVariable( "e2", 2 );
	my_parser.addVariable( "e3", 3 );
	my_parser.addVariable( "e4", 4 );
*/

	try
	{
	    TermNode root = my_parser.parse( str );
	    
	    System.out.println( root.toString() );
	}
	catch( ParseError pe )
	{
	    System.err.println( pe );
	}
    }

    //
    //
    // ===================================================================================================
    //
    //

    //
    //  grammar:
    //
    //
    //    TermNode           :=   Identifier  |  Function  |  UnaryTermNode  |  BinaryTermNode
    //
    //    Function           :=   ['mean' | 'max' | 'min' | etc... ]   "("   TermNode  [ "," TermNode ]*   ")"
    //	    
    //    UnaryTermNode      :=   UnaryOperator  Operand
    //
    //    BinaryTermNode     :=   Operand  BinaryOperator  Operand
    //	

    //
    //
    // ===================================================================================================
    //
    //


    public static class ParseError extends Exception
    {
	public ParseError( String s_ )
	{
	    s = s_;
	}

	public String toString() { return s; }

	private String s;
    }


    private final boolean debug_parse = false;


    //
    // ===================================================================================================
    //


    public Parser()
    {
    }

    public void setBinaryOperatorSymbols( String[] symbols )
    {
	binary_operator_symbols = symbols;
    }
    public void setUnaryOperatorSymbols( String[] symbols )
    {
	unary_operator_symbols = symbols;
    }
    public void setFunctionSymbols( String[] symbols )
    {
	function_symbols = symbols;
    }

    public void setIdentifiers( String[] symbols )
    {
	// note that the identifiers are sorted by length so the longest one is seen first.
	// this is to ensure that "abc.attr1" is detected as a single entity (rather than "abc" and ".attr1")
	//
	java.util.Arrays.sort( symbols, new SortByLengthComparator() );

	identifiers = symbols;
    }

    public TermNode parse( String input ) throws ParseError
    {
	final String[] all_symbols = generateSymbolList();

	final Vector tokens = extractTokens( input, all_symbols );

	final TermNode root = convertTokensToTermNode( tokens, all_symbols );
	    
	return root;
    }

    // the gammer symbols are fixed:

    final String[] grammar_symbols = { "(", ")", ",", " " };
    
    // default collection of symbols (which can be changed by calling the set...Symbols() methods)

    private String[] binary_operator_symbols = { "*", "/", "+", "-", ">=", "<=", "==", "!=", ">", "<", "&", "|" };  // note precedence order
    private String[] unary_operator_symbols  = { "-", "!"  }; 
    private String[] function_symbols        = { "min", "max" };

    private String[] identifiers;

    //
    //
    // --------------------------------------------------------------------------------------------------
    //
    //

    private final String[] generateSymbolList()
    {
	// compose a full list of possible symbols in the order in which they
	// should be detected
	
	Vector all_symbols_v = new Vector();
	
	addSymbols( all_symbols_v, grammar_symbols );
	addSymbols( all_symbols_v, binary_operator_symbols );
	addSymbols( all_symbols_v, unary_operator_symbols );
	addSymbols( all_symbols_v, function_symbols );
	
	return (String[]) all_symbols_v.toArray( new String[ all_symbols_v.size() ] );
    } 


    private void addSymbols( final Vector vec, final String[] strs )
    {
	for(int s=0; s < strs.length; s++)
	    vec.add( strs[ s ] );
    }

    private class SortByLengthComparator implements java.util.Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    return ((String)o2).length() - ((String)o1).length();
	}
	public boolean equals(Object o) { return false; }
    }

    //
    // ===================================================================================================
    //


    private class Token
    {
	public static final int Identifier           = 0;
	public static final int OpenBracket          = 1;
	public static final int CloseBracket         = 2;
	public static final int BinaryOperator       = 3;
	public static final int UnaryOperator        = 4;
	public static final int FunctionOperator     = 5;
	public static final int ArgumentSeparator    = 6;

	public final String[] token_type_names = 
	{ 
	    "Identifier", 
	    "OpenBracket", 
	    "CloseBracket", 
	    "BinaryOperator", 
	    "UnaryOperator", 
	    "FunctionOperator", 
	    "ArgumentSeparator" 
	};

	public int    type;   // what sort of token is this?
	
	public String string;    // the text associated with this token

	public int    str_pos;  // the position of the start of this token in the original text

	public Token( int t, String s, int p )
	{
	    type = t;
	    string  = s;
	    str_pos = p;
	}
	
	public Token( final String s, final int p )
	{
	    type = getType(s);
	    string = s;
	    str_pos = p;
	}

	public boolean isOperator()
	{
	    return ( ( type == BinaryOperator ) ||
		     ( type == UnaryOperator ) ||
		     ( type == FunctionOperator ) );
	}

	public String toString()
	{
	    //return string + " [" + token_type_names[ type ] + "]";
	    return string;
	}
	
	private int getType( String tok_s )
	{
	    if(tok_s.equals("("))
		return OpenBracket;

 	    if(tok_s.equals(")"))
		return CloseBracket;
	    	    
 	    if(tok_s.equals(","))
		return ArgumentSeparator;

	    for(int sym=0; sym < binary_operator_symbols.length; sym++)
	    {
		if( tok_s.equals( binary_operator_symbols[ sym ] ) )
		{
		    return BinaryOperator;
		}
	    }

	    for(int sym=0; sym < unary_operator_symbols.length; sym++)
	    {
		if( tok_s.equals( unary_operator_symbols[ sym ] ) )
		{
		    return UnaryOperator;
		}
	    }

	    for(int sym=0; sym < function_symbols.length; sym++)
	    {
		if( tok_s.equals( function_symbols[ sym ] ) )
		{
		    return FunctionOperator;
		}
	    }

	     // otherwise it must be an identifier of some flavour (i.e. a variable or a constant)

	    return Identifier;
	}
    }
	


    // =================================================================================================


    private Vector extractTokens(String str, String[] all_symbols )
    {
	// create an initial input consisting of the source String
	Vector things = new Vector();
	things.add( str );

	extractTokens( things, all_symbols );
	
//	showThings( things );

	return things;
    }


    //
    // 'things' is a mixture of String's and Token's
    //
    private void extractTokens( final Vector things, final String[] all_symbols )
    {
	// how many String objects remain?

	int n_strings = 0;

	for(int t=0; t < things.size(); t++)
	{
	    if ( things.elementAt( t ) instanceof String )
		n_strings++;
	}

	if( n_strings == 0 )
	    // everything has been converted to tokens, we have finished
	    return;

	// find the longest String 
	// (or the first joint longest String if several Strings are joint longest)

	int longest = 0;
	for(int t=0; t < things.size(); t++)
	{
	    if ( things.elementAt( t ) instanceof String )
	    {
		String str = (String) things.elementAt( t );
		if( str.length() > longest )
		    longest = str.length();
	    }
	}
	
	// extract the 'best' token from this String
	for(int t=0; t < things.size(); t++)
	{
	    if ( things.elementAt( t ) instanceof String )
	    {
		String str = (String) things.elementAt( t );

		if( str.length() == longest )
		{
		    replaceThingWithStringTokenString( things, t, all_symbols );
		}
	    }
	}	

	// and continue recursively...

	if( debug_parse )
	    showThings( things );


	extractTokens( things, all_symbols );
    }

    private void replaceThingWithStringTokenString( final Vector things, final int index, final String[] all_symbols )
    {
	String target = (String) things.elementAt( index );
	
	//
	// look for any of the known identifiers first
	// (so that identifiers which contain symbols, for example "Data=1", will be detected
	//
	if( identifiers != null )
	{
	    for(int i=0; i < identifiers.length; i++)
	    {
		// check for both quoted and unquoted versions of the identifier

		String test = "\"" + identifiers[ i ] + "\"";
		int pos = target.indexOf( test );

		if( pos < 0 )
		{
		    test = identifiers[ i ];
		    pos = target.indexOf( test );
		}

		if( pos >= 0 )
		{
		    Vector split = split( target, pos, test );
		    
		    things.removeElementAt( index );
		    things.addAll( index, split );
		    
		    return;
		}
	    }
	}

	//
	// now look for any of the symbols
	//
	for(int s=0; s < all_symbols.length; s++)
	{
	    int pos = unquotedPositionOf( target, all_symbols[ s ], 0 );

	    if( pos >= 0 )
	    {
		Vector split = split( target, pos, all_symbols[ s ] );
		
		things.removeElementAt( index );
		things.addAll( index, split );
		
		return;
	    }
	}

	//
	// not a symbol or a known identifiers, it must be an unrecognised constant, a variable, or an empty string
	//
	things.removeElementAt( index );
	
	// check for empty string
	String trimmed = target.trim();

	if( trimmed.length() > 0 )
	{
	    things.add( index, new Token( trimmed, -1 ) );
	}
    }


    // find the first location of 'seek' within 'string', but make sure that
    // 'seek' doesn't occur within a quoted section of 'string'
    //
    private int unquotedPositionOf( final String string, final String seek, final int offset )
    {
	int pos = string.indexOf( seek, offset );

	if( pos < 0 )
	    return -1;

	// count how many "s occur before pos...

	int n_quote_chars = 0;

	for(int c=0; c < pos; c++)
	{
	    if( string.charAt( c ) == '\"' )
	    {
		// make sure the " wasn't escaped...

		if( ( c == 0 ) || ( string.charAt( c-1 ) != '\\' ) )
		{
		    n_quote_chars++;
		}
	    }
	}

	// if there were an odd number of "s, then 'seek' occurs within quoted text and should be ignored

	if( ( n_quote_chars % 2 ) == 1 )
	{
	    // check again, further along in the string....
	    return unquotedPositionOf( string, seek, offset + seek.length() );
	}
	else
	{
	    // it's a winner...
	    return pos;
	}
    }


    // convert the input String into a Vector containing a prefix String, a Token and a suffix String
    // be removing the substring 'extract' which occurs at character index 'pos'
    //
    // the prefix and/or the suffix can be empty, in which case they should not be added to the output Vector
    //
    // the Token should be generated using the contents of 'extract'
    //
    private Vector split( final String input, final int pos, final String extract )
    {
	Vector result = new Vector();
	
	if( pos > 0 )
	{
	    String prefix = input.substring( 0, pos );
	    result.add( prefix );
	}
	
	if( extract.trim().length() > 0 )
	{
	    Token token = new Token( extract, -1 ); // dont actually have the original position, so use -1
	    result.add( token );
	}

	if(( pos + extract.length() ) < input.length() )
	{
	    String suffix = input.substring( pos + extract.length() );
	    result.add( suffix );
	}

	return result;
    }


    // ========================================================


    private void showThings( Vector things )
    {
	showThings( things, 0 );
    }

    private void showThings( Vector things, int start )
    {
	for(int t=start; t < things.size(); t++)
	{
	    if( things.elementAt( t ) instanceof String )
	    {
		System.out.print( "[" + things.elementAt( t ) + "] " );
	    }
	    else
	    {
		if( things.elementAt( t ) instanceof Token )
		{
		    Token tok = (Token) things.elementAt( t );
		    System.out.print( "<" + tok.string + "> " );
		}
		else
		{
		    if( things.elementAt( t ) instanceof TermNode )
		    {
			TermNode tn = (TermNode) things.elementAt( t );
			System.out.print( "{" + tn.toString() + "} " );
		    }
		}
	    }
	}
	System.out.println();
    }



    //
    //
    // ===================================================================================================
    //
    //



    public abstract class TermNode
    {
	public abstract String toString();
    }

    public class IdentifierTermNode extends TermNode
    {
	public String identifier;    

	public String toString() { return identifier; }

	public IdentifierTermNode( String i ) { identifier = i; }
    }

/*
    public class VariableTermNode extends TermNode
    {
	public String variable_name;

	public String toString() { return variable_name; }

	public VariableTermNode( String vn ) { variable_name = vn; }
    }
*/

    public class BinaryOperatorTermNode extends TermNode
    {
	public String   operator;
	public TermNode left_operand, right_operand;

	public String toString() 
	{ 
	    return "(" + left_operand + operator + right_operand + ")";
	} 

	public BinaryOperatorTermNode( String o, TermNode l, TermNode r )
	{
	    operator = o; left_operand = l; right_operand = r;
	}

    }

    public class UnaryOperatorTermNode extends TermNode
    {
	public String   operator;
	public TermNode operand;

	public String toString() 
	{ 
	    return "(" + operator + operand + ")";
	}
	public UnaryOperatorTermNode( String o, TermNode tn )
	{
	    operator = o; operand = tn;
	}
    }

    public class FunctionTermNode extends TermNode
    {
	public String     function_name;
	public TermNode[] argument;         // can be null

	public FunctionTermNode( String f, TermNode[] a )
	{
	    function_name = f;
	    argument = a;
	}

	public String toString() 
	{ 
	    StringBuffer sbuf = new StringBuffer();

	    sbuf.append( function_name );
	    sbuf.append("(");
	    
	    if( argument != null )
		for( int a=0; a < argument.length; a++ )
		{
		    if( a > 0 )
			sbuf.append( "," );
		    sbuf.append( argument[ a ].toString() );
		}

	    sbuf.append(")");

	    return sbuf.toString();
	} 
    }


    //
    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
    //

    
    private TermNode convertTokensToTermNode( final Vector tokens, final String[] all_symbols ) throws ParseError
    {
	// find the best TermNode that can be constructed
	//
	// (or the first joint bestest TermNode if several TermNode are joint best)


	
	//
	// patterns to search for (in order of preference):
	//
        //
	//     TermNode
        //
	//     TermNode ArithmeticOperator TermNode         (i.e. binary math operator)
        //
	//     !TermNode ArithmeticOperator TermNode        (i.e. unary math operator)
	//
	//     TermNode EqualityOperator TermNode
        //
	//     FunctionOperator '(' [ TermNode [ ',' TermNode ] ] ')'
	//
	//     '(' TermNode ')' 
	//

	Vector things = tokens;

	int limiter = 0;

	if( debug_parse )
	    System.out.println( "Parser: begins...");
				    
	while( someTokensRemain( things ) )
	{
	    while( identifyIdentifierTermNodes( things ) );

	    while( identifyFunctionTermNodes( things ) );
	    
	    while( identifyUnaryOperatorTermNodes( things ) );
	    
	    while( identifyBinaryOperatorTermNodes( things ) );
	    
	    if( ++limiter > 8 )
		break;
	}

	if( debug_parse )
	    System.out.println( "Parser: ...end");

	if( things.size() == 1 )
	{
	    // looks good
	    if( things.elementAt( 0 ) instanceof TermNode )
		return (TermNode) things.elementAt( 0 );
	    else
		throw new ParseError( "Unexpected '" + things.elementAt( 0 ) + "'" );
	}
	if( things.size() > 1 )
	{
	    // hmmm.. something wasn't parsed correctly
	    throw new ParseError( "Unexpected '" + things.elementAt( 1 ) + "'" );
	}
	else
	{
	    throw new ParseError( "Nothing could be parsed from input.");
	}
    }


    private boolean someTokensRemain( Vector things )
    {
	// how many ParseTreeNode objects remain?
	
	int n_toks = 0;

	for(int t=0; t < things.size(); t++)
	{
	    if ( things.elementAt( t ) instanceof Token )
		n_toks++;
	}

	if( debug_parse )
	{
	    if( n_toks > 0 )
	    {
		System.out.println("  (" + n_toks + " ParseTreeNodes remaining....)" );
	    }
	    
	}

	return ( n_toks > 0 );
    }


    private boolean removeRedundantBrackets( Vector things )
    {
	//System.out.println( "Parser.removeRedundantBrackets()....");
	
        // identify the pattern '(' TermNode ')' and replace it with TermNode unless 
	// the pattern is preceded by a function name

	for(int t=0; t < things.size(); t++)
	{
	    if( (t + 2) < things.size() )
	    {
		if( things.elementAt( t ) instanceof Token )
		{
		    Token token = (Token) things.elementAt( t );
		    
		    if( token.type == Token.OpenBracket )
		    {
			// check this isn't the start of a function call
			boolean is_function = false;
			if( t > 0 )
			{
			   Token prev_token = (Token) things.elementAt( t - 1 );
			   for(int f=0; f < function_symbols.length; f++)
			       if( prev_token.string.equals( function_symbols[ f ] ) )
				   is_function = true;
			}
			
			if( ! is_function )
			{
			    if( things.elementAt( t + 1 ) instanceof TermNode )
			    {
				TermNode term_node = (TermNode) things.elementAt( t + 1 );
				
				if( things.elementAt( t + 2 ) instanceof Token )
				{
				    Token second_token = (Token) things.elementAt( t + 2 );
				    
				    if( second_token.type == Token.CloseBracket )
				    {
					// the target pattern has been found, do the replace action...
					
					replaceSubVector( things, t, 3, term_node );
					
					return true;
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	
	return false;
    }


    private boolean identifyIdentifierTermNodes( Vector things )
    {
	while( removeRedundantBrackets( things ) );
	
	//System.out.println( "Parser.identifyIdentifierTermNodes()....");
	
	boolean anything_found = false;

	for(int t=0; t < things.size(); t++)
	{
	    if( things.elementAt( t ) instanceof Token )
	    {
		Token token = (Token) things.elementAt( t );

		if( token.type == Token.Identifier )
		{
		    things.set( t, new IdentifierTermNode( token.string ) );

		    anything_found = true;
		}
	    }
	}

	return anything_found;
    }


    private boolean identifyBinaryOperatorTermNodes( Vector things )
    {
	while( removeRedundantBrackets( things ) );

	// identify the pattern TermNode ArithmeticOperator TermNode and replace it with TermNode

        // note that all possible re-writes must be done for each of the symbols in turn
	//      ( i.e. extracting all of the '*'s before any of the '+'s are touched )


	//System.out.println( "Parser.identifyBinaryOperatorTermNodes()....");
				    
	for(int a=0; a < binary_operator_symbols.length; a++) // consider operator precedence (i.e. '*' before '+')
	{
	    for(int t=0; t < things.size(); t++)
	    {
		if( things.elementAt( t ) instanceof Token )
		{
		    Token token = (Token) things.elementAt( t );

		    if( ( token.type == Token.UnaryOperator ) || ( token.type == Token.BinaryOperator ) )
		    {
			if( token.string.equals( binary_operator_symbols[ a ] ) )
			{
			    Object lhs = ( t > 0 )                 ? things.elementAt( t - 1 ) : null;
			    Object rhs = ( (t+1) < things.size() ) ? things.elementAt( t + 1 ) : null;
			    
			    if( ( lhs != null )&& ( rhs != null ) )
			    {
				if( ( lhs instanceof TermNode )  && ( rhs instanceof TermNode ) )
				{
				    // looks good for a BinaryOperator, create a new TermNode

				    //System.out.println( "Parser: extracting '" + binary_operator_symbols[ a ] + "'" );
				    //System.out.println( "   lhs=" + (TermNode) lhs );
				    //System.out.println( "   rhs=" + (TermNode) rhs );

				    replaceSubVector( things, t-1, 3, new BinaryOperatorTermNode( binary_operator_symbols[ a ], 
												  (TermNode) lhs, 
												  (TermNode) rhs ) );

				    //System.out.println( "Parser: result is '" + thingToString( things ) );

				    return true;
				}
			    }
			}
		    }
		}
	    }
	}	
	return false;
    }



    private boolean identifyUnaryOperatorTermNodes( Vector things )
    {
	while( removeRedundantBrackets( things ) );

	//System.out.println( "Parser.identifyUnaryOperatorTermNodes()....");

        //
	// identify the pattern ArithmeticOperator TermNode (when it occurs after a non-TermNode) and replace it with TermNode
	//

	for(int a=0; a < unary_operator_symbols.length; a++) // consider operator precedence (i.e. '*' before '+')
	{
	    for(int t=0; t < things.size(); t++)
	    {
		if( things.elementAt( t ) instanceof Token )
		{
		    Token token = (Token) things.elementAt( t );

		    if( ( token.type == Token.UnaryOperator ) || ( token.type == Token.BinaryOperator ) )
		    {
			if( token.string.equals( unary_operator_symbols[ a ] ) )
			{
			    Object lhs = ( t > 0 )                 ? things.elementAt( t - 1 ) : null;
			    Object rhs = ( (t+1) < things.size() ) ? things.elementAt( t + 1 ) : null;
			    
			    // can only be unary if the 'lhs' is not a TermNode (or it is null)
			    
			    if( ( lhs == null ) || ( ( lhs instanceof TermNode ) == false ) )
			    {
				if ( rhs instanceof TermNode )
				{
				    // looks good for a BinaryOperator, create a new TermNode
				    
				    replaceSubVector( things, t, 2, new UnaryOperatorTermNode( unary_operator_symbols[ a ], 
											       (TermNode) rhs ) );

				    return true;
				}
			    }
			}
		    }
		}
	    }
	}	
	return false;
    }



    private boolean identifyFunctionTermNodes( final Vector things ) throws ParseError
    {
	while( removeRedundantBrackets( things ) );

	//System.out.println( "Parser.identifyFunctionTermNodes()....");


	// identify the pattern FunctionOperator '(' [TermNode] [, TermNode ]*  ')' and replace it with TermNode
	
	Vector args_v = new Vector();

	for(int t=0; t < things.size(); t++)
	{
	    if( things.elementAt( t ) instanceof Token )
	    {
		Token token = (Token) things.elementAt( t );
		
		for(int f=0; f < function_symbols.length; f++)
		{
		    if( token.string.equals( function_symbols[ f ] ) )
		    {
			int things_consumed = identifyArguments( things, t+1, args_v );

			if( things_consumed > 0 )
			{
			    if( debug_parse )
				System.out.println( things_consumed + " things consumed in search for arg list for " + function_symbols[ f ] );

			    TermNode[] args_a = (TermNode[]) args_v.toArray( new TermNode[ args_v.size() ] );
			    
			    replaceSubVector( things, t, things_consumed + 1, new FunctionTermNode( function_symbols[ f ], args_a ) );

			    return true;
			}
		    }
		}
	    }
	}
	return false;
    }

    //
    // scan the things looking for the pattern '(' [TermNode] [, TermNode ]*  ')'
    // and return the length of the pattern if found, otherwise -1
    //
    // the collection of arguments that were identified is returned in 'args'
    //
    //
    //    examples of valid patterns:
    //
    //         '(' ')'
    //
    //         '('  TermNode  ')'
    //
    //         '('  TermNode  ',' TermNode  ')'
    //
    //         '('  TermNode  ','  TermNode  ','  TermNode  ')'
    //
    //

    private int identifyArguments( final Vector things, final int start_pos, final Vector args ) throws ParseError
    {
	int pos = start_pos;

	int state = 1;   // 1==expecting '(' ;  2==expecting arg or ')' ;  3==expecting ',' or ')'; 4==finished,not expecting anything ;

	args.removeAllElements();

	if( debug_parse )
	{
	    System.out.println("identifyArguments(): start pos is " + start_pos  + ", things are:");
	    showThings( things, start_pos );
	}

	while( pos < things.size() )
	{
	    Object thing = things.elementAt( pos++ );

	    int next_state = 0;

	    if( debug_parse )
		System.out.println("identifyArguments(): state is " + state );
		
	    if( thing instanceof Token )
	    {
		Token token = (Token) thing;
		
		if( debug_parse )
		    System.out.println("identifyArguments(): thing is a Token '" + token.toString() + "'");
		
		// it was the right thing, setup for the next thing
		
		switch( token.type )
		{
		case Token.OpenBracket:
		    if( state == 1 )
		    {
			if( debug_parse )
			    System.out.println("identifyArguments(): OK: '(' found");
			next_state = 2;
		    }
		    else
		    {
			if( debug_parse )
			    System.out.println("identifyArguments(): BAD: unexpected ')'");
			return -1;
		    }
		    break;

		case Token.CloseBracket:
		    if( ( state == 2 ) || ( state == 3 ) )
		    {
			if( debug_parse )
			    System.out.println("identifyArguments(): OK: ')' found");
			return pos - start_pos;
		    }
		    break;

		case Token.ArgumentSeparator:
		    if( state == 3 )
		    {
			if( debug_parse )
			    System.out.println("identifyArguments(): OK: ',' found");
			next_state = 2;
		    }
		    else
		    {
			if( debug_parse )
			    System.out.println("identifyArguments(): BAD: unexpected ','");
			return -1;
		    }
		    break;

		default:
		    // any other token is bad news
		    if( debug_parse )
			System.out.println("identifyArguments(): BAD: unexpected '" + token.string + "'");
		    return -1;
		}
	    }
	    if( thing instanceof TermNode )
	    {
		TermNode term_node = (TermNode) thing;

		if( debug_parse )
		    System.out.println("identifyArguments(): thing is a TermNode '" + term_node.toString() + "'");

		if( state == 2 )
		{
		    // ok, add the argument to the list

		    if( debug_parse )
			System.out.println("identifyArguments(): OK: '" + term_node + "' found");

		    args.add( term_node );

		    // setup for the next token

		    next_state = 3;
		}
		else
		{
		    // didn't expect this TermNode here

		    if( debug_parse )
			System.out.println("identifyArguments(): BAD: unexpected TermNode '" + term_node + "'");

		    return -1;
		}
	    }
	    
	    state = next_state;
	}

	// if we get here, then the list was either never started, or never terminated

	if( state == 1 )
	{
	    if( debug_parse )
		System.out.println("identifyArguments(): BAD: '(' not found");
	}
	else
	{
	    if( debug_parse )
		System.out.println("identifyArguments(): BAD: ')' not found");
	}

	return -1;
    }



    private final void replaceSubVector( final Vector vec, final int start, final int count, final Object new_thing )
    {
	for( int c=0; c < count; c++ )
	    vec.removeElementAt( start );
	vec.add( start, new_thing );
    }

    private final Vector getSubVector( Vector vec, int start, int end )
    {
	Vector result = new Vector();

	for( int i=start; i < end; i++ )
	    result.add( vec.elementAt( i ) );

	return result.size() > 0 ? result : null;
    }


    //
    //
    // ===================================================================================================
    //
    //

    // things contains either Tokens or TermNodes
    public String thingToString( Vector things )
    {
	StringBuffer sbuf = new StringBuffer();
	
	for(int t=0; t < things.size(); t++)
	{
	    if( things.elementAt( t ) instanceof Token )
	    {
		sbuf.append( " [ " +  ( (Token) things.elementAt( t ) ).string + " ] " );
	    }
	    else
	    {
		sbuf.append( " [ " +  ( (TermNode) things.elementAt( t ) ).toString() + " ] " );
	    }
	}

	return sbuf.toString();
    }

    //Hashtable variable_name_to_id_ht = new Hashtable();

}
