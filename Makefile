# ===================================================================================
#
#     makefile for maxdView 1.x
#
#
#     multi-os version,  26th June 2002
#
#
#     CygWin+MS:Windows:XP == the CygWin(c) Unix Toolset, see www.cygwin.com
#
#
# ===================================================================================


#
# --- JAVA setup ---------------------------------------------
#


#JAVA_HOME = /home/java/jdk
#JAVA_HOME = /cygdrive/c/softs/jdk1.5.0
JAVA_HOME = /cygdrive/c/softs/Java/jdk1.5.0_06
##JAVA_HOME = /cygdrive/d/java/jdk1.2.2
#JAVA_HOME = "/cygdrive/c/softs/jdk1.5.0_02"

#
# --- PERL setup ---------------------------------------------
#

PERL = perl

#
# --- RMI setup ---------------------------------------------
#

#RMI_OPTS = -Djava.rmi.server.codebase=file:/home/dave/bio/maxd/maxdView/ -Djava.security.policy=/home/dave/.java.policy


#
# --- classpath -----------------------------------------------
#

#CLASSPATH =  -classpath .:${JDBC_HOME}:${EXTRAS}
CLASSPATH = -classpath .


#
# --- compile & run options -----------------------------------
#

#COMPILE_OPTIONS = -deprecation
#COMPILE_OPTIONS = -O
COMPILE_OPTIONS = -g -target 1.4 -source 1.4

#RUN_OPTIONS = -Xmx128M ${RMI_OPTS}
RUN_OPTIONS = -Xmx350M ${RMI_OPTS}

#
# --- make rules ----------------------------------------------
#


CLASS_FILES = \
\
CompilationInfo.class \
\
maxdView.class \
\
CustomMenu.class \
  PluginCommand.class \
  CustomMenuCommandTreeNode.class \
  MeasSpotAttrID.class \
\
PluginManager.class \
\
DragAndDropEntity.class \
DragAndDropTextField.class \
DragAndDropPanel.class \
DragAndDropList.class \
DragAndDropTree.class \
DragAndDropTable.class \
DraggableTree.class \
\
GraphContext.class \
GraphPlot.class \
GraphPanel.class \
GraphFrame.class \
\
ExprData.class \
RemoteExprDataInterface.class \
\
HelpPanel.class \
HelpMaker.class \
\
ProgressOMeter.class \
NameTagSelector.class \
NumberParser.class \
LabelSlider.class \
\
DataPlot.class \
  Finder.class \
  PlotAxis.class \
  AxisManager.class \
  Decoration.class \
  DecorationManager.class \
  PrintManager.class \
  TagEditor.class \
  DataPlotColourOptions.class \
    RampEditor.class \
    BlenderColouriser.class \
    EqualisingColouriser.class \
    RampedColouriser.class \
    DiscreteColouriser.class \
  DataPlotLayoutOptions.class \
\
Svdcmp.class \
\
DoubleCompare.class \
\
AnnotationViewer.class \
AnnotationLoader.class \
AnnoSource.class \
\
UserInputCancelled.class \
\
DatabaseConnection.class \
ConnectionManager.class

# --------- -------- --------- -------- --------- -------

ISYS_CLASS_FILES = maxdViewISYSClient.class maxdViewISYSService.class maxdViewISYSProvider.class maxdViewISYSSelectionListener.class maxdViewISYSVisibilityListener.class maxdViewISYSDataGrabber.class maxdViewISYSDataPackager.class

# --------- -------- --------- -------- --------- -------

RUNTIME_ENVIRONMENT = LICENCE plugins images docs external code-fragments rmiDemo demo 

# --------- -------- --------- -------- --------- -------

INSTALLER_IMAGES = images/timelapse.jpg images/ticked.jpg images/unticked.jpg images/open-hand.gif images/thumbs-up.gif images/thumbs-down.gif images/flat-palm.gif 

# --------- -------- --------- -------- --------- -------


all: $(CLASS_FILES)
	${JAVA_HOME}/bin/rmic $(CLASSPATH) ExprData ExprData.RemoteCluster
	${PERL} check-for-uncompiled-files.pl

run: $(CLASS_FILES)
	${JAVA_HOME}/bin/java ${RUN_OPTIONS} $(CLASSPATH) maxdView

