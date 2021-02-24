#!/bin/sh

killall jacktrip
killall jackd

FILE=~/.jacktrip_remotename

if test -f "$FILE"; then
	echo "Your jacktrip remote name is: "
	NAME=$(cat $FILE)
	echo $NAME
else
	NAME=$(zenity --entry --text "Please choose a JackTrip client name:" --title "Who are you?");
	echo $NAME > $FILE
fi

OUTCHANS=$(zenity --list --radiolist \
--text="Please select the number of outgoing channels to the network!" \
--column="Select" --column="Outgoing Channels"  FALSE "1" TRUE "2");


# sleep 1

# knock wintermute.ak.tu-berlin.de 4464:udp 4466:udp 61000:udp 4464:tcp  -d 10 &&

sleep 1

jackd -P 90 -d alsa -d hw:1,0 -r 48000 -p 256 &

sleep 2

bin/jacktrip_nils -C 85.214.78.6 --numincoming 2 --numoutgoing $OUTCHANS -K AP_$NAME
