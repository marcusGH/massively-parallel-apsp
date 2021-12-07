#!/usr/bin/env bash
declare -a base_url="https://www.cs.utah.edu/~lifeifei/research/tpq"
declare -a agent='--user-agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36'

declare -a files=("cal.cnode" "cal.cedge" "SD.cnode" "SF.cedge" "NA.cnode" "NA.cedge" "TG.cnode" "TG.cedge" "OL.cnode" "OL.cedge")

for f in "${files[@]}" ; do
    wget "$agent" "$base_url/$f" -P "../datasets/"
done
