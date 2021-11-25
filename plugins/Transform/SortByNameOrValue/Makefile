# --------- -------- --------- -------- --------- -------

CLASS_FILES = SortByNameOrValue.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SortByNameOrValue.class : SortByNameOrValue.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SortByNameOrValue.java

####### stuff

clean:
	rm -f *.class