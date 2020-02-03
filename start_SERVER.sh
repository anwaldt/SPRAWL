#!/bin/sh

################################

killall zita-n2j
killall jacktrip
killall sclang
killall scsynth
killall qjackctl
killall jackd
killall pd

sleep 2

sudo cpupower frequency-set -g performance

################################

jackd -P 95 -d alsa -d hw:MADIFXtest -p 128 -n 2 & 

sleep 2

# rm log/SC.log

sclang SC/sprawl_SERVER.sc > log/SC.log &


sleep 4

# ~/Development/jacktrip-recent/src/jacktrip -S --nojackportsconnect -p 3 -n 1 > log/jacktrip.log &

# ~/Development/jacktrip-github/jacktrip/build-jacktrip-Desktop-Release/jacktrip -S --nojackportsconnect -p 3 -n 1 -V > log/jacktrip.log &

qjackctl &

zita-n2j --jname PI_01 --chan 1 192.168.0.100 4401 &
sleep 4
zita-n2j --jname PI_02 --chan 1 192.168.0.100 4422 &
sleep 4
zita-n2j --jname PI_03 --chan 1 192.168.0.100 4431 &
sleep 4
zita-n2j --jname PI_04 --chan 1 192.168.0.100 4434 &
sleep 4
zita-n2j --jname PI_05 --chan 1 192.168.0.100 4455 &
sleep 4
zita-n2j --jname PI_06 --chan 1 192.168.0.100 4461 &
sleep 4
zita-n2j --jname PI_07 --chan 1 192.168.0.100 4471 &
sleep 4
zita-n2j --jname PI_08 --chan 1 192.168.0.100 4408 &
sleep 4
zita-n2j --jname PI_09 --chan 1 192.168.0.100 4419 &
sleep 4
zita-n2j --jname PI_10 --chan 1 192.168.0.100 4410 &


# qjackctl &
