# --------- -------- --------- -------- --------- -------

CLASS_FILES = JustOClust.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

JustOClust.class : JustOClust.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) JustOClust.java

####### stuff

clean:
	rm -f *.class