#!/bin/sh

install -o root -g root -m a+r,u+w *.service /usr/lib/systemd/user/
