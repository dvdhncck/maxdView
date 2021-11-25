#!/usr/bin/perl
#

sub process_file
{
    local($fname) = @_;

    if( $fname =~ m/$filespec/ )
    {
#	print "file: $fname\n";
#	print "$fname\n";

	open(infile, $fname);
	
	open(outfile, ">./tmpfile");

	my $go = 0;
	my $close_c = 0;

	while(<infile>)
	{
	    print outfile $_;

	    if( m/\<BODY\>/ )
	    {
		print outfile "\<\!\-\- PAGE BREAK \-\-\>\n"; 
	    }
	}
    
	print "$fname\n";

	close(infile);
	close(outfile);

	unlink $fname;
	
	rename "tmpfile", $fname ;
    }

    return;
}

sub process_directory
{
    local($dname) = @_;
    
    my $c;
    my @contents;
    my $name;

#    print "dir: $dname\n";
    
    @contents = glob($dname . "/*");

#    print "contains: " . ($#contents+1) . " things\n";
        
    for($c=0; $c <= $#contents; $c++)
	{
	    $name = $contents[$c];

	    if(($name ne '.') && ($name ne '..'))
	    {
		if(-d $name)
		{
		    if($descend > 0)
		    {
			process_directory( $name );
		    }
		}
		else
		{
		    process_file( $name );
		}
	    }
	}
    
    return;
}

if((length($ARGV[0]) == 0) || (length($ARGV[1]) == 0) || (length($ARGV[2]) == 0))
{
    print "usage: change-tags.pl FROM_TAG TO_TAG [root-dir] file-spec\n";
    exit;
}

$all = 1;

$filespec = '.';
 
$file_spec_pos = 2;
$descend = 0;

$from_tag = $ARGV[0];
$to_tag = $ARGV[1];

# $from_tag =~ tr/[A-Z]/[a-z]/;

print "$from_tag to $to_tag\n";

if(length($ARGV[3]) > 0)
{
    $file_spec_pos = 3;
    if(length($ARGV[2]) > 0)
    {
	$rootdir = $ARGV[2];
	$descend = 1;
    }
}
else
{
    $rootdir = '.';
}

if(length($ARGV[$file_spec_pos]) > 0)
{
    $filespec = $ARGV[$file_spec_pos];
}

print "starting in $rootdir, filespec is $filespec\n";

process_directory($rootdir);


