#!/usr/bin/perl
#
use IO;

STDOUT->autoflush(1);

while(<STDIN>)
{
    chop;

    $ok = 0;

    if( $_ eq "5x1")
    {
	print "$_ = \n\n";
	print "   1.23    2.34   3.45    4.56    5.67\n";

	$ok = 1;
    }

    if( $_ eq "1x5")
    {
	print "$_ = \n\n";
	print "   1.23\n";
	print "   2.34\n";
	print "   3.45\n"; 
	print "   4.56\n";
	print "   5.67\n";

	$ok = 1;
    }

    if( $_ eq "3x3")
    {
	print "$_ = \n\n";
	print "   1    2   3\n";
	print "   4    5   6\n";
	print "   7    8   9\n";

	$ok = 1;
    }

    if( $_ eq "broken")
    {
	print "$_ = \n\n";
	print "   1    2   3\n";
	print "   4    6\n";
	print "   7    8   9\n";

	$ok = 1;
    }
   
    if($ok == 0)
    {
	@parts = split(/x/, $_);
	$col = $parts[0];
	$row = $parts[1];
	
	if(($col > 0) && ($row > 0))
	{
	    print $col . "x" . $row ." = \n\n";
	    
	    for($r=1; $r <= $row ; $r++)
	    {
		for($c=1; $c <= $col ; $c++)
		{
		    if($c > 0)
		    { print "\t"; }
		    
		    print ($c * $r);
		}
		 print "\n";
	    }
	    $ok = 1;
	}
    }

    if( /\;$/ )
    {
	$ok = 1;
    }

    if($ok == 0)
    {
	print "computing $_ ...\n";
    }
    
}
