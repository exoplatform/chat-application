package org.exoplatform.addons.chat.model;

import java.util.List;

public class MentionModel {

    private String roomId;
    private String roomName;
    private List<String> mentionedUsers;
    private String sender;
    private String senderFullName;

    public MentionModel(String roomId, List<String> mentionedUsers) {
        this.roomId = roomId;
        this.mentionedUsers = mentionedUsers;
    }

    public MentionModel() {
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<String> getMentionedUsers() {
        return mentionedUsers;
    }

    public void setMentionedUsers(List<String> mentionedUsers) {
        this.mentionedUsers = mentionedUsers;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public void setSenderFullName(String senderFullName) {
        this.senderFullName = senderFullName;
    }
}
