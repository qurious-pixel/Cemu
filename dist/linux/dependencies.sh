#!/bin/bash

CUBEBHASH=2071354a69aca7ed6df3b4222e305746c2113f60
ZSTDVER=1.5.5
PugiXMLVER=1.11.4
IMGUIVER=1.89.4
SDL2VER=2.26.5
GLSLANGVER=12.1.0
LIBZIPVER=1.9.2
WXVER=3.2.2.1
FMTVER=9.1.0
BOOSTVER=1_81_0
STATIC_LLVM_VER=16.0.2


####################
# "Create cache dir"
if [[ ! -e ${CACHEDIR} ]]; then mkdir ${CACHEDIR}; fi
ls -al ${CACHEDIR}

####################
# "Install llvm"
cd ${CACHEDIR}
if [[ ! -e llvm-project-${STATIC_LLVM_VER}.src ]]; then
	rm -r llvm-*/
	curl -sSfLO https://github.com/llvm/llvm-project/releases/download/llvmorg-${STATIC_LLVM_VER}/llvm-project-${STATIC_LLVM_VER}.src.tar.xz
	tar -xf llvm-project-${STATIC_LLVM_VER}.src.tar.xz
	rm llvm-project-${STATIC_LLVM_VER}.src.tar.xz
	cd llvm-project-${STATIC_LLVM_VER}.src/llvm
	cmake -S . -B build -GNinja -DLLVM_ENABLE_CXX1Y=ON -DLLVM_TARGETS_TO_BUILD="X86" -DCMAKE_BUILD_TYPE=Release -DLLVM_USE_INTEL_JITEVENTS=ON -DLLVM_USE_PERF=ON \
		-DLLVM_BUILD_RUNTIME=OFF -DLLVM_BUILD_TOOLS=OFF -DLLVM_INCLUDE_DOCS=OFF -DLLVM_INCLUDE_EXAMPLES=OFF -DLLVM_INCLUDE_TESTS=OFF -DLLVM_INCLUDE_TOOLS=OFF \
		-DLLVM_INCLUDE_UTILS=OFF -DLLVM_INCLUDE_BENCHMARKS=OFF -DWITH_POLLY=OFF -DLLVM_ENABLE_Z3_SOLVER=OFF -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++
	cmake --build build
	cd ${CACHEDIR} 
fi
sudo ninja -C llvm-project-${STATIC_LLVM_VER}.src/llvm/build install

####################
# "Install cubeb"
cd ${CACHEDIR}
if [[ ! -e cubeb-${CUBEBHASH} ]]; then
	rm -r cubeb-*/
	git clone https://github.com/mozilla/cubeb cubeb-${CUBEBHASH}   
	cd cubeb-${CUBEBHASH}
	git checkout ${CUBEBHASH}
	git submodule update --init --recursive
	cmake -B ./build .  
	cmake --build ./build
	cd ..
fi
sudo make -C cubeb-${CUBEBHASH}/build install

####################
# "Install ZSTD"
cd ${CACHEDIR}
if [[ ! -e zstd-${ZSTDVER} ]]; then
    rm -r zstd-*/
    curl -sSfLO https://github.com/facebook/zstd/releases/download/v${ZSTDVER}/zstd-${ZSTDVER}.tar.gz
    tar xf zstd-${ZSTDVER}.tar.gz
    rm zstd-${ZSTDVER}.tar.gz
    cd zstd-${ZSTDVER}/build/cmake
    mkdir builddir && cd builddir
    cmake .. -DCMAKE_INSTALL_PREFIX="/usr"
    make && cd ${CACHEDIR}
fi
sudo make -C zstd-${ZSTDVER} install

####################
# "Install PugiXML"
cd ${CACHEDIR}
if [[ ! -e pugixml-${PugiXMLVER} ]]; then
    rm -r pugixml-*/
    curl -sSfLO https://github.com/zeux/pugixml/releases/download/v${PugiXMLVER}/pugixml-${PugiXMLVER}.tar.gz
    tar xf pugixml-${PugiXMLVER}.tar.gz
    rm pugixml-${PugiXMLVER}.tar.gz
    cd pugixml-${PugiXMLVER}
    cmake -S . -B build
    cmake --build build
    cd ..
fi
sudo make -C pugixml-${PugiXMLVER}/build install

