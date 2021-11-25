# --------- -------- --------- -------- --------- -------

CLASS_FILES = MeasurementManager.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

MeasurementManager.class : MeasurementManager.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) MeasurementManager.java

####### stuff

clean:
	rm -f *.class