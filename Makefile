ECLIPSE ?= eclipse
MD_TARGETS := $(wildcard docs/*.md)
PANDOC_FLAGS := --toc --filter pandoc-crossref
DOC_TARGETS := $(MD_TARGETS:.md=.pdf)

.PHONY: all
all:
	@ant

.PHONY: run
run:
	@ant run

.PHONY: run-debug
run-debug:
	@ant run-debug

.PHONY: clean
clean:
	@ant clean

.PHONY: docs
docs: $(DOC_TARGETS)
	$(info $@)
	@echo > /dev/null

.PHONY: format
format:
	$(ECLIPSE) -nosplash -application org.eclipse.jdt.core.JavaCodeFormatter -config .settings/org.eclipse.jdt.core.prefs src

%.pdf: %.md
	$(info [DOC] $< -> $@)
	@pandoc $(PANDOC_FLAGS) --from=markdown --latex-engine=xelatex --to=latex $< -o $@
