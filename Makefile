ECLIPSE ?= eclipse

.PHONY: all
all:
	ant

.PHONY: run
run:
	ant run

.PHONY: run-debug
run-debug:
	ant run-debug

.PHONY: clean
clean:
	ant clean

.PHONY: format
format:
	$(ECLIPSE) -nosplash -application org.eclipse.jdt.core.JavaCodeFormatter -config .settings/org.eclipse.jdt.core.prefs src
