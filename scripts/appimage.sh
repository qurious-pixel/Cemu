#!/bin/bash
  
curl -sSfLO "https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/linuxdeploy-x86_64.AppImage"
chmod a+x linuxdeploy*.AppImage
curl -sSfLO "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage" 
chmod a+x appimagetool*.AppImage
curl -sSfLO "https://raw.githubusercontent.com/linuxdeploy/linuxdeploy-plugin-gtk/master/linuxdeploy-plugin-gtk.sh"
chmod a+x linuxdeploy-plugin-gtk.sh
 
mkdir -p AppDir/usr/
cp ci/scripts/{cemu.desktop,cemu.png} AppDir/

mkdir -p AppDir/usr/share/applications 
mkdir -p AppDir/usr/share/icons/hicolor/scalable/apps
mkdir -p AppDir/usr/lib

cp -r bin AppDir/usr/
cp ci/scripts/cemu.sh AppDir/usr/bin/

chmod +x AppDir/usr/bin/{Cemu,cemu.sh}
chmod +x AppDir/AppRun

export UPD_INFO="gh-releases-zsync|qurious-pixel|Cemu|ci|Cemu.AppImage.zsync"
./linuxdeploy-x86_64.AppImage                   \
  --appdir="$GITHUB_WORKSPACE"/AppDir/          \
  -d "$GITHUB_WORKSPACE"/AppDir/cemu.desktop    \
  -i "$GITHUB_WORKSPACE"/AppDir/cemu.png        \
  -e "$GITHUB_WORKSPACE"/AppDir/usr/bin/Cemu	  \
  --plugin gtk

ln -sr AppDir/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders/libpixbufloader-png.so AppDir/usr/lib/libpixbufloader-png.so

./appimagetool-x86_64.AppImage "$GITHUB_WORKSPACE"/AppDir "Cemu.AppImage" -u "gh-releases-zsync|qurious-pixel|Cemu|ci|Cemu.AppImage.zsync"

mkdir -p "$GITHUB_WORKSPACE"/artifacts/ 
mv Cemu.AppImage* "$GITHUB_WORKSPACE"/artifacts/
ls -al artifacts
