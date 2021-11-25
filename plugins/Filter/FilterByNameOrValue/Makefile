# --------- -------- --------- -------- --------- -------

CLASS_FILES = FilterByNameOrValue.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

FilterByNameOrValue.class : FilterByNameOrValue.java
	${JAVA_HOME}/bin/javac  -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) FilterByNameOrValue.java

####### stuff

clean:
	rm -f *.class