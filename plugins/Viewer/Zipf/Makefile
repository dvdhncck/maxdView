# --------- -------- --------- -------- --------- -------

CLASS_FILES = ZipfAnalyser.class
 
# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ZipfAnalyser.class : ZipfAnalyser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ZipfAnalyser.java

####### stuff

clean:
	rm -f *.class
