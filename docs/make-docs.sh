#!/bin/tcsh

# -----------------------------------------------------------------------

if (uname !~ "CYGWIN.+") then


  echo "** make index...."

  perl < docs/make-index.pl > docs/index.dat

  gzip -9 -f docs/index.dat

  echo "** make method reference...."

  docs/make-method-ref.sh

else


  perl < docs/make-index.pl > docs/index.dat

  docs/make-method-ref.sh

  gzip -9 -f docs/index.dat


endif


# -----------------------------------------------------------------------
