# requires GNU make
SHELL=/bin/bash

.DELETE_ON_ERROR:

%.pdf %.aux %.idx: %.tex
	pdflatex -halt-on-error -file-line-error $<
	while grep 'Rerun to get ' $*.log ; do pdflatex -halt-on-error $< ; done
%.ind: %.idx
	makeindex $*
%.bbl: %.aux
	bibtex $*
%.pdftex %.pdftex_t: %.fig
	fig2dev -L pdftex_t -p $*.pdftex $< $*.pdftex_t
	fig2dev -L pdftex $< $*.pdftex

all: dissertation.pdf

dissertation.pdf: titlepage.tex CUni3.pdf declaration.tex abstract.tex

clean:
	rm -f *.log *.aux *.toc *.bbl *.ind *.lot *.lof
