# Chat - POC with Vue.js

## Build

Build the module with 
```
mvn clean install
```

## Deployment in eXo Platform

In order to deploy the Chat application in eXo Platform, copy the built war file to eXo webapps and start eXo.
The new portlet should be available and can be added in any page of the portal.

## Development mode

In development mode, the application can be executed in standalone mode (no portal).
The web server Express is used to serve resources and simulate server calls.

### Install Node

### Build

At the root of the application, run
```
npm install
npm run build
```

### Run

At the root of the application, run
```
npm start
```
This command starts the Node Express server.
The application is now available at http://localhost:3000

The application served by Express is located in the target folder (in order to be compliant with Maven).
Therefore, if you change a file in the folder src, the change will not be considered.
Use the following command to see all you changes immediately:
```
npm run watch
```
As soon as a file is changed, it will be copied to target and be available.


### Running Tests

// TODO
