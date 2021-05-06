#!/bin/sh

install -o root -g root -m a+r,u+w *.service /usr/lib/systemd/user/
install -o root -g root -m a+r,u+w jack-matchmaker /etc/conf.d/
