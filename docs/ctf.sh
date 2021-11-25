#!/bin/bash
#
# _c_hange _t_ags in _f_iles 
#
if test $# -lt 2; then
    echo "$0: wrong number of parameters" >&2
    cat <<EOF
usage: $0 FROM TO [root-dir] [file-name-spec]
  [root-dir]           optional root directory, default is .
  [file-name-spec]     optional filename filter (e.g. *.h)
EOF
    exit 1
fi

from_tag=$1; shift

to_tag=$1; shift

echo from $from_tag to $to_tag;

if test $# -ge 1; then
   regexp=$1; shift
else
   regexp="."
fi

echo regexp is $regexp;

if test $# -ge 1; then
   rootdir=$1; shift
else
   rootdir="."
fi

echo rootdir is $rootdir;


let counter=0
let dirs=0
let hit=0
suffix=tmp.$$.out

if ! test -e $rootdir; then
  echo "$0: $rootdir: no such file or directory" >&2
  exit 0;
fi

if test $# -gt 0; then
  namefilter=$1;
  let dirs=1
  echo "name filter is" $namefilter
    for a in `find $rootdir -name $namefilter -print`; do
       echo "Doing $a"	
	if test -f $a; then
          if grep -n $regexp $a; then
	    echo '  -- in' $a ' --'
	  let hit=1;
	  fi
          let counter=$counter+1;
	else
	  let dirs=$dirs+1;
	fi
        done
else
    for a in `find $rootdir -print`; do
#       echo "Doing $a"	
	if test -f $a; then
#          echo grep -n -e \"$regexp\" $a
          if grep -n $regexp $a; then
	    echo '  -- in' $a ' --'
	  let hit=1;
	  fi
          let counter=$counter+1;
	else
	  let dirs=$dirs+1;
	fi
        done
fi  

if test $hit -eq 0; then
  echo No matches;
fi

echo $counter files, $dirs directories visitated
