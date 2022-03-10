package org.exoplatform.addons.chat.model;

import java.util.List;

public class MessageReceivedModel {
	
    private String roomId;
    private String roomName;
    private String message;
    private String sender;
    private String senderFullName;
    private List<String> receivers; 
	
    public MessageReceivedModel(String roomId, String roomName, String message, String sender, String senderFullName, List<String> receivers) {
		this.roomId = roomId;
		this.roomName = roomName;
		this.message = message;
		this.sender = sender;
		this.senderFullName = senderFullName;
		this.receivers = receivers;
	}

	public MessageReceivedModel() {
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getSenderFullName() {
		return senderFullName;
	}

	public void setSenderFullName(String senderFullName) {
		this.senderFullName = senderFullName;
	}

	public List<String> getReceivers() {
		return receivers;
	}

	public void setReceivers(List<String> receivers) {
		this.receivers = receivers;
	}

	@Override
	public String toString() {
		return "MessageReceivedModel [roomId=" + roomId + ", roomName=" + roomName + ", message=" + message
				+ ", sender=" + sender + ", senderFullName=" + senderFullName + ", receivers=" + receivers + "]";
	}
}
