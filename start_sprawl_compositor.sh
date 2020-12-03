#!/bin/sh

pidof sprawl-compositor
if [ $? -eq 1 ]
then
  export QT_QPA_EGLFS_KMS_CONFIG=$HOME/.eglfs.json
  export QT_IM_MODULE=qtvirtualkeyboard
  export QT_QPA_EGLFS_ALWAYS_SET_MODE="1"
  exec $HOME/sprawl/bin/sprawl-compositor --platform eglfs &
  unset QT_IM_MODULE
fi

export QT_WAYLAND_SHELL_INTEGRATION=ivi-shell
export QT_WAYLAND_DISABLE_WINDOWDECORATION=1
export QT_QPA_PLATFORM=wayland
