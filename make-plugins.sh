#!/bin/tcsh
#
# -------------------------------------------------------------------------------------

# version 2, doesn't use the '-printf' action of 'find'
# and use env vars to pass the locations of Java and Swing
# to the plugin's makefiles

#
# version 3 uses tcsh's { } test to stop once one of the compiles
# fails rather than ploughing on with the other compiles
#

#
# version 4 accepts the JAVA_HOME & COMPILE_OPTIONS variables as arguments
#           to make sure the plugins are compiled the same as the core
#

# -------------------------------------------------------------------------------------

#
# platform independant options
#

#
# JAVA_HOME & COMPILE_OPTIONS are now set using argv[1] and argv[2]
#

setenv JAVA_HOME ${1}
setenv COMPILE_OPTIONS "-g -target 1.4 -source 1.4"

echo compiling with $JAVA_HOME $COMPILE_OPTIONS

# -------------------------------------------------------------------------------------

#
# platform specific options
#

if (uname !~ "CYGWIN.+") then

  #
  # this is the CygWin environment...
  #


  #
  # eek, look how much escaping is needed!
  #

  setenv MV_CLASSPATH \"..\/..\/..\/\;\.\"

  setenv MV_SAX_CLASSPATH "..\/..\/..\/\;\.\;..\\..\\..\\external\\xml4j\\xerces.jar"

  setenv MV_GP_CLASSPATH \"\;GraphPackage\"

  setenv MV_OROINC_CLASSPATH  "..\/..\/..\/\;\.\;..\\..\\..\\external\\OROMatcher-1.1.0a\\"

  setenv MV_WEKA_CLASSPATH "..\/..\/..\/\;\.\;c:\\softs\\weka-3-4\\weka.jar"

# this is not used any more
#  setenv MV_JNL_CLASSPATH "..\/..\/..\/\;\.\;..\\accessories\\JNL\\Classes"


  #
  # for bash-like shells the classpath is of the form \".\;..\/..\/..\/\"
  #

else

  #
  # this is a unix-like environment...
  #
  setenv MV_CLASSPATH ".:../../.."
  setenv MV_SAX_CLASSPATH ".:../../..:GraphPackage/"
  setenv MV_GP_CLASSPATH ".:../../..:external/xml4j/xerces.jar"
  setenv MV_OROINC_CLASSPATH ".:../../..:external/OROMatcher-1.1.0a"
  setenv MV_WEKA_CLASSPATH ".:../../..:/usr/local/softs/weka-3-2-4/weka.jar"
# this is not used any more
#  setenv MV_JNL_CLASSPATH ".:../../..:/home/java/apps/JNL/Classes"

endif

# -------------------------------------------------------------------------------------

#
# the actual commands (i.e. a recursive make) themselves
#


foreach i ( `find plugins -type d -print` )
#
#   make options:
#        -e make environment vars override definitions found in Makefiles
#        -C changes to the specified directory before searching for Makefile
#
    pushd $i
    if (-e Makefile) then

      if ( { make -e } <= 0) then
        exit -1
      endif
      
    endif
    popd
end
