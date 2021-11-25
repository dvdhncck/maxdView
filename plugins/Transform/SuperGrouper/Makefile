# --------- -------- --------- -------- --------- -------

CLASS_FILES = SuperGrouper.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SuperGrouper.class : SuperGrouper.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SuperGrouper.java

####### stuff

clean:
	rm -f *.clas