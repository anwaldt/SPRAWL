#!/bin/sh

export QT_QPA_EGLFS_KMS_CONFIG=$HOME/.eglfs.json
export QT_IM_MODULE=qtvirtualkeyboard
export QT_QPA_EGLFS_ALWAYS_SET_MODE="1"
exec $HOME/sprawl/bin/sprawl-compositor --platform eglfs &
unset QT_IM_MODULE
