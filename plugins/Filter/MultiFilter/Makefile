# --------- -------- --------- -------- --------- -------

CLASS_FILES = MultiFilter.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

MultiFilter.class : MultiFilter.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) MultiFilter.java

####### stuff

clean:
	rm -f *.class