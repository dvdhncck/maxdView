# --------- -------- --------- -------- --------- -------

CLASS_FILES = SaveToDatabase.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

SaveToDatabase.class : SaveToDatabase.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SaveToDatabase.java

####### stuff

clean:
	rm -f *.class