rundebug: $(CLASS_FILES)
	${JAVA_HOME}/bin/jdb ${RUN_OPTIONS} $(CLASSPATH) maxdView

runremote: $(CLASS_FILES)
	${JAVA_HOME}/bin/java ${RUN_OPTIONS} $(CLASSPATH) maxdView -allow_rmi

all-plugins:
	./make-plugins.sh $(JAVA_HOME) $(COMPILE_OPTIONS)

isys: $(CLASS_FILES) $(ISYS_CLASS_FILES)
	make all


index:
	docs/make-docs.sh


jar: $(CLASS_FILES)
	/bin/rm -f CompilationInfo.class
	make all-plugins
	make all
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) GraphTest.java
#	make isys
	docs/make-docs.sh
	touch CompilationInfo.java
	make CompilationInfo.class
	${JAVA_HOME}/bin/jar cf maxdViewCoreSource.jar Makefile *.java 
	${JAVA_HOME}/bin/jar cf maxdView.jar *.class maxdViewCoreSource.jar $(RUNTIME_ENVIRONMENT) make-plugins.sh
	ls -l *.jar

remake: 
	make super-clean
	make all
	./make-plugins.sh


#
#
####### implicit rules

.java.class:
	${JAVA_HOME}/bin/javac $(CLASSPATH) $<
.class.java:
	${JAVA_HOME}/bin/javac $(CLASSPATH) $<

####### 

maxdView.class : maxdView.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) maxdView.java
AnnoSource.class : AnnoSource.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) AnnoSource.java
AnnotationViewer.class : AnnotationViewer.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) AnnotationViewer.java
AnnotationLoader.class : AnnotationLoader.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) AnnotationLoader.java
AxisManager.class : AxisManager.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) AxisManager.java
BlenderColouriser.class : BlenderColouriser.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) BlenderColouriser.java
ConnectionManager.class : ConnectionManager.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) ConnectionManager.java
CustomMenu.class : CustomMenu.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) CustomMenu.java
CustomClassLoader.class : CustomClassLoader.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) CustomClassLoader.java
CustomMenuCommandTreeNode.class : CustomMenuCommandTreeNode.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) CustomMenuCommandTreeNode.java
DoubleCompare.class : DoubleCompare.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DoubleCompare.java
EqualisingColouriser.class : EqualisingColouriser.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) EqualisingColouriser.java
ExprData.class :  ExprData.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) ExprData.java
DataPlot.class : DataPlot.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DataPlot.java
DataPlotColourOptions.class : DataPlotColourOptions.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DataPlotColourOptions.java
DataPlotLayoutOptions.class : DataPlotLayoutOptions.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DataPlotLayoutOptions.java
DatabaseConnection.class : DatabaseConnection.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DatabaseConnection.java
Decoration.class : Decoration.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) Decoration.java
DecorationManager.class : DecorationManager.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DecorationManager.java
DiscreteColouriser.class : DiscreteColouriser.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DiscreteColouriser.java
DragAndDropEntity.class : DragAndDropEntity.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DragAndDropEntity.java
DragAndDropList.class : DragAndDropList.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DragAndDropList.java
DragAndDropTree.class : DragAndDropTree.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DragAndDropTree.java
DragAndDropTable.class : DragAndDropTable.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DragAndDropTable.java
DragAndDropPanel.class : DragAndDropPanel.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DragAndDropPanel.java
DragAndDropTextField.class : DragAndDropTextField.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DragAndDropTextField.java
DraggableTree.class : DraggableTree.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) DraggableTree.java
Finder.class : Finder.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) Finder.java
GraphPlot.class : GraphPlot.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) GraphPlot.java
GraphFrame.class : GraphFrame.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) GraphFrame.java
GraphPanel.class : GraphPanel.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) GraphPanel.java
GraphContext.class : GraphContext.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) GraphContext.java
LabelSlider.class : LabelSlider.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) LabelSlider.java
MeasSpotAttrID.class : MeasSpotAttrID.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) MeasSpotAttrID.java
FilterController.class : FilterController.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) FilterController.java
PlotAxis.class : PlotAxis.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) PlotAxis.java
PluginCommand.class : PluginCommand.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) PluginCommand.java
PluginManager.class : PluginManager.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) PluginManager.java
PrintManager.class : PrintManager.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) PrintManager.java
RampedColouriser.class : RampedColouriser.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) RampedColouriser.java
RampViewer.class : RampViewer.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) RampViewer.java
RampEditor.class : RampEditor.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) RampEditor.java
HelpPanel.class : HelpPanel.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) HelpPanel.java
HelpMaker.class : HelpMaker.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) HelpMaker.java
ProgressOMeter.class : ProgressOMeter.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) ProgressOMeter.java
RemoteExprDataInterface.class : RemoteExprDataInterface.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) RemoteExprDataInterface.java
Svdcmp.class : Svdcmp.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) Svdcmp.java
NameTagSelector.class : NameTagSelector.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) NameTagSelector.java
NumberParser.class : NumberParser.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) NumberParser.java
TagEditor.class : TagEditor.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) TagEditor.java
#
#
CompilationInfo.class:
	./make-compile-info.sh ${JAVA_HOME}/bin/javac > CompilationInfo.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) CompilationInfo.java
