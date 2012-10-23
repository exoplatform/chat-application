Chat app
============

Chat Application using Server-Sent Events

This Chat application comes with a client project (/application) and a server project (/server).
Server can run on the same Java Application Server but as it's a near to real time application and can process a lot of data,
it's recommended to use the server as a standalone application on another AS.

# 2 Servers Mode w/ Apache and Tomcats

## Application Portlets

this part is really easy. Once you've build the application, you get a war file with two portlets.
Simple put this war in the eXo Platform 3.5 Tomcat webapps folder.


## Chat Server

the server standalone application can be deployed in a standard tomcat 7 bundle.
Copy the chatServer.war file in the Tomcat webapps folder.

## Virtual Hosting


The app is using extensive use of Server Sent Events and Ajax calls. For this to run, the client must call the server
on the same domain name and same protocol.

This easiest way to do so is to use Apache to re-route the requests using ProxyPass module.

### Apache configuration

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

### Tomcat

If you don't want problems with encoding, one important tip is to configure the URIEncoding on tomcat side :

conf/server.xml :

<pre><code>
    &lt;Connector port="8280" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8243" URIEncoding="UTF-8" /&gt;

    &lt;Connector port="8209" protocol="AJP/1.3" redirectPort="8243" URIEncoding="UTF-8" /&gt;

</code></pre>
