#!/bin/sh

################################

killall jacktrip
killall sclang
killall scsynth

################################

rm log/SC.log

sclang SC/sprawl_SERVER.sc > log/SC.log &

for i in $(seq 0 15); do
    rm log/poe_$i'.log'
    ~/jacktrip-master-e85fb18fd143ced04384dd43bbac3fa9207cffd8/src/jacktrip -s -o $i -n 1 --clientname POE_$i > log/poe_$i'.log' &
done