#       force a recompile for this file every time
	touch CompilationInfo.java
#
#
SynthesiseData.class : SynthesiseData.java
	${JAVA_HOME}/bin/javac $(CLASSPATH) $(COMPILE_OPTIONS) SynthesiseData.java
#
#
# wrappers for NCGR's ISYS 
#

#ISYS_HOME = /home/dave/bio/ISYS2/
#ISYS_CLASSPATH = ${ISYS_HOME}lib/collections.jar;${ISYS_HOME}lib/objectmodel.jar;${ISYS_HOME}lib/ice.jar;${ISYS_HOME}lib/system.jar;${ISYS_HOME}lib/event.jar;${ISYS_HOME}lib/util.jar;${ISYS_HOME}lib/web.jar;${ISYS_HOME}lib/isysutil.jar

#
# note the horrid quote method needed to compile using cygwin
#
#ISYS_HOME = c:\\Documents\ and\ Settings\\dave\\bio\\interesting\ softs\\ISYS\\isys_plaform\\

#ISYS_CLASSPATH = ".;${ISYS_HOME}lib/collections.jar;${ISYS_HOME}lib/objectmodel.jar;${ISYS_HOME}lib/ice.jar;${ISYS_HOME}lib/system.jar;${ISYS_HOME}lib/event.jar;${ISYS_HOME}lib/util.jar;${ISYS_HOME}lib/web.jar;${ISYS_HOME}lib/isysutil.jar"

#ISYS_CLASSPATH = .;${ISYS_HOME}isys\\lib\\isys.jar;${ISYS_HOME}isys\\lib\\commons-discovery.jar;${ISYS_HOME}isys\\lib\\commons-logging.jar;${ISYS_HOME}isys\\lib\\Brazil-for-isys.jar;${ISYS_HOME}isys\\lib\\jhbasic.jar;

ISYS_CLASSPATH = ".;lib\\lib\\isys.jar;"

maxdViewISYSClient.class : maxdViewISYSClient.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSClient.java
maxdViewISYSProvider.class : maxdViewISYSProvider.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSProvider.java
maxdViewISYSService.class : maxdViewISYSService.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSService.java
maxdViewISYSSelectionListener.class : maxdViewISYSSelectionListener.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSSelectionListener.java
maxdViewISYSVisibilityListener.class : maxdViewISYSVisibilityListener.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSVisibilityListener.java
maxdViewISYSDataGrabber.class : maxdViewISYSDataGrabber.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSDataGrabber.java
maxdViewISYSDataPackager.class : maxdViewISYSDataPackager.java
	${JAVA_HOME}/bin/javac -classpath $(ISYS_CLASSPATH) $(COMPILE_OPTIONS) maxdViewISYSDataPackager.java

#
#
#

####### cleaning

super-clean:
	touch CompilationInfo.java
#
#
# NOTE: this removes .class files from the external directory which is BAD
#       because the OROMatcher .class files get blasted
#
	/bin/rm -f \*.class
	/bin/rm -f \*~
	find plugins -name \*.class -exec /bin/rm -f \{\} \;
	find plugins -name \*~ -exec /bin/rm -f \{\} \;
#
plugins-clean:
	find plugins -name \*.class -exec /bin/rm -f \{\} \;
#
clean:
	touch CompilationInfo.java
	/bin/rm -rf *.class
