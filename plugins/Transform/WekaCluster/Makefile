# --------- -------- --------- -------- --------- -------

CLASS_FILES = WekaCluster.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

WekaCluster.class : WekaCluster.java
	${JAVA_HOME}/bin/javac -classpath $(MV_WEKA_CLASSPATH) $(COMPILE_OPTIONS) WekaCluster.java

####### stuff

clean:
	rm -f *.class