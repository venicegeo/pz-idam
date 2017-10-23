#!/bin/bash

APP=pz-idam
EXT=jar
ARTIFACT_STORAGE_URL=https://nexus.devops.geointservices.io/content/repositories/Piazza-Group/
sed -i "s,\${env.ARTIFACT_STORAGE_URL},$ARTIFACT_STORAGE_URL,g" pom.xml
