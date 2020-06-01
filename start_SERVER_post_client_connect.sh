#!/bin/sh

PanoramixApp -L config/panoramix_EN324.txt &
sleep 10
ardour5 ../ssss/ssss.ardour &
sleep 10
jmess -D -c config/jmess_session_2020_01_08.xml

