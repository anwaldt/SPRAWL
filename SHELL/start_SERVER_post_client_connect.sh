#!/bin/sh

killall PanoramixApp
killall ardour5

sleep 2

PanoramixApp -b 256 -i 32 -o 32 -L config/panoramix_8-4.txt &

sleep 6

#ardour5 ../ssss/ssss.ardour &

#sleep 6

jmess -D -c config/silent_green_ZITA.jmess

