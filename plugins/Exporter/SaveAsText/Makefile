# --------- -------- --------- -------- --------- -------

CLASS_FILES = SaveAsText.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SaveAsText.class : SaveAsText.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SaveAsText.java

####### stuff

clean:
	rm -f *.class
