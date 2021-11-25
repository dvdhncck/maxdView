
#
# the format of the index file:
#
#  1. prelude:    
# 
#  the first line is the number of files lines indexed  (call this N_FILES)
#  the next N_FILES lines are the names of the indexed files
#  (which specify the file numbers used in the data section below)
#  and the following N_FILES lines are the names of the documents
#  (extracted from the <TITLE>..</TITLE> tags
#
#  example prelude:
#
#     	  4
#     	  docs/Index.html
#     	  docs/demo.html
#     	  docs/test.html
#     	  docs/sample.html
#         maxdView: Index
#         maxdView Demonstration Document
#         A Test Page
#         A Sample Program
#
#    which says that 4 four files (numbered 0..3) have been indexed
#
#
#
#  2. data section
#
#  each line in the file lists the occurences of one word in one file
#  lines are sorted in alphabetical order of word
#
#
#  example line:
#
#         fish 2 3 45 49 130
#
#   this says that the word 'fish' occurs 3 times in file number 2
#
#   (the word positions are 45, 49 and 130 and file 2 is 'docs/test.html'
#    from the previous example)
#
#  (note that the word can be a number in which case the line
#   might be:
# 
#         7 4 1 78
#
#   which says that the word "7" occurs once (at position 78) in file 4 
#

$file_count = 0;

$min_word_length = 3;

