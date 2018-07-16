# Chat Front-end application with Vue.js

## Build

Build the module with 
```
mvn clean install
```

## Deployment in eXo Platform

In order to deploy the Chat application in eXo Platform, copy the built war file to eXo webapps and start eXo.
The new portlet should be available and can be added in any page of the portal.

## Development mode

In development mode, the application can be executed in eXo server.
Chat application should be installed on eXo server.
Edit the webpack.dev.js file with the path location of eXo server.

### Install Node

### Install modules

At the root of the application, run
```
npm install
```

### Run development mode

At the root of the application, run
```
npm start
```
This command will build and put output files on the eXo server.

Therefore, if you change a file in the folder src, the change will not be considered.
Use the following command to see all you changes immediately:
```
npm run watch
```
As soon as a file is changed, chages will be available immediately.


### Running Tests

At the root of the application, run
```
npm run test
```

To see the coverage of tests, run
```
npm run test-coverage
```