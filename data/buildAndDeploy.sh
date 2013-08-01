#!/bin/bash

SCRIPT_LAUNCH_DIR=$(pwd)
PROJECT_DIR=$(cd $(dirname "$0"); pwd)

CHT_TOMCAT_DIRECTORY=/Users/benjamin/servers/platform-4.0.1
#CHT_TOMCAT_DIRECTORY=/Users/benjamin/servers/apache-tomcat-7.0.35

PLF_TOMCAT_DIRECTORY=/Users/benjamin/servers/platform-4.0.1

if [ $# = 1 ]
then
  PROPERTIES=$1
else
  PROPERTIES="data/chat.properties"
fi
SHARED_LAYOUT="data/sharedLayout.xml"

cd $PROJECT_DIR/..

### SERVER
mvn clean install -Dmaven.test.skip=true -pl server/
rm -Rf $CHT_TOMCAT_DIRECTORY/webapps/chatServer.war
#rm -Rf $CHT_TOMCAT_DIRECTORY/webapps/chatServer/
cp "$PROPERTIES" $CHT_TOMCAT_DIRECTORY/conf/chat.properties
cp server/target/chatServer.war $CHT_TOMCAT_DIRECTORY/webapps/

### CLIENT
mvn clean install -Dmaven.test.skip=true -pl application/
rm -Rf $PLF_TOMCAT_DIRECTORY/webapps/chat.war
#rm -Rf $PLF_TOMCAT_DIRECTORY/webapps/chat/
cp "$PROPERTIES" $PLF_TOMCAT_DIRECTORY/conf/chat.properties
#cp "$SHARED_LAYOUT" $PLF_TOMCAT_DIRECTORY/webapps/platform-extension/WEB-INF/conf/portal/portal/
#cp server/target/chatServer.jar $PLF_TOMCAT_DIRECTORY/lib/
cp application/target/chat.war $PLF_TOMCAT_DIRECTORY/webapps/chat.war

### EXTENSION
#mvn clean install -Dmaven.test.skip=true -pl exo-addons-chat-extension-config
#cp exo-addons-chat-extension-config/target/exo-addons-chat-extension-config-0.7.0-beta2-SNAPSHOT.jar $PLF_TOMCAT_DIRECTORY/lib/
#mvn clean install -Dmaven.test.skip=true -pl exo-addons-chat-extension
#cp exo-addons-chat-extension/target/chat-extension.war $PLF_TOMCAT_DIRECTORY/webapps/
