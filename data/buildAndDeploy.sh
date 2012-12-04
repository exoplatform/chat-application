#!/bin/bash

SCRIPT_LAUNCH_DIR=$(pwd)
PROJECT_DIR=$(cd $(dirname "$0"); pwd)
#PLF_TOMCAT_DIRECTORY=/Users/benjamin/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle
#CHT_TOMCAT_DIRECTORY=/Users/benjamin/java/demos/apache-tomcat-7.0.30
PLF_TOMCAT_DIRECTORY=/Users/benjamin/java/demos/eXo-Platform-tomcat-3.5.5/tomcat-bundle
CHT_TOMCAT_DIRECTORY=/Users/benjamin/java/demos/eXo-Platform-tomcat-3.5.5/tomcat-bundle

if [ $# = 1 ]
then
  PROPERTIES=$1
else
  PROPERTIES="chat.properties"
fi

cd /Users/benjamin/git/chat

### SERVER
mvn clean install -Dmaven.test.skip=true -pl server/
rm -Rf $CHT_TOMCAT_DIRECTORY/webapps/chatServer.war
rm -Rf $CHT_TOMCAT_DIRECTORY/webapps/chatServer/
cp "data/$PROPERTIES" $CHT_TOMCAT_DIRECTORY/conf/chat.properties
cp server/target/chatServer.war $CHT_TOMCAT_DIRECTORY/webapps/

### CLIENT
mvn clean install -Dmaven.test.skip=true -pl application/
rm -Rf $PLF_TOMCAT_DIRECTORY/webapps/chat.war
rm -Rf $PLF_TOMCAT_DIRECTORY/webapps/chat/
cp "data/$PROPERTIES" $PLF_TOMCAT_DIRECTORY/conf/chat.properties
cp server/target/chatServer.jar $PLF_TOMCAT_DIRECTORY/lib/
cp application/target/chat.war $PLF_TOMCAT_DIRECTORY/webapps/
