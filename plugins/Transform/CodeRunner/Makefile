# --------- -------- --------- -------- --------- -------

CLASS_FILES = CodeRunner.class CodeWrapper.class CompiledUserClass.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

CodeRunner.class : CodeRunner.java CodeWrapper.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) CodeRunner.java

CompiledUserClass.class : CompiledUserClass.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) CompiledUserClass.java


####### stuff

clean:
	rm -f *.class