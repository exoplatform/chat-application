package org.benjp.services;

import juzu.SessionScoped;

import javax.inject.Named;
import java.io.*;

@Named("chatService")
public class ChatService {


  public static void write(String message, String user, String room) throws IOException
  {
    String file = "/Users/benjamin/Desktop/log-"+room+".txt";
    // Create file
    FileWriter fstream = new FileWriter(file, true);
    BufferedWriter out = new BufferedWriter(fstream);

    out.write("<div class='msgln'><b>"+user+"</b>: "+message+"<br></div>");

    //Close the output stream
    out.close();

  }

  public static String read(String room) throws IOException
  {
    String file = "/Users/benjamin/Desktop/log-"+room+".txt";

    StringBuffer sb = new StringBuffer();

    try
    {
      byte[] buffer = new byte[1024];
      //Construct the BufferedInputStream object
      //FileReader fstream = new FileReader(file);
      //BufferedInputStream bufferedInput = new BufferedInputStream(fstream);
      FileInputStream bufferedInput = new FileInputStream(file);

      int bytesRead = 0;

      //Keep reading from the file while there is any content
      //when the end of the stream has been reached, -1 is returned
      while ((bytesRead = bufferedInput.read(buffer)) != -1) {

        //Process the chunk of bytes read
        //in this case we just construct a String and print it out
        String chunk = new String(buffer, 0, bytesRead);
        sb.append(chunk);

      }
    }
    catch (FileNotFoundException fnfe)
    {

    }
    //System.out.println("READ***"+sb.toString());
    return sb.toString();
  }



}
