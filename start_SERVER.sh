#!/bin/sh

################################

killall jacktrip
killall sclang
killall scsynth
sleep 2
################################

rm log/SC.log

sclang SC/sprawl_SERVER.sc > log/SC.log &

~/Development/jacktrip-recent/src/jacktrip -S -p 3 -n 1 > log/jacktrip.log &
