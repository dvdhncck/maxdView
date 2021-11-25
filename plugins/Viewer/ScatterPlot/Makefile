# --------- -------- --------- -------- --------- -------

CLASS_FILES = ScatterPlot.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ScatterPlot.class : ScatterPlot.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ScatterPlot.java

####### stuff

clean:
	rm -f *.class