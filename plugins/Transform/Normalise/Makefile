# --------- -------- --------- -------- --------- -------

CLASS_FILES = Normalise.class GGMNormaliser.class LSNormaliser.class CenteringNormaliser.class IDNormaliser.class SampleNormaliser.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

Normalise.class : Normalise.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) Normalise.java

CenteringNormaliser.class : CenteringNormaliser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) CenteringNormaliser.java
IDNormaliser.class : IDNormaliser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) IDNormaliser.java
GGMNormaliser.class : GGMNormaliser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) GGMNormaliser.java
LSNormaliser.class : LSNormaliser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) LSNormaliser.java
SampleNormaliser.class : SampleNormaliser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) SampleNormaliser.java


####### stuff

clean:
	rm -f *.class
