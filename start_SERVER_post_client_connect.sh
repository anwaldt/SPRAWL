#!/bin/sh

PanoramixApp -L config/panoramix_8-4.txt &

sleep 6

ardour5 ../ssss/ssss.ardour &

sleep 6

jmess -D -c config/silent_green.jmess

