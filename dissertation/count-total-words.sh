echo "$(head -n 4 diss.tex) $(tail -n +3 -q sections/*.tex) $(tail diss.tex -n +5 -q)" | \
    texcount - $@ -sub=chapter | \
    grep -P "Introduction|Preparation|Implementation|Evaluation|Conclusions" | \
    grep -oP "\d+[+]\d+[+]\d+" | \
    bc | \
    awk '{s+=$1} END {print s}'
