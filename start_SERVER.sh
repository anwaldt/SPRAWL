#!/bin/sh

################################


killall jacktrip
killall sclang
killall scsynth
killall jackd
killall qjackctl

sleep 2

################################

rm log/SC.log

sclang SC/sprawl_SERVER.sc > log/SC.log &

sleep 4

#~/Development/jacktrip-recent/src/jacktrip -S --nojackportsconnect -p 3 -n 1 > log/jacktrip.log &

~/Development/jacktrip-github/jacktrip/build-jacktrip-Desktop-Release/jacktrip -S --nojackportsconnect -p 3 -n 1 &

qjackctl &
