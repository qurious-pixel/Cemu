#!/bin/bash

if [[ ! -e "$HOME/.config/cemu" ]]; then 
	cp -r "$APPDIR/usr/bin/" "$HOME/.config/cemu"
fi

cp "$APPDIR/usr/bin/Cemu" "$HOME/.config/cemu/Cemu"

"$HOME/.config/cemu/Cemu"