####################
# "Install imgui"
cd ${CACHEDIR}
if [[ ! -e imgui-${IMGUIVER} ]]; then
    rm -r imgui-*/
    curl -sSfL https://codeload.github.com/ocornut/imgui/tar.gz/v${IMGUIVER} -o imgui-${IMGUIVER}
    curl -sSfL https://github.com/ocornut/imgui/archive/v${IMGUIVER}.tar.gz -o imgui-${IMGUIVER}
    tar xf imgui-${IMGUIVER}
    cd imgui-${IMGUIVER}
    curl -sSfLO https://raw.githubusercontent.com/microsoft/vcpkg/master/ports/imgui/CMakeLists.txt
    curl -sSfLO https://raw.githubusercontent.com/microsoft/vcpkg/master/ports/imgui/imgui-config.cmake.in
    cmake -DCMAKE_INSTALL_PREFIX=/usr -DBUILD_SHARED_LIBS=ON -S. -B cmake-build-shared
    cmake --build cmake-build-shared
    cd ../
fi
sudo make -C imgui-${IMGUIVER}/cmake-build-shared install

####################
# "Install SDL2"
cd ${CACHEDIR}
if [[ ! -e SDL2-${SDL2VER} ]]; then
    rm -r SDL2-*/
    curl -sLO https://libsdl.org/release/SDL2-${SDL2VER}.tar.gz
    tar -xzf SDL2-${SDL2VER}.tar.gz
    rm SDL2-${SDL2VER}.tar.gz
    cd SDL2-${SDL2VER}
    ./configure --prefix=/usr
    make && cd ../
fi  
sudo make -C SDL2-${SDL2VER} install
sdl2-config --version

####################
# "Install glslang"
cd ${CACHEDIR}
if [[ ! -e glslang-${GLSLANGVER} ]]; then
    rm -r glslang-*/
    curl -sSfL https://github.com/KhronosGroup/glslang/archive/refs/tags/${GLSLANGVER}.tar.gz -o glslang-${GLSLANGVER}
    tar xf glslang-${GLSLANGVER}
    cd glslang-${GLSLANGVER}
    mkdir -p build && cd build
    cmake .. -GNinja -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="/usr"
    ninja && cd ../../
fi
sudo ninja -C glslang-${GLSLANGVER}/build install
glslangValidator --version

####################
# "Install Libzip"
cd ${CACHEDIR}
if [[ ! -e libzip-${LIBZIPVER} ]]; then
	rm -r libzip-*/
    curl -sSfLO https://libzip.org/download/libzip-${LIBZIPVER}.tar.gz
    tar -xf libzip-${LIBZIPVER}.tar.gz
    rm libzip-${LIBZIPVER}.tar.gz
    cd libzip-${LIBZIPVER}
    mkdir build && cd build
    cmake .. -DCMAKE_INSTALL_PREFIX=/usr
    make && cd ../../
fi
sudo make -C libzip-${LIBZIPVER}/build install

####################
# "Install wxWidgets"
cd ${CACHEDIR}
if [[ ! -e wxWidgets-${WXVER} ]]; then
    rm -r wxWidgets-*/
    curl -sSfLO https://github.com/wxWidgets/wxWidgets/releases/download/v${WXVER}/wxWidgets-${WXVER}.tar.bz2
    tar xf wxWidgets-${WXVER}.tar.bz2
    rm wxWidgets-${WXVER}.tar.bz2
    cd wxWidgets-${WXVER}
    mkdir build-gtk3 && cd build-gtk3
    cmake ..
    make && cd ../../
fi
sudo make -C wxWidgets-${WXVER}/build-gtk3 install

####################
# "Install fmt"
cd ${CACHEDIR}
if [[ ! -e fmt-${FMTVER} ]]; then
    rm -r fmt-*/
    curl -sSfL https://github.com/fmtlib/fmt/archive/refs/tags/${FMTVER}.tar.gz -o fmt-${FMTVER}
    tar xf fmt-${FMTVER}
    cd fmt-${FMTVER}
    mkdir -p build && cd build
    cmake .. -GNinja -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="/usr"
    ninja && cd ../../
fi
sudo ninja -C fmt-${FMTVER}/build install

####################
# "Install Boost"
cd ${CACHEDIR}
if [[ ! -e boost_${BOOSTVER} ]]; then
    rm -r boost_*/
    BOOSTVER2=$(echo ${BOOSTVER} | sed s/\_/./g)
    curl -sSfLO https://boostorg.jfrog.io/artifactory/main/release/${BOOSTVER2}/source/boost_${BOOSTVER}.tar.gz
    tar xf boost_${BOOSTVER}.tar.gz
    rm boost_${BOOSTVER}.tar.gz
    cd boost_${BOOSTVER}
    ./bootstrap.sh --prefix=/usr --with-toolset=clang --with-libraries=container,program_options,nowide,random,filesystem
    cd ..
fi
cd boost_${BOOSTVER}
sudo ./b2 --build-typ=minimal install