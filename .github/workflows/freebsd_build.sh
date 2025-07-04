#!/usr/bin/env -S su -m root -ex

sed -i '' 's/quarterly/latest/' /etc/pkg/FreeBSD.conf
export ASSUME_ALWAYS_YES=true
pkg info # debug
pkg install llvm16
pkg install git ccache cmake ninja ffmpeg
pkg install pkgconf alsa-lib pulseaudio sdl3 evdev-proto vulkan-headers vulkan-loader opencv

cmake -B build -G Ninja
cmake --build build
