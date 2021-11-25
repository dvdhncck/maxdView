#!/bin/tcsh
#
echo "** make-method-ref.sh begins"

# ------------------------------------------------------------------------------------------------

if (uname !~ "CYGWIN.+") then

# ------------------------------------------------------------------------------------------------

  setenv DOCS_HOME "docs\"
  setenv JAVA_HOME /cygdrive/c/softs/jdk1.4.2


  /bin/rm -f  ${DOCS_HOME}method-ref.dat

  foreach i ( ExprData DataPlot maxdView AnnotationLoader AnnotationViewer AxisManager Colouriser DatabaseConnection DecorationManager HelpPanel Plugin PrintManager ProgressOMeter RemoteExprDataInterface )

    echo searching $i

    ${JAVA_HOME}/bin/javap -classpath ${DOCS_HOME}.. $i > tmp

    perl docs/parse-javap-output.pl < tmp  >> docs/method-ref.dat

    /bin/rm -f tmp

  end

# ------------------------------------------------------------------------------------------------

else

# ------------------------------------------------------------------------------------------------

  setenv DOCS_HOME /home/dave/bio/maxd/maxdView/docs/
  setenv JAVA_HOME /home/java/jdk

  /bin/rm -f  ${DOCS_HOME}method-ref.dat

  foreach i ( ExprData DataPlot maxdView AnnotationLoader AnnotationViewer AxisManager Colouriser DatabaseConnection DecorationManager HelpPanel Plugin PrintManager ProgressOMeter RemoteExprDataInterface )

    echo searching $i

    ${JAVA_HOME}/bin/javap -classpath ${DOCS_HOME}.. $i | ${DOCS_HOME}parse-javap-output.pl >> ${DOCS_HOME}method-ref.dat

  end

endif

# ------------------------------------------------------------------------------------------------

echo "** make-method-ref.sh ends"


