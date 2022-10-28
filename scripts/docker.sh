#!/bin/bash -ex

yum install -y ccache

cmake -S . -B build -DCMAKE_BUILD_TYPE=release -DCMAKE_C_COMPILER=/usr/lib64/ccache -DCMAKE_CXX_COMPILER=/usr/lib64/ccache -DENABLE_VCPKG=OFF -DPORTABLE=OFF -G Ninja
cmake --build build

mv bin/Cemu_release bin/Cemu 

chmod +x dist/linux/appimage.sh && dist/linux/appimage.sh
