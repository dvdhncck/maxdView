# --------- -------- --------- -------- --------- -------

CLASS_FILES = RunExternal.class ExternalProg.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

RunExternal.class : RunExternal.java ExternalProg.java
	${JAVA_HOME}/bin/javac  -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) RunExternal.java
ExternalProg.class : ExternalProg.java 
	${JAVA_HOME}/bin/javac  -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ExternalProg.java

####### stuff

clean:
	rm -f *.class