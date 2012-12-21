Chat Application
============

Chat Application using Async Ajax calls to a Chat server using MongoDB for storage.

This Chat application comes with a client project (/application) and a server project (/server).
Server can run on the same Java Application Server but as it's a near to real time application and can process a lot of data, it's recommended to use the Chat Server as a standalone application on another AS.
Thus, Chat Client portlets will run on an eXo Platform based App Server, where the Chat Server can run on a standard tomcat installation.

![alt text](https://raw.github.com/benjp/chat/master/data/screenshots/01-overview.png "Overview")


Be Up and Running : One Tomcat Server mode
===============

Step 1 :  Build 
----------------

Prerequisite : install [Maven 3](http://maven.apache.org/download.html).

    git clone https://github.com/exo-addons/chat-application.git
    cd chat-application

then build the project with maven :

    mvn clean install

Step 2 :  Configure MongoDB Server
----------------

Prerequisite : install [MongoDB](http://www.mongodb.org/downloads).
then configure your chat.properties file like :

    dbServerHost=localhost
    dbServerPort=27017
    dbName=chat
    dbAuthentication=false
    dbUser=admin
    dbPassword=pass

    chatServerUrl=http://localhost:8080/chatServer
    chatPortalPage=/portal/intranet/chat


Step 3 : Deploy 
---------------

Prerequisite : install [eXo Platform 3.5 Tomcat bundle](http://www.exoplatform.com/company/en/download-exo-platform) and rename it `tomcat/`

    cp data/chat.properties tomcat/conf/
    cp server/target/chatServer.jar tomcat/lib/
    cp server/target/chatServer.war tomcat/webapps/
    cp application/target/chat.war tomcat/webapps/

Step 4 : Run
------------

Use eXo start script :

    cd tomcat 
    ./start_eXo.sh


Now, point your browser to [http://localhost:8080/portal/intranet/](http://localhost:8080/portal/intranet/) and login with `john/gtn`


You will then 2 new applications you can find in the Applications section.

Upcoming
------------

An eXo extension will be provided soon as a starter kit to auto-deploy the apps in eXo.




Be Up and Running : Two Tomcat Servers mode
===============

To be sure, the Chat will never interfere with eXo Platform, you can install it on a separated server. It's of course the recommended configuration.

Step 1 :  Build 
----------------

Prerequisite : install [Maven 3](http://maven.apache.org/download.html).

    git clone https://github.com/exo-addons/chat-application.git
    cd chat-application

then build the project with maven :

    mvn clean install

Step 2 :  Configure MongoDB Server
----------------

Prerequisite : install [MongoDB](http://www.mongodb.org/downloads).
then configure your chat.properties file like :

    dbServerHost=localhost
    dbServerPort=27017
    dbName=chat
    dbAuthentication=false
    dbUser=admin
    dbPassword=pass

    chatServerUrl=http://localhost:8888/chatServer
    chatPortalPage=/portal/intranet/chat

You can notice here, we call Chat Server on port 8888.


Step 3 : Configure Chat Server on Tomcat
---------------

Step 3a : Configuration
-------------

Prerequisite : install [Apache Tomcat 7](http://tomcat.apache.org/download-70.cgi) and rename it `chat-server/`

Change the conf/server.xml to port 8280 :
 
<pre><code>
    &lt;Connector port="8280" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8243" URIEncoding="UTF-8" /&gt;

    &lt;Connector port="8209" protocol="AJP/1.3" redirectPort="8243" URIEncoding="UTF-8" /&gt;

</code></pre>


Step 3b : Deploy
-------------

    cp data/chat.properties chat-server/conf/
    cp server/target/chatServer.war chat-server/webapps/

Step 3c : Run
-------------

    cd chat-server
    ./bin/catalina.sh run


Step 4 : Deploy Chat Application
---------------

Step 4a : Deploy
-------------

Prerequisite : install [eXo Platform 3.5 Tomcat bundle](http://www.exoplatform.com/company/en/download-exo-platform) and rename it `tomcat/`

    cp data/chat.properties tomcat/conf/
    cp server/target/chatServer.jar tomcat/lib/
    cp application/target/chat.war tomcat/webapps/

Step 4b : Run
-------------

Use eXo start script :

    cd tomcat 
    ./start_eXo.sh

Step 5 : Virtualhosts
---------------

The Chat Application is using extensive use of Ajax calls. For this to run in multiple servers mode, the client must call the server on the same domain name and same protocol (that's the easier way to avoid anti-fishing security in modern browsers).

This easiest way to do so is to use Apache to re-route the requests using ProxyPass module.

In your Apache, configure some Proxy Pass rules, here's an example :

<pre><code>
&lt;VirtualHost *:8888&gt;
  ServerName www.localhost.com

  ProxyRequests Off
  ProxyPreserveHost On

  &lt;Proxy *&gt;
    Order deny,allow
    Allow from all
  &lt;/Proxy&gt;

  ProxyPass /chatServer http://localhost:8280/chatServer
  ProxyPassReverse /chatServer http://localhost:8280/chatServer
  ProxyPass / http://localhost:8080/
  ProxyPassReverse / http://localhost:8080/
&lt;/VirtualHost&gt;

</code></pre>



Step 6 : Login
---------------

Now, point your browser to [http://localhost:8888/portal/intranet/](http://localhost:8888/portal/intranet/) and login with `john/gtn`


You will then 2 new applications you can find in the Applications section.


License
===============

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.

