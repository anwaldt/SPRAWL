#!/bin/bash

EXT="~/.local/share/SuperCollider/Extensions/SPRAWL/"

if [ -d "$EXT" ]; then
    echo "$EXT exists."
else
  mkdir ~/.local/share/SuperCollider/Extensions/SPRAWL/
fi

cp  *.sc ~/.local/share/SuperCollider/Extensions/SPRAWL/
