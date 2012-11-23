#!/bin/bash

if [ $# = 1 ]
then
  properties=$1
else
  properties="chat.properties"
fi

cd /Users/benjamin/git/chat

### SERVER
mvn clean install -Dmaven.test.skip=true -pl server/
rm -Rf ~/java/demos/apache-tomcat-7.0.30/webapps/chatServer.war
rm -Rf ~/java/demos/apache-tomcat-7.0.30/webapps/chatServer/
cp "data/$properties" ~/java/demos/apache-tomcat-7.0.30/conf/chat.properties
cp server/target/chatServer.war ~/java/demos/apache-tomcat-7.0.30/webapps/

### CLIENT
mvn clean install -Dmaven.test.skip=true -pl application/
rm -Rf ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/chat.war
rm -Rf ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/chat/
cp "data/$properties" ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/conf/chat.properties
cp server/target/chatServer.jar ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/lib/
cp application/target/chat.war ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/



