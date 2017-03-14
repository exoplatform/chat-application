package org.exoplatform.chat.services;

public class ChatException extends RuntimeException {
  private int status;

  public ChatException() {
    super();
  }

  public ChatException(int status, String message) {
    super(message);
    
    this.status = status;
  }

  public int getStatus() {
    return this.status;
  }
}
