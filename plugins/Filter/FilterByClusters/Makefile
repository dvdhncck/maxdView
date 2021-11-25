# --------- -------- --------- -------- --------- -------

CLASS_FILES = FilterByClusters.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

FilterByClusters.class : FilterByClusters.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) FilterByClusters.java

####### stuff

clean:
	rm -f *.class