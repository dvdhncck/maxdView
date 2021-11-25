# --------- -------- --------- -------- --------- -------

CLASS_FILES = RegExpFilter.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

RegExpFilter.class : RegExpFilter.java
	${JAVA_HOME}/bin/javac -classpath $(MV_OROINC_CLASSPATH) $(COMPILE_OPTIONS) RegExpFilter.java

####### stuff

clean:
	rm -f *.class