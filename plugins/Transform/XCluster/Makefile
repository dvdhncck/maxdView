# --------- -------- --------- -------- --------- -------

CLASS_FILES = XCluster.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

XCluster.class : XCluster.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) XCluster.java

####### stuff

clean:
	rm -f *.class