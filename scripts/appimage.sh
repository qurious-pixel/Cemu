#!/bin/bash

if [ -z "${GITHUB_WORKSPACE}" ]; then
    export GITHUB_WORKSPACE="."
fi
  
curl -sSfLO "https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/linuxdeploy-x86_64.AppImage"
chmod a+x linuxdeploy*.AppImage
curl -sSfL https://github.com$(curl https://github.com/probonopd/go-appimage/releases | grep "mkappimage-.*-x86_64.AppImage" | head -n 1 | cut -d '"' -f 2) -o mkappimage.AppImage
chmod a+x mkappimage.AppImage
curl -sSfLO "https://raw.githubusercontent.com/linuxdeploy/linuxdeploy-plugin-gtk/master/linuxdeploy-plugin-gtk.sh"
chmod a+x linuxdeploy-plugin-gtk.sh
 
mkdir -p AppDir/usr/
cp dist/linux/{info.cemu.Cemu.desktop,info.cemu.Cemu.png} AppDir/

if [[ -e dist/linux/info.cemu.Cemu.metainfo.xml ]]; then
	mkdir -p "$GITHUB_WORKSPACE"/AppDir/usr/share/metainfo
	cp dist/linux/info.cemu.Cemu.metainfo.xml "$GITHUB_WORKSPACE"/AppDir/usr/share/metainfo/
fi

mkdir -p AppDir/usr/share/applications 
mkdir -p AppDir/usr/share/icons/hicolor/scalable/apps
mkdir -p AppDir/usr/lib

cp -r bin AppDir/usr/
#cp .ci/cemu.sh AppDir/usr/bin/

chmod +x AppDir/usr/bin/cemu    # {Cemu,cemu.sh}
#chmod +x AppDir/AppRun

export UPD_INFO="gh-releases-zsync|cemu-project|Cemu|ci|Cemu.AppImage.zsync"
./linuxdeploy-x86_64.AppImage                   		\
  --appdir="$GITHUB_WORKSPACE"/AppDir/          		\
  -d "$GITHUB_WORKSPACE"/AppDir/info.cemu.Cemu.desktop  \
  -i "$GITHUB_WORKSPACE"/AppDir/info.cemu.Cemu.png      \
  -e "$GITHUB_WORKSPACE"/AppDir/usr/bin/cemu			\
  --plugin gtk

VERSION=2.0 ./mkappimage.AppImage "$GITHUB_WORKSPACE"/AppDir

mkdir -p "$GITHUB_WORKSPACE"/artifacts/ 
mv Cemu-2.0-x86_64.AppImage "$GITHUB_WORKSPACE"/artifacts/
ls -al artifacts
