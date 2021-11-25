# --------- -------- --------- -------- --------- -------

CLASS_FILES = ReadNative.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ReadNative.class : ReadNative.java
	${JAVA_HOME}/bin/javac -classpath $(MV_SAX_CLASSPATH) $(COMPILE_OPTIONS) ReadNative.java

####### stuff

clean:
	rm -f *.class