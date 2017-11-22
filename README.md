Chat Application
============

eXo Chat is an Instant Messaging application for eXo Platform 4.
Thanks to [Weemo](http://www.weemo.com) and their powerful cloud solution, it’s even possible to benefit from Video Conferencing (One to One, Conference) and Screen Sharing from within eXo Chat.

You can discuss or find help on [eXo Chat Space Community](http://community.exoplatform.com/portal/g/:spaces:chat_application/chat_application)

Technically speaking, this Chat application comes with a client project (/application) and a server project (/server). All the Chat data is stored using MongoDB for storage.
Server can run on the same Java Application Server but as it's a near to real time application and can process a lot of data, it's recommended to use the Chat Server as a standalone application on another AS.
Thus, Chat Client portlets will run on an eXo Platform based App Server, where the Chat Server can run on a standard tomcat installation.

<img src="https://raw.github.com/benjp/chat/master/data/screenshots/chat-platform4-retina.png" alt="eXo Chat Overview on Platform 4" width="800">

I wrote some blog posts on this app and technologies I use to build it, you can find them here :
- [eXo Platform Add-Ons: Chat in Space](http://blog.exoplatform.com/2012/12/11/exo-platform-add-ons-chat-in-space)
- [Chat Application: Why MongoDB?](http://blog.exoplatform.com/2012/12/18/chat-application-why-mongodb)
- [Chat in the Cloud with MongoHQ](http://blog.exoplatform.com/2012/12/20/chat-in-the-cloud-with-mongohq)
- [Video Chat in eXo Platform 4 with Weemo](http://blog.exoplatform.com/2013/05/22/video-chat-in-exo-platform-4-with-weemo)

If you simply want to build it and use it, follow the steps.

Roadmap is available in the wiki section : [Chat Roadmap](https://github.com/exo-addons/chat-application/wiki/Roadmap)

Be Up and Running
===============

One Tomcat Server mode
----------------------

Jump to Wiki Install Page : [One Server Mode](https://github.com/exo-addons/chat-application/wiki/One-Server-Mode)

Two Tomcat Servers mode
-----------------------

To be sure, the Chat will never interfere with eXo Platform, you can install it on a separated server. It's of course the recommended configuration.

Jump to Wiki Install Page : [Two Servers Mode](https://github.com/exo-addons/chat-application/wiki/Two-Servers-Mode)

Migrate to Chat 1.2
-------------------
* Before processing the migration, it's recommended to backup the data. This can be done with the following command:

	mongodump --host {host} --port {port} --username {username} --password {password} --db {dbName} --out "{backup_folder}"

with {host} and {port} are respectively hostname/IP and port of mongoDB server; {username} and {password} are required if authentication is enabled for your database; {dbName} is name of chat database and {backup_folder} is path to folder that backup data is stored.

* To migrate, you have two options:

** Automatical migration: you just need to turn off your Chat Server, deploy new chatServer.war package then restart Chat Server.

** Manual migration: to do a manual migration, you have to get the migration script - migration-chat-addon.js file - that is packaged in chatServer.war/WEB-INF/lib/server-{version}.jar. Extract binary packages to get migration script then launch the following command:

	mongo --quiet {host}:{port}/{dbName} -u {username} -p {password} migration-chat-addon.js

with {host} and {port} are respectively hostname/IP and port of mongoDB server; {username} and {password} are required if authentication is enabled for your database; {dbName} is name of database that is configured in chat.properties.

Testing with Docker
===============

prerequisite : you need Docker 1.10+ and Docker Compose 1.6+

You can use Docker to develop and test eXo Chat add-on in an easy way.
We provide a fully functional Docker Compose file to start eXo Trial with a persistent volume (to keep data across test sessions).

Create a Docker volume
-------------------
This step is only needed once.

    docker volume create --name plf_chat_test

Build the project
-------------------
From the home of the source project :

    # To build everything
    mvn clean package

    # To only build the add-on package and his required modules
    mvn -am -pl :embedded-packaging clean package

(!) you need to execute this step each time you changed the add-on source code and want to redeploy it.

Start eXo Platform
-------------------

Simply use docker-compose to start eXo Trial and the chat add-on

    docker-compose up -d

(!) you need to wait eXo Platform to be started and if you want to tail the eXo Platform logs just do in another terminal :

    docker exec exo_chat tail -f /var/log/exo/platform.log

When you see the `Server startup in xxxxxxx ms` in your logs, it mean that you can use your eXo Platform :

* Linux host : http://localhost/
* from a Windows or OS X host : http://DOCKER_MACHINE_IP/

Code, test & debug the add-on
-------------------

### rebuild and reload your add-on

You have made some changes on the add-on code and you want to test it without restarting eXo Platform ? quite simple ...

What you have to do is :

    # rebuild your add-on package
    mvn -am -pl :embedded-packaging clean package

    # use the add-on manager to redeploy the newly build version
    docker exec exo_chat /opt/exo/current/addon install --catalog=file:///etc/exo/catalog.json  exo-chat:1.3.x-SNAPSHOT --force --batch-mode

WARNING: this will produce a `java.lang.IllegalStateException: No pre init tasks can be added to the portal container ...` exception because eXo doesn't support the full hot reloading of an extension but the 3 wars of the add-ons will still be reloaded.
If you dislike this, you can simply restart the container with `docker-compose restart exo` and wait eXo Platform start ... ;-)

### inspect mongodb data

If you need to connect to the mongodb backend you just need to use the Mongo Shell from the container :

    docker exec -ti exo_chat mongo chat

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

