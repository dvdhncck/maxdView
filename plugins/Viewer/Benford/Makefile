# --------- -------- --------- -------- --------- -------

CLASS_FILES = BenfordAnalyser.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

BenfordAnalyser.class : BenfordAnalyser.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH)$(MV_GP_CLASSPATH) $(COMPILE_OPTIONS) BenfordAnalyser.java

####### stuff

clean:
	rm -f *.class


