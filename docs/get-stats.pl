
sub scan_directory
{
    local($dir_name) = @_;

#    print "scan_directory: ", $dir_name, "\n";

    my @names;
    my $name;

    opendir(DIR, $dir_name) || die "can't opendir $dir_name: $!";

    @names = readdir(DIR);

    foreach $name (@names)
    {
	if ( $name !~ /^\./ )
	{
	    $full_name = $dir_name . '/' . $name;

	    if( $full_name =~ /.java$/ )
	    {
		scan_code( $full_name );
	    }
	    else
	    {
		if( $full_name =~ /.class$/ )
		{
		    $total_number_of_classes++;
		}
		else
		{
		    if( -d $full_name )
		    {
			scan_directory( $full_name );
		    }
		}
	    }
	}
    }
    
}


sub scan_code
{
    local($file_name) = @_;
    
    my $lines;
    my $lines_of_code;
    my $one_line_comments;
    my $block_comments;
    my $block_comment_lines;
    my $trimmed;
    my $one_line_comment_pos;
    my $comment_block_start_pos;
    my $comment_block_end_pos;

    my $in_comment_block;

#    print "scan_code: ", $file_name, "\n";
    
    open(INFILE, $file_name);
 
    $in_comment_block = 0;

    while(<INFILE>)
    {
	chop;
	
	if( length( $_ ) > 0 )
	{
	    $lines++;

	    $trimmed = $_;
	    $trimmed =~ s/^\ +//;
	    $trimmed =~ s/\ +$//;

	    $one_line_comment_pos = index $trimmed, "\/\/";

	    $comment_block_start_pos = index $trimmed, "\/\*";
	    $comment_block_end_pos   = index $trimmed, "\*\/";

	    if( $comment_block_start_pos >= 0 )
	    {
		$in_comment_block = 1;
		$block_comments++;
	    }


	    if( $in_comment_block )
	    {
		$block_comment_lines++;
	    }
	    else
	    {
		if( $one_line_comment_pos == 0 )
		{
		    $one_line_comments++;
		}
		else
		{
		    if( $one_line_comment_pos > 0 )
		    {
			$one_line_comments++;
			$lines_of_code++;
		    }
		    else
		    {
			$lines_of_code++;
		    }
		}
	    }

	    
	    if( $comment_block_end_pos >= 0 )
	    {
		$in_comment_block = 0;
	    }


	}
    }
   
    print $file_name . "\n".
	"\tcode=" . $lines_of_code . 
	", o_comm=" . $one_line_comments . 
	", b_comm=" . $block_comments . "\n";

    $total_one_line_comments += $one_line_comments;
    $total_lines_of_code += $lines_of_code;
    $total_block_comments += $block_comments;
    $total_files ++;
}


#scan_directory("../../test_get_stats");
scan_directory("..");

print "totals:\n";
print "             files = $total_files\n";
print "           classes = $total_number_of_classes\n";
print "     lines of code = $total_lines_of_code\n";
print " one line comments = $total_one_line_comments\n";
print "    comment blocks = $total_block_comments\n";
