#!/bin/bash -ex

yum install -y vulkan-dev libglvnd-opengl

cmake -S . -B build -DCMAKE_BUILD_TYPE=release -DENABLE_VCPKG=OFF -DPORTABLE=OFF -G Ninja
cmake --build build
