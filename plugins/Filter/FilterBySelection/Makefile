# --------- -------- --------- -------- --------- -------

CLASS_FILES = FilterBySelection.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

FilterBySelection.class : FilterBySelection.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) FilterBySelection.java

####### stuff

clean:
	rm -f *.class