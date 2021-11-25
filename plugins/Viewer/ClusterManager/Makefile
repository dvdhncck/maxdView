# --------- -------- --------- -------- --------- -------

CLASS_FILES = ClusterManager.class ColourSetChooser.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ClusterManager.class : ClusterManager.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ClusterManager.java
ColourSetChooser.class : ColourSetChooser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ColourSetChooser.java

####### stuff

clean:
	rm -f *.class