#!/bin/bash

#start pd patch

pd -jack PD/Live_Grain/Live_Granular.pd &

# connect inputs
sleep 10
jmess -D -c config/mario.jmess


