# --------- -------- --------- -------- --------- -------

CLASS_FILES = Notepad.class

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

Notepad.class : Notepad.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) Notepad.java

####### stuff

clean:
	rm -f *.class