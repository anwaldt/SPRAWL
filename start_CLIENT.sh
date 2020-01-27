#!/bin/sh

################################

killall sclang
killall scsynth

sleep 2

################################

sclang SC/sprawl_REMOTE_silent-green.sc &

sleep 6

jmess -D -c config/client.jmess 
