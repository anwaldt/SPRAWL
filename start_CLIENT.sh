#!/bin/sh

################################

killall pd
killall sclang
killall scsynth

sleep 2

################################

pd PD/movement_1.pd &

sclang SC/sprawl_REMOTE_silent-green.sc &

sleep 6

jmess -D -c config/client.jmess 
