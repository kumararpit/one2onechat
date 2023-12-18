package com.arpit.oneToOneChat.internalChat;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * @author Arpit Kumar
 */
@Component
public class MessageDBQueue {
    private Queue<Message> messages = new LinkedList<>();

    public void enqueue(Message message) {
        messages.add(message);
    }

    public List<Message> getNotInsertedMsg(int recipientId) {
        return messages.stream()
                .filter(message -> message.getRecipientId().equals(recipientId) || message.getSenderId().equals(recipientId))
                .collect(Collectors.toList());
    }

    public void enqueue(List<Message> message) {
        messages.addAll(message);
    }

    public List<Message> dequeueAll(int senderId) {
        List<Message> messageList = messages.stream()
                .filter(message -> message.getSenderId().equals(senderId))
                .collect(Collectors.toList());

        // Remove the dequeued messages from the original queue
        messages.removeAll(messageList);

        return messageList;
    }
}
