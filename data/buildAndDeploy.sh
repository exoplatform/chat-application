#!/bin/bash

SCRIPT_LAUNCH_DIR=$(pwd)
PROJECT_DIR=$(cd $(dirname "$0"); pwd)

CHT_TOMCAT_DIRECTORY=/Users/benjamin/servers/platform-community-4.0.0-SNAPSHOT
#CHT_TOMCAT_DIRECTORY=/Users/benjamin/servers/apache-tomcat-7.0.35
#CHT_TOMCAT_DIRECTORY=/Users/benjamin/servers/eXo-Platform-tomcat-3.5.6/tomcat-bundle

PLF_TOMCAT_DIRECTORY=/Users/benjamin/servers/platform-community-4.0.0-SNAPSHOT
#PLF_TOMCAT_DIRECTORY=/Users/benjamin/servers/eXo-Platform-tomcat-3.5.6/tomcat-bundle
#PLF_TOMCAT_DIRECTORY=/Users/benjamin/servers/eXo-Platform-tomcat-3.5.6-BEN/tomcat-bundle

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
rm -Rf $CHT_TOMCAT_DIRECTORY/webapps/chatServer/
cp "$PROPERTIES" $CHT_TOMCAT_DIRECTORY/conf/chat.properties
cp server/target/chatServer.war $CHT_TOMCAT_DIRECTORY/webapps/

### CLIENT
mvn clean install -Dmaven.test.skip=true -pl application/
rm -Rf $PLF_TOMCAT_DIRECTORY/webapps/chat.war
rm -Rf $PLF_TOMCAT_DIRECTORY/webapps/chat/
cp "$PROPERTIES" $PLF_TOMCAT_DIRECTORY/conf/chat.properties
#cp "$SHARED_LAYOUT" $PLF_TOMCAT_DIRECTORY/webapps/platform-extension/WEB-INF/conf/portal/portal/
cp server/target/chatServer.jar $PLF_TOMCAT_DIRECTORY/lib/
cp application/target/chat.war $PLF_TOMCAT_DIRECTORY/webapps/chat.war
