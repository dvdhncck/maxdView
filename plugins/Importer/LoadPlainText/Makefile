# --------- -------- --------- -------- --------- -------

CLASS_FILES = LoadPlainText.class 

# --------- -------- --------- -------- --------- -------

all: $(CLASS_FILES)

####### 

LoadPlainText.class : LoadPlainText.java
	${JAVA_HOME}/bin/javac -classpath $(MV_CLASSPATH) $(COMPILE_OPTIONS) LoadPlainText.java

####### stuff

clean:
	rm -f *.class