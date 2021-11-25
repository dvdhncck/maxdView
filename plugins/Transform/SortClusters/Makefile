# --------- -------- --------- -------- --------- -------

CLASS_FILES = SortClusters.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SortClusters.class : SortClusters.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SortClusters.java

####### stuff

clean:
	rm -f *.class