sub search_all_in_directory
{
    local($some_dir) = @_;

    # print "checking '", $some_dir, "'\n";

    opendir(DIR, $some_dir) || die "can't opendir $some_dir: $!";

    my @names;
    my $prefix;
    my $d;
    my $fname;

    @names = readdir(DIR);

    $prefix = $some_dir;
    if( $prefix !~ /\/$/ )
    {
	$prefix = $some_dir . '/';
    }

    # print "checking: '", $some_dir, "', ", $#names, " names\n";

    for($d=0; $d <= $#names; $d++)
    {
	$fname = $prefix.$names[$d]; 

	if( $fname =~ /\.html$/ )
	{
	    # print $fname, "\n";

	    index_file ( $fname );
	}
	else
	{
	    if ( -d $fname )
	    {
		if ( $names[$d] !~ /^\./ )
		{
		    # print "directory: " , $fname, "\n";
		    
		    search_all_in_directory( $fname );
		}
	    }
	    else
	    {
		# print "'", $fname, "' ?\n";
	    }
	}
    }

    closedir DIR;
}

# \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/


sub index_file
{
    local($fname) = @_;

    my $word;
    my %pos_table;
    my @pos_list;
    my %word_count;
    my $word = 0;
    my $text;

    # print "opening ", $fname, "\n";
    
    # first, extract the title from the document

    open(in_file, $fname);
    
    $in_title = 0;
    $title = '';
    while(<in_file>)
    {
	chop;
	
	@words = split /\ |\<|\>|\n|\t|\,|\./;
	
	foreach $w (@words)
	{
	    # print "+" . $w . "+\n";
	    
	    if($w eq "/TITLE")
	    {
		$in_title = 2;
	    }
	    
	    if($in_title == 1)
	    {
		if(length($title) > 0) { $title .= ' '; }
		$title .= $w;
	    }
	    if($w eq "TITLE")
	    {
		if($in_title == 0)
		{
		    $in_title = 1;
		}
	    }
	}
    }
    
    $filename_to_title{ $fname } = $title;

    # then index each of the words

    open(in_file, $fname);

    while(<in_file>)
    {
	chop;
	$text = $text . ($_ . " ");
    }

    # convert to lowercase
    $text =~ tr/A-Z/a-z/;

    # strip HTML tags
    $text =~ s/\<[^\<\>]+\>/ /go;     # open tags
    $text =~ s/\<\/[^\<\>]+\>/ /go;   # close tags
    
    # convert into words
    @words = split(/ |\n|\t|\<|\"|\\|\/|\'|\`|\=|\(|\)|\-|\#|\&|\,|\.|\:|\;|\>/, $text);
    
    for($i=0; $i < $#words; $i++)
    {
	if( length( $words[$i] ) >= $min_word_length)
	{
	    # print $words[$i], "\t", $ARGV[0], "\n";
	    $word++;
	    
	    # save positions of all occurances of this word
	    @pos_list = @{ $pos_table { $words[$i] } } ;
	    @pos_list = ( @pos_list, $word );
	    $pos_table { $words[$i] } = [ @pos_list ];
	    
	    $total_n_words++;
	    
	    $word_count{ $words[$i] } ++;
	    
	}
	
    }
    
    $total_n_lines++;

    # $debug_index_file = 1;

    if($debug_index_file)
    {
	@keys = keys %pos_table;
	for($k=0; $k <= $#keys; $k++)
	{
	    $word = $keys[ $k ];
	    
	    @pos_list = @{ $pos_table { $word } } ;
	    
	    print $k, "=", $word, ":";
	    
	    for($p=0; $p <= $#pos_list; $p++)
	    {
		print $pos_list[$p], " ";
	    }
	    
	    print "\n";
	}
    }


    # now store the index in this file in the master index

    @keys = keys %pos_table;
    for($k=0; $k <= $#keys; $k++)
    {
	$word = $keys[ $k ];
	
	@master_list = @{ $master_table { $word } } ;
	@pos_list    = @{ $pos_table { $word } } ;
	
	$n_matches = $#pos_list + 1;

	# and append this list in the master list for this word
	
	# (put the file_id and number of matches at the head of the local list)
	@master_list = ( @master_list, $file_count, $n_matches, @pos_list );

	$master_table { $word } = [ @master_list ];
    }    
    
    $file_names[ $file_count ]  = $fname;
    $file_count++;
    
}

# \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/

@dir_list = ( "docs", "plugins" );

$run = 1;

if($run)
{
    while( $dir_list[0] )
    {
	# print "doing " . $dir_list[0] . "\n";

	search_all_in_directory ( $dir_list[0] );

	shift  @dir_list;
    }

    #@words = sort { @{$master_table{$b}} <=> @{$master_table{$a}} } keys %master_table;
    #@words = sort { @{$master_table{$b}}[0] <=> @{$master_table{$a}}[0] } keys %master_table;
    #@words = sort { @{$b} <=> @{$a} } keys %master_table;
    @words = sort ( keys % master_table );

    # @words = keys %master_table;

    print $file_count, "\n";
    
    for($f=0; $f < $file_count; $f++)
    {
	print $file_names[ $f ], "\n";
    }

    for($f=0; $f < $file_count; $f++)
    {
	print  $filename_to_title{ $file_names[ $f ] }, "\n";
    }
    
     
    for($i=0; $i <= $#words; $i++)
    {
	$word = $words[$i];
	
	# now parse the list of hits:

	@pos_list = @{ $master_table { $word } } ;
	
	if($debug_pos_list)
	{
	    print $word, ": poslist= (";
	    
	    for($p=0; $p <= $#pos_list; $p++)
	    {
		print $pos_list[$p], " ";
	    }
	    print ")\n";
	}

	$done = 0;
	$ind = 0;
	while(! $done)
	{
	    
	    $filecode = $pos_list[$ind];
	    $ind++;
	    $nmatches = $pos_list[$ind];
	    $ind++;

	    print $word, " " ,  $filecode , " " , $nmatches, " ";

	    for($p=0; $p < $nmatches; $p++)
	    {
		print $pos_list[$ind+$p], " ";
	    }
	    print "\n";

	    
	    $ind += $nmatches;

	    if( $ind >= $#pos_list)
	    {
		$done = 1;
	    }

	}
    }


}

# \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/

# $test1 = 1;

if($test1)
{
    # hash of lists

    @t1 = ( 1, 2, 3, 4 );
    @t1 = ( 0, @t1 );
    
#    for($i=0; $i <= $#t1; $i++)
#    {
#	print $t1 [$i], "\n";
#    }
    
    $ht { "test1" } = [ @t1] ;

    @t2 = ( 10,20,30 );
    $ht { "test2" } = [ @t2] ;

    @array = @{ $ht { "test2" } } ;
    
    #print "<", ${$arrayref}[0], ">\n";
    
    for($i=0; $i <= $#array; $i++)
    {
	print $array [$i], "\n";
    }
    
}


if($test2)
{
    print "test2\n";

    @a1 = ([1,5],[8,10]);

    @a2 = ([22, 29]);
    @a2 = (@a2, [13, 20]);

    @b2 = ( @a1, @a2 );
    

    for($i=0; $i <= $#b2; $i++)
    {
	print $b2[$i][0], "--" , $b2[$i][1], "\n";
    }
     
    @t1 = $b2[2];
    print $t1[0] , "..." , $t1[1], "\n";
}


if($test3)
{
    print "test3\n";

    @list = $$a1[0];
    @{$list} = ( 1, @{$list} );
    $a1[0] = [ @list] ;

    @list = $$a1[0];
    @{$list} = ( 2, @{$list} );
    $a1[1] =  [@list ];

    $listref = [ @a1[0] ];

    # for($i=0; $i <= $#data; $i++)
    {
	print $listref[0], "\n";
    }
     
}

#$test4 = 1;

if($test4)
{
    $test = "<HTML><IMG SRC=\"bling.jpg\"><A HREF=\"flappity.html\">flops!</A><TABLE><TITLE>title</TITLE></TABLE></HTML>";
#    $test = "<HTML></A><TABLE><TITLE>title</TITLE></TABLE></HTML>";

    $test = "<META name=\"description\" content=\"maxdView 0.6.x"; # \nGetting Started Tutorial\">hello!</P>";

    # convert to lowercase
    $test =~ tr/A-Z/a-z/;

    # strip HTML tags
    $test =~ s/\<[^\<\>]+\>//go;
    $test =~ s/\<\/[^\<\>]+\>//go;
  
    print $test, "\n";
}

