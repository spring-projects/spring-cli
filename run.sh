#!/bin/sh
tmpDir="$(mktemp -d)"
sbmDir="$tmpDir/spring-boot-migrator"
exampleAppDir="$tmpDir/demo-spring-song-app"

echo "$tmpDir"

pushd "$tmpDir"
  git clone https://github.com/sanagaraj-pivotal/demo-spring-song-app
  git clone https://github.com/spring-projects-experimental/spring-boot-migrator.git
  pushd spring-boot-migrator
    git checkout a2636fb
  popd
  pushd spring-boot-migrator/sbm-support-rewrite
    mvn clean install -DskipTests
  popd
popd

./gradlew clean build -x test

cp ./build/libs/spring-cli-0.0.1-SNAPSHOT.jar  "$exampleAppDir"

#pushd $exampleAppDir
#  java -jar spring-cli-0.0.1-SNAPSHOT.jar boot upgrade
#popd

#rm -rf $tmpDir
#cd /tmp
#
#git clone
#
#pushd