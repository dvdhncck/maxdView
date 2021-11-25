# --------- -------- --------- -------- --------- -------

CLASS_FILES = DataMunger.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

DataMunger.class : DataMunger.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) DataMunger.java

####### stuff

clean:
	rm -f *.class