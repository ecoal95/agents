ECLIPSE ?= eclipse

all:
	ant

run:
	ant run

run-debug:
	ant run-debug

clean:
	ant clean

format:
	$(ECLIPSE) -nosplash -application org.eclipse.jdt.core.JavaCodeFormatter -config .settings/org.eclipse.jdt.core.prefs src
