#!/usr/bin/env -S su -m root -ex

sed -i '' 's/quarterly/latest/' /etc/pkg/FreeBSD.conf
export ASSUME_ALWAYS_YES=true
pkg info # debug #
pkg install -y boost-libs cmake-core curl glslang gtk3 ninja png pkgconf rapidjson wayland wayland-protocols wx32-gtk3 xorg zstd

cmake -B build -DCMAKE_BUILD_TYPE=release -DENABLE_BLUEZ=OFF -DENABLE_DISCORD_RPC=OFF -DENABLE_FERAL_GAMEMODE=OFF -DENABLE_HIDAPI=OFF -DENABLE_VCPKG=OFF -G Ninja
cmake --build build
