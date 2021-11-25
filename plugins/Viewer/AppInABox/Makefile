# --------- -------- --------- -------- --------- -------

CLASS_FILES = \
\
AppInABox.class \
\
\
# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

AppInABox.class : AppInABox.java LimitedStyledDocument.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) AppInABox.java

####### stuff

clean:
	rm -f *.class