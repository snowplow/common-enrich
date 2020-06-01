#!/bin/bash

# SCE is published to snowplow-maven which doesn't work with release-manager hence a specific script

tag=$1
project_version=$(sbt "show version" | tail -n 1 | awk '{print $2}')

if [ "${project_version}" != "${tag}" ]; then
    echo "Tag '${tag}' doesn't match version of the project ('${project_version}'). Aborting!"
    exit 1
fi

mkdir ~/.bintray/
FILE=$HOME/.bintray/.credentials
cat <<EOF >$FILE
realm = Bintray API Realm
host = api.bintray.com
user = $BINTRAY_SNOWPLOW_MAVEN_USER
password = $BINTRAY_SNOWPLOW_MAVEN_API_KEY
EOF

sbt +publish
sbt +bintraySyncMavenCentral
