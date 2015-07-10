package org.exoplatform.chat.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.chat.utils.PropertyManager;

public class MigrateService {

  private static final Logger LOG = LoggerFactory.getLogger(MigrateService.class);

  public MigrateService() {}

  public void migrate() {
    // Collect database info
    String hostname = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_HOST);
    String port = PropertyManager.getProperty(PropertyManager.PROPERTY_SERVER_PORT);
    String dbName = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_NAME);
    String isAuth = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_AUTHENTICATION);
    String username = "", password = "";
    if (Boolean.parseBoolean(isAuth)) {
      username = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_USER);
      password = PropertyManager.getProperty(PropertyManager.PROPERTY_DB_PASSWORD);
    }

    if (StringUtils.isEmpty(dbName)) {
      LOG.error("Database name is required. Set it in the variable 'dbName' in chat.properties");
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("mongo --quiet ");
    if (!StringUtils.isEmpty(hostname)) {
      sb.append(hostname);
      if (!StringUtils.isEmpty(port)) {
        sb.append(":")
          .append(port);
      }
      sb.append("/");
    }

    sb.append(dbName)
      .append(" ");

    if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
      sb.append("-u ")
        .append(username)
        .append(" -p ")
        .append(password)
        .append(" ");
    }

    // Copy migration script to /temp folder to perform migrate process via mongo command
    InputStream fileIn = this.getClass().getClassLoader().getResourceAsStream("migration-chat-addon.js");
    OutputStream fileOut = null;
    File migrationScriptfile = null;
    try {
      migrationScriptfile = File.createTempFile("migration-chat-addon", ".js");
      migrationScriptfile.deleteOnExit();
      fileOut = new FileOutputStream(migrationScriptfile);
      byte[] buf = new byte[1024];
      int bytesRead;
      while ((bytesRead = fileIn.read(buf)) > 0) {
        fileOut.write(buf, 0, bytesRead);
      }
    } catch(IOException e){
      LOG.error("Failed to copy migration script : "+e.getMessage(), e);
      return;
    } finally {
      try {
        if (fileIn != null) {
          fileIn.close();
        }
        if (fileOut != null) {
          fileOut.close();
        }
      } catch (IOException e){
        LOG.error("Failed to close files : "+e.getMessage(), e);
      }
    }

    // Execute mongo command
    String command = sb.append(migrationScriptfile.getAbsolutePath()).toString();
    try {
      Process p = Runtime.getRuntime().exec(command);
      StringBuffer output = new StringBuffer();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = "";
      while ((line = reader.readLine())!= null) {
        output.append(line + "\n");
      }
      p.waitFor();
      LOG.info(output.toString());
    } catch (Exception e) {
      LOG.error("Error while migrating chat data : " + e.getMessage(), e);
    }
  }
}