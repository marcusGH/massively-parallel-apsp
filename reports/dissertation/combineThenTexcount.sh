echo "$(head -n 4 diss.tex) $(tail -n +3 -q sections/*.tex) $(tail diss.tex -n +5 -q)" | texcount - $@
