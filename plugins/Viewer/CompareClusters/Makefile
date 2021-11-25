# --------- -------- --------- -------- --------- -------

CLASS_FILES = CompareClusters.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

CompareClusters.class : CompareClusters.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) CompareClusters.java

####### stuff

clean:
	rm -f *.class