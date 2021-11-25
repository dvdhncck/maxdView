# --------- -------- --------- -------- --------- -------

CLASS_FILES = EventWatcher.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

EventWatcher.class : EventWatcher.java
	${JAVA_HOME}/bin/javac  -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) EventWatcher.java

####### stuff

clean:
	rm -f *.class