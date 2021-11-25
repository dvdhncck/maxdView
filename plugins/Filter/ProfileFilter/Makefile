# --------- -------- --------- -------- --------- -------

CLASS_FILES = ProfileFilter.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

ProfileFilter.class : ProfileFilter.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) ProfileFilter.java

####### stuff

clean:
	rm -f *.class