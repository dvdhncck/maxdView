# --------- -------- --------- -------- --------- -------

CLASS_FILES = DatabaseLoader.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

DatabaseLoader.class : DatabaseLoader.java
	${JAVA_HOME}/bin/javac -classpath ${MV_CLASSPATH} $(COMPILE_OPTIONS) DatabaseLoader.java

####### stuff

clean:
	rm -f *.class