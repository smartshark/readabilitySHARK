#!/bin/sh
PLUGIN_PATH=$1

cd $PLUGIN_PATH

# Build jar file
./gradlew clean assemble || exit 1

cp ./build/libs/readabilitySHARK.jar ./