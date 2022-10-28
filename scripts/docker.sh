#!/bin/bash -ex

cmake -S . -B build -DCMAKE_BUILD_TYPE=release -DENABLE_VCPKG=OFF -DPORTABLE=OFF -G Ninja
cmake --build build

mv bin/Cemu_release bin/Cemu 

chmod +x dist/linux/appimage.sh && dist/linux/appimage.sh
