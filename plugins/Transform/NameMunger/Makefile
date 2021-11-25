# --------- -------- --------- -------- --------- -------

CLASS_FILES = NameMunger.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### rules

NameMunger.class : NameMunger.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) NameMunger.java

####### stuff

clean:
	rm -f *.class