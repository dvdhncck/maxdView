# --------- -------- --------- -------- --------- -------

CLASS_FILES = WebPlot.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

WebPlot.class : WebPlot.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) WebPlot.java

####### stuff

clean:
	rm -f *.class