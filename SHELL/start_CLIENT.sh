#!/bin/sh

################################


killall zita-j2n
killall qjackctl
killall pd
killall sclang
killall scsynth

sleep 2

################################

zita-j2n --16bit --chan 1 192.168.0.100 4401 &

qjackctl &

# pd ~/concert/sprawl/PD/movement_1.pd &

sleep 2

sclang ~/concert/sprawl/SC/sprawl_REMOTE_silent-green.sc &

sleep 6

jmess -D -c config/client.jmess 
