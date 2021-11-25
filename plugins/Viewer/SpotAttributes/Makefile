# --------- -------- --------- -------- --------- -------

CLASS_FILES = SpotAttributes.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SpotAttributes.class : SpotAttributes.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SpotAttributes.java

####### stuff

clean:
	rm -f *.class