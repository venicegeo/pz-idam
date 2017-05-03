#!/bin/bash -ex

root=$(pwd -P)
mkdir -p $root/.m2/repository

mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Pcoverage-per-test org.jacoco:jacoco-maven-plugin:report -DdataFile=$root/target/jacoco.exec -Dmaven.repo.local="$root/.m2/repository"
