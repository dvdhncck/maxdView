# --------- -------- --------- -------- --------- -------

CLASS_FILES = MathFilter.class Parser.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

MathFilter.class : MathFilter.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) MathFilter.java
Parser.class : Parser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) Parser.java

####### stuff

clean:
	rm -f *.class