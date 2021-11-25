# --------- -------- --------- -------- --------- -------

CLASS_FILES = PvalueCalculator.class  TTestType.class StudentTTestCalculator.class TTest.class

MV_COLT_CLASSPATH = "..\/..\/..\/\;.\;..\/..\/..\/external\/colt\/colt.jar"

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

PvalueCalculator.class : PvalueCalculator.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) PvalueCalculator.java
TTestType.class : TTestType.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) TTestType.java
StudentTTestCalculator.class : StudentTTestCalculator.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) StudentTTestCalculator.java
TTest.class : TTest.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) TTest.java

####### stuff

clean:
	rm -f *.class