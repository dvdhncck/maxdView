
use File::Glob ':glob';

@src_list = glob ('*.java');

foreach $i ( @src_list )
{
#    print $i, "\n";

# check that there is at least on .class file for this .java file

    $obj_name = $i;

    $obj_name =~  s/\.java/\.class/;

#    @obj_list = glob ( '$obj_name' );

    if(! -e $obj_name)
    {
	print "WARNING: ", $obj_name, " not found\n";
    }

}
