package com.arpit.oneToOneChat.internalChat;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * @author Arpit Kumar
 */
@Log4j2
@Component
public class MessageQueue {
    private Queue<Message> unsentMessages = new LinkedList<>();

    public void enqueue(Message message) {
        unsentMessages.add(message);
    }

    public List<Message> dequeueAll(int recipientId) {
        List<Message> unSendMessageList = unsentMessages.stream()
                .filter(message -> message.getRecipientId().equals(recipientId))
                .collect(Collectors.toList());

        // Remove the dequeued messages from the original queue
        unsentMessages.removeAll(unSendMessageList);

        return unSendMessageList;
    }
}
