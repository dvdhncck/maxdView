# --------- -------- --------- -------- --------- -------

CLASS_FILES = ReorderMeasurements.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ReorderMeasurements.class : ReorderMeasurements.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ReorderMeasurements.java

####### stuff

clean:
	rm -f *.class
