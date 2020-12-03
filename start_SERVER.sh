#!/bin/sh

################################

killall jacktrip
killall sclang
killall scsynth
sleep 2
################################

rm log/SC.log

sclang SC/sprawl_SERVER.sc > log/SC.log &

~/Development/jacktrip/builddir/jacktrip -S -p 5 -D --udprt > log/jacktrip.log &
jack-matchmaker -p matchmaker/sprawl_server.pattern &

