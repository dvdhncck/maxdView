#!/usr/bin/perl
#!C:\Program Files\Perl5\bin\perl
#

#
# process the output of 'javap'
#

$current_block_name = "";
$is_public = 0;

$debug = 0;
$line = 0;

while(<STDIN>)
{
    chop;

    $line++;

    tr/\t/\ /;

# detect start of class or interface

    if(( /\ class\ /) || ( /\ interface\ /))
    {
	$current_block_is_interface = ( /[^interface|\ interface]\ / ) ? 1 : 0;
	$current_block_is_public    = ( /[^public|\ public]\ / ) ? 1 : 0;

	$block_depth++;
	
	# extract class name
        # as all text between 'class' and either 'extends' or 'implements'
 
	@toks = split/\ /,$_;
	
	$in_class_name = 0;
	$class_name = "";

	$t = 0;
	while($t <= $#toks)
	{
	    if( $toks[$t]  =~ /extends/ )
	    {
		$in_class_name = 0;
	    }
	    if( $toks[$t]  =~ /implements/ )
	    {
		$in_class_name = 0;
	    }

	    if($in_class_name == 1)
	    {
		$class_name .= $toks[$t];
	    }

	    if( $toks[$t]  =~ /class/ )
	    {
		$in_class_name = 1;
	    }
	    if( $toks[$t]  =~ /interface/ )
	    {
		$in_class_name = 1;
	    }
	    $t++;
	}

	# put it on the end of the nest list
	@class_nest_list = ( @class_nest_list, $class_name );
	
	if($debug)
	{
	    print ($current_block_is_public ? "PUBLIC " : "nonPUBLIC ");
	    print ($current_block_is_interface ? "INTERFACE" : "CLASS");
	    print " '", $class_name, "'\n";
	    
	    print "nest list has ",  ($#class_nest_list + 1), " entries\n";
	    
	    print "nest list tail = ",  $class_nest_list[ $#class_nest_list ], "\n";
	    
	    print "currently in '" , $current_block_name, "'\n";

	    print "caused on line $line :  " , $_ ,"\n";
	}

	$current_block_name = $class_name;
	
	print "#START $class_name\n";
    }

# detect end of block char ( '}' which signifies end of class or interface )
    if( /\}/)
    {
	if($debug)
	{
	    print "BLOCK '", $current_block_name, "' ENDS ";
	}

	# get the enclosing class name from the nest list
	
	pop @class_nest_list;
	
	$current_block_name =  $class_nest_list[ $#class_nest_list] ;

	if($debug)
	{
	    print "(back in '", $current_block_name, "' )\n";
	}

	print "#DONE\n";
    }
   
# detect all public methods

    if( /\ *public.*\(/)
    {
	if($current_block_is_public)
	{
	
	    # extract method name
	    
	    $p = index $_,"\(";
	    
	    $method_signature = $_;
	    
	    $method_signature =~ s/^public\ //;
	    $method_signature =~ s/^final\ //;
	    
	    $method_signature =~ s/\ public\ //;
	    $method_signature =~ s/\ final\ //;
	    
	    $line = substr $_,0,$p;
	    
	    @toks = split/\ /,$line;
	    
	    $method_name = $toks[ $#toks ];

	    $method_type = $toks[ $#toks - 1 ];
	    $method_type =~ tr/\$/\./;

	    $method_args = substr $_,($p + 1);
	    $p = index $method_args, "\)";
	    $method_args = substr $method_args,0,$p;

	    # print "    ", $current_block_name, ".", $method_name, "\n";

	    print $method_name, "\t", $method_type, "\t", $method_args, "\n";
	}
    }

}
