# --------- -------- --------- -------- --------- -------

CLASS_FILES = StackPlot.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

StackPlot.class : StackPlot.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) StackPlot.java

####### stuff

clean:
	rm -f *.class