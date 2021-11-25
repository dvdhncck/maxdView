# --------- -------- --------- -------- --------- -------

CLASS_FILES = QCChart.class QCChartCalculator.class QCControlLineSet.class QCInput.class QCMeasurementStruct.class QCQuicksetWindow.class QCResults.class QCResultsGraphDisplay.class QCResultsTableDisplay.class
# --------- -------- --------- -------- --------- -------

MV_COLT_CLASSPATH = "..\/..\/..\/\;.\;..\/..\/..\/external\/colt\/colt.jar"

all: $(CLASS_FILES)

####### 

QCChart.class : QCChart.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCChart.java
QCChartCalculator.class : QCChartCalculator.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCChartCalculator.java
QCControlLineSet.class : QCControlLineSet.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCControlLineSet.java
QCInput.class : QCInput.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCInput.java
QCMeasurementStruct.class : QCMeasurementStruct.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCMeasurementStruct.java
QCQuicksetWindow.class : QCQuicksetWindow.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCQuicksetWindow.java
QCResults.class : QCResults.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCResults.java
QCResultsGraphDisplay.class : QCResultsGraphDisplay.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCResultsGraphDisplay.java
QCResultsTableDisplay.class : QCResultsTableDisplay.java
	${JAVA_HOME}/bin/javac -classpath $(MV_COLT_CLASSPATH) $(COMPILE_OPTIONS) QCResultsTableDisplay.java

####### stuff

clean:
	rm -f *.class