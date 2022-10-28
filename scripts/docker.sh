#!/bin/bash -ex

cmake -S . -B build -DCMAKE_BUILD_TYPE=release -DENABLE_VCPKG=OFF -DPORTABLE=OFF -G Ninja
cmake --build build
