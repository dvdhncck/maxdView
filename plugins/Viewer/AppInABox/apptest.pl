#!/usr/bin/perl
#
use IO;

open(file, '>/home/dave/bio/maxd/maxdView/plugins/Viewer/AppInABox/log.dat');
open(out, '>-');

out->autoflush(1);
file->autoflush(1);

while(<STDIN>)
{
    printf out $_, "\n";

    printf out "HelloO\n";

    printf file $_;

#    flush;
#    fflush file;
}

