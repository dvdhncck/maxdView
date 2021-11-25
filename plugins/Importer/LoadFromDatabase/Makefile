# --------- -------- --------- -------- --------- -------

CLASS_FILES = SwingWorker.class LoadFromDatabase.class Controller.class maxdConnection_m2.class ConnectionManager_m2.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

LoadFromDatabase.class : LoadFromDatabase.java
	${JAVA_HOME}/bin/javac -classpath ${MV_CLASSPATH} $(COMPILE_OPTIONS) LoadFromDatabase.java
Controller.class : Controller.java
	${JAVA_HOME}/bin/javac -classpath ${MV_CLASSPATH} $(COMPILE_OPTIONS) Controller.java
maxdConnection_m2.class : maxdConnection_m2.java
	${JAVA_HOME}/bin/javac -classpath ${MV_CLASSPATH} $(COMPILE_OPTIONS) maxdConnection_m2.java
ConnectionManager_m2.class : ConnectionManager_m2.java
	${JAVA_HOME}/bin/javac -classpath ${MV_CLASSPATH} $(COMPILE_OPTIONS) ConnectionManager_m2.java
SwingWorker.class : SwingWorker.java
	${JAVA_HOME}/bin/javac -classpath ${MV_CLASSPATH} $(COMPILE_OPTIONS) SwingWorker.java

####### stuff

clean:
	rm -f *.class