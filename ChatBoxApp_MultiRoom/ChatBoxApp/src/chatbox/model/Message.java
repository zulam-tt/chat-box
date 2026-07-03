package chatbox.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum Type { TEXT, JOIN, LEAVE, SYSTEM, CREATE_ROOM, SWITCH_ROOM, ROOM_LIST, DELETE_ROOM, ADMIN_INFO }

    private String  sender;
    private String  content;
    private String  timestamp;
    private Type    type;
    private boolean encrypted;
    private String  room;

    public Message(String sender, String content, Type type) {
        this.sender    = sender;
        this.content   = content;
        this.type      = type;
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.encrypted = false;
        this.room      = "chung";
    }

    public Message(String sender, String content, Type type, String room) {
        this(sender, content, type);
        this.room = room;
    }

    public String  getSender()    { return sender; }
    public String  getContent()   { return content; }
    public String  getTimestamp() { return timestamp; }
    public Type    getType()      { return type; }
    public boolean isEncrypted()  { return encrypted; }
    public String  getRoom()      { return room != null ? room : "chung"; }

    public void setContent(String c)    { this.content   = c; }
    public void setEncrypted(boolean e) { this.encrypted  = e; }
    public void setRoom(String r)       { this.room       = r; }

    @Override
    public String toString() {
        return "[" + timestamp + "][" + room + "] " + sender + ": " + content;
    }
}
