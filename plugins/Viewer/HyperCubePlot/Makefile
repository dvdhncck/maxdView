# --------- -------- --------- -------- --------- -------

CLASS_FILES = HyperCubePlot.class KnobPanel.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

HyperCubePlot.class : HyperCubePlot.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) HyperCubePlot.java
KnobPanel.class : KnobPanel.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) KnobPanel.java

####### stuff

clean:
	rm -f *.class