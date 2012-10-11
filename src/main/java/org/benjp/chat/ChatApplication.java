package org.benjp.chat;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.View;
import juzu.portlet.JuzuPortlet;
import juzu.template.Template;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Scope;
import java.io.*;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ChatApplication extends juzu.Controller
{

  @Inject
  @Path("index.gtmpl")
  Template index;

  String roomdata;

  @View
  public void index() throws IOException
  {
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();
    index.with().set("user", remoteUser).set("room", "roomname").render();
  }

  @Resource
  public Response.Content send(String user, String message, String room) throws IOException
  {
    try
    {
      //System.out.println(user + "::" + message + "::" + room);
      if (message!=null && user!=null)
      {
        write(message, user, room);
      }

    }
    catch (Exception e)
    {
      return Response.notFound("Problem on Chat server. Please, try later").withMimeType("text/event-stream");
    }
    String data = "id: "+System.currentTimeMillis()+"\n";
    data += "data: "+read(room) +"\n\n";


    return Response.ok(data).withMimeType("text/event-stream").withHeader("Cache-Control", "no-cache");
  }

  private void write(String message, String user, String room) throws IOException
  {
    String file = "/Users/benjamin/Desktop/log-"+room+".txt";
    // Create file
    FileWriter fstream = new FileWriter(file, true);
    BufferedWriter out = new BufferedWriter(fstream);

    out.write("<div class='msgln'><b>"+user+"</b>: "+message+"<br></div>");

    //Close the output stream
    out.close();

  }

  private String read(String room) throws IOException
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
