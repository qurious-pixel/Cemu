#!/bin/sh -ex

cd cemu || exit 1

git config --global --add safe.directory '*'


apt update -qq
apt install -y clang cmake freeglut3-dev libgcrypt20-dev libglm-dev libgtk-3-dev libpulse-dev libsecret-1-dev libsystemd-dev libudev-dev nasm ninja-build libbluetooth-dev


bash /cemu/dependencies/vcpkg/bootstrap-vcpkg.sh

cmake -S . -B build  -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=/usr/bin/clang -DCMAKE_CXX_COMPILER=/usr/bin/clang++ -G Ninja -DCMAKE_MAKE_PROGRAM=/usr/bin/ninja
cmake --build build

# mv bin/Cemu_release bin/Cemu

