#!/bin/sh

killall pd

################################

pd -noaudio -nogui PD/movement_1.pd &
pd -noaudio -nogui PD/movement_2.pd &
pd -noaudio -nogui PD/movement_3.pd &
pd -noaudio -nogui PD/movement_4.pd &
pd -noaudio -nogui PD/movement_5.pd &
pd -noaudio -nogui PD/movement_6.pd &
pd -noaudio -nogui PD/movement_7.pd &
pd -noaudio -nogui PD/movement_8.pd &
pd -noaudio -nogui PD/movement_9.pd &
pd -noaudio -nogui PD/movement_10.pd &
