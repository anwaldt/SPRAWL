#!/bin/sh

PanoramixApp -L config/panoramix_8-4.txt &

sleep 6

ardour5 ../ssss/ssss.ardour &

sleep 6

jmess -D -c config/jmess_session_2020_01_08.xml

