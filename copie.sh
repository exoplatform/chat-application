cp server/target/chatServer.jar ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/lib/
rm -Rf ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/chatServer/
cp server/target/chatServer.war ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/
rm -Rf ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/chat/
cp application/target/chat.war ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/
