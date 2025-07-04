#!/bin/sh -ex

cd cemu || exit 1

git config --global --add safe.directory '*'


apt update -qq
apt install -y autoconf clang-15 cmake freeglut3-dev git libbluetooth-dev libgcrypt20-dev libglm-dev libgtk-3-dev libltdl-dev libpulse-dev libsecret-1-dev libsystemd-dev libudev-dev nasm ninja-build zip

cd /cemu/dependencies/vcpkg
git remote update
git pull origin master
./bootstrap-vcpkg.sh
cd /cemu

## test build
#apt update -qq
#apt install -y clang-15
#cd /cemu/dependencies/ih264d
##

cmake -S . -B build  -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=/usr/bin/clang-15 -DCMAKE_CXX_COMPILER=/usr/bin/clang++-15 -G Ninja -DCMAKE_MAKE_PROGRAM=/usr/bin/ninja
cmake --build build

# mv bin/Cemu_release bin/Cemu

