package com.arpit.oneToOneChat.internalChat;

import lombok.Data;
import org.json.JSONObject;

/**
 * @author Arpit Kumar
 */
@Data
public class Message {
    private String chatId;
    private Integer senderId;
    private Integer recipientId;
    private String content;
    private String time;
    private boolean isDelivered;

    @Override
    public String toString() {
        return new JSONObject(this).toString();
    }
}
