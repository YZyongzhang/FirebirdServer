package org.yongzhang.firebird.Data;

public class Message {
    private String id;
    private Long fromUserId;
    private String fromUsername;
    private Long toUserId;
    private String content;
    private String date;

    public Message() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}

