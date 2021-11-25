# --------- -------- --------- -------- --------- -------

CLASS_FILES = SVD.class WachterPlot.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SVD.class : SVD.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SVD.java
WachterPlot.class : WachterPlot.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) WachterPlot.java

####### stuff

clean:
	rm -f *.class