# --------- -------- --------- -------- --------- -------

CLASS_FILES = NewProfileViewer.class SpotPicker.class ProfilePanel.class MeasPicker.class MeasGroupPicker.class GraphLayoutPanel.class OptionsPanel.class PanelSelector.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

NewProfileViewer.class : NewProfileViewer.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) NewProfileViewer.java

ProfilePanel.class : ProfilePanel.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ProfilePanel.java

SpotPicker.class : SpotPicker.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SpotPicker.java

MeasPicker.class : MeasPicker.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) MeasPicker.java

MeasGroupPicker.class : MeasGroupPicker.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) MeasGroupPicker.java

GraphLayoutPanel.class : GraphLayoutPanel.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) GraphLayoutPanel.java

OptionsPanel.class : OptionsPanel.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) OptionsPanel.java

PanelSelector.class : PanelSelector.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) PanelSelector.java

####### stuff

clean:
	rm -f *.class