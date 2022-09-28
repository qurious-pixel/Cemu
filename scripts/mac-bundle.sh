#!/bin/bash

set -x 

APPNAME=cemu
APPBUNDLE=$APPNAME.app
APPBUNDLECONTENTS=$APPBUNDLE/Contents
APPBUNDLEEXE=$APPBUNDLECONTENTS/MacOS
APPBUNDLERESOURCES=$APPBUNDLECONTENTS/Resources
APPBUNDLEICON=$APPBUNDLECONTENTS/Resources

mkdir dist/mac/$APPNAME.iconset
sips -z 16 16     dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_16x16.png
sips -z 32 32     dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_16x16@2x.png
sips -z 32 32     dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_32x32.png
sips -z 64 64     dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_32x32@2x.png
sips -z 128 128   dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_128x128.png
sips -z 256 256   dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_128x128@2x.png
sips -z 256 256   dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_256x256.png
sips -z 512 512   dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_256x256@2x.png
sips -z 512 512   dist/mac/$APPNAME.png --out dist/mac/$APPNAME.iconset/icon_512x512.png
cp dist/mac/$APPNAME.png dist/mac/$APPNAME.iconset/icon_512x512@2x.png
iconutil -c icns -o dist/mac/$APPNAME.icns dist/mac/$APPNAME.iconset
	
mkdir $APPBUNDLE
mkdir $APPBUNDLE/Contents
mkdir $APPBUNDLE/Contents/MacOS
mkdir $APPBUNDLE/Contents/Resources
cp dist/mac/Info.plist $APPBUNDLECONTENTS/
#cp dist/mac/PkgInfo $APPBUNDLECONTENTS/
cp dist/mac/$APPNAME.icns $APPBUNDLEICON/
cp bin/$APPNAME $APPBUNDLEEXE/$APPNAME
otool -L bin/$APPNAME
otool -l bin/$APPNAME | grep -A 2 LC_RPATH  | tail -n 1 | awk '{print $2}' | dylibbundler -od -b -x  $APPBUNDLEEXE/$APPNAME -d $APPBUNDLECONTENTS/libs
