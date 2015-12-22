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

