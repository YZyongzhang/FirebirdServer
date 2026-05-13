package org.yongzhang.firebird.Data;

import java.util.List;

public class MessagesResponse {
    private List<Message> messages;

    public MessagesResponse() {}
    public MessagesResponse(List<Message> messages) { this.messages = messages; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}

