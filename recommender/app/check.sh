#!/bin/bash

CONFIG="test_config.yml"

startdate=$1
enddate=$2

sDateTs=`date -j -f "%d_%m_%Y" $startdate "+%s"`
eDateTs=`date -j -f "%d_%m_%Y" $enddate "+%s"`
dateTs=$sDateTs
offset=86400

while [ "$dateTs" -le "$eDateTs" ]
do
  date=`date -j -f "%s" $dateTs "+%d_%m_%Y"`
  printf '%s\n' $date
  java -jar "target/binder-0.0.1-SNAPSHOT.jar" --check --checkDate=$date #--config="$CONFIG" 
  dateTs=$(($dateTs+$offset))
done
