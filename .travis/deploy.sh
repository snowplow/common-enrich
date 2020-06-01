#!/bin/bash

# SCE is published to snowplow-maven which doesn't work with release-manager hence a specific script

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
