# --------- -------- --------- -------- --------- -------

CLASS_FILES = Distogram.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

Distogram.class : Distogram.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) Distogram.java

####### stuff

clean:
	rm -f *.class