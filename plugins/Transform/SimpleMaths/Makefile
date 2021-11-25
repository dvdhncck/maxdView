# --------- -------- --------- -------- --------- -------

CLASS_FILES = SimpleMaths.class Parser.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

Parser.class : Parser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) Parser.java
SimpleMaths.class : SimpleMaths.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SimpleMaths.java

####### stuff

clean:
	rm -f *.class