#!/bin/bash -ex

pushd `dirname $0`/.. > /dev/null
root=$(pwd -P)
popd > /dev/null

# gather some data about the repo
source $root/ci/vars.sh

# Copy JKS
[ -z "$JKS" ] || mv $JKS $root/src/main/resources/pz-idam.jks

# Path to output JAR
src=$root/target/piazza-idam*.jar

# Build Spring-boot JAR
[ -f $src ] || mvn clean package -U

# stage the artifact for a mvn deploy
mv $src $root/$APP.$EXT

[ -z "$JKS" ] || rm $root/src/main/resources/pz-idam.jks
