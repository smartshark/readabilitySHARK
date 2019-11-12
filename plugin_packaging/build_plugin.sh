#!/bin/bash

current=`pwd`
mkdir -p /tmp/readabilitySHARK/
cp -R ../src /tmp/readabilitySHARK/
cp -R ../libs /tmp/readabilitySHARK/

cp ../libs/readability.classifier /tmp/readabilitySHARK/

cp ../build.gradle /tmp/readabilitySHARK/
cp ../gradlew /tmp/readabilitySHARK/
cp ../gradlew.bat /tmp/readabilitySHARK/
cp -R ../gradle /tmp/readabilitySHARK/

cp * /tmp/readabilitySHARK
cd /tmp/readabilitySHARK/

tar -cvf "$current/readabilitySHARK_plugin.tar" --exclude=*.tar --exclude=build_plugin.sh *