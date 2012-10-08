package org.benjp.chat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class ChatServlet extends HttpServlet
{

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String[] params = request.getRequestURI().split("/");
    try
    {
      if (params.length==4)
      {
        String room = params[3];
        System.out.println("CHAT SERVLET::"+System.currentTimeMillis());

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        //response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "no-cache");
        InputStream in = getStream(room);
        OutputStream out = response.getOutputStream();

        out.write(new String("id: "+System.currentTimeMillis()+"\n").getBytes());
        out.write(new String("data: ").getBytes());

        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
          out.write(buffer, 0, len);
          len = in.read(buffer);
        }
        out.write(new String("\n\n").getBytes());
        response.flushBuffer();

      }
    }
    catch (Exception e)
    {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return;
  }


  private InputStream getStream(String room) throws IOException
  {
    String file = "/Users/benjamin/Desktop/log-"+room+".txt";

    try
    {
      //Construct the BufferedInputStream object
      return new FileInputStream(file);
    }
    catch (FileNotFoundException fnfe)
    {

    }
    return null;
  }


}