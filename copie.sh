#/Users/benjamin/java/demos/apache-tomcat-7.0.30

### CLIENT
rm -Rf ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/chat/
cp server/target/chatServer.jar ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/lib/
cp application/target/chat.war ~/java/demos/eXo-Platform-tomcat-3.5.4/tomcat-bundle/webapps/


### SERVER
rm -Rf ~/java/demos/apache-tomcat-7.0.30/webapps/chatServer/
cp server/target/chatServer.war ~/java/demos/apache-tomcat-7.0.30/webapps/

