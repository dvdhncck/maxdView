# --------- -------- --------- -------- --------- -------

CLASS_FILES = ProfileViewer.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ProfileViewer.class : ProfileViewer.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ProfileViewer.java

####### stuff

clean:
	rm -f *.class