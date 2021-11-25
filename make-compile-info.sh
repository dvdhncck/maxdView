#!/bin/bash

echo $1

echo class CompilationInfo
echo \{

# 
# date and uname are easy.....
#
echo public final String compile_time = \"`date`\"\;
echo public final String compile_host = \"`uname`\"\; 

#
# the Java version info is written to stderr rather than stdout, so temporarily
# dump to it a file.
#
`$1 -version >& jtmp;`

#
# use 'tr' to remove the "s and LFs that appear in the version string.
#
echo public final String compiler     = \"`head -1 jtmp | tr \" \' | tr \[\:cntrl\:\] \ `\"\; 

#
# and clean up
#
rm -f jtmp

echo \}


