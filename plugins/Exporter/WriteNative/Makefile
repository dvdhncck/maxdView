# --------- -------- --------- -------- --------- -------

CLASS_FILES = WriteNative.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

WriteNative.class : WriteNative.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) WriteNative.java

####### stuff

clean:
	rm -f *.class
