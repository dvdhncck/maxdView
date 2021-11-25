# --------- -------- --------- -------- --------- -------

CLASS_FILES = TextExplorer.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

TextExplorer.class : TextExplorer.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) TextExplorer.java

####### stuff

clean:
	rm -f *.class