package com.arpit.oneToOneChat.internalChat;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Arpit Kumar
 */
public class ChatWebSocketHandler implements WebSocketHandler {
    private final Map<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Autowired
    private MessageQueue messageQueue;

    @Autowired
    private MessageDBQueue dbQueue;

    @Autowired
    private ChatRepository chatRepository;
    public static List<String> CHATIDSLIST = new ArrayList<>();
    private String CHAT_ID_DELIMITER = "-";
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @Override
    public Mono<Void> handle(WebSocketSession session) {
        int userId = Integer.parseInt(session.getHandshakeInfo().getUri().getQuery().split("=")[1]); // Extract username from the query parameter
        sessions.put(userId, session);

        this.publishUnsendMessages(userId, session);
        Flux<String> output = session.receive()
                .flatMap(payLoad -> handleIncomingMessage(userId, payLoad.getPayloadAsText()))
                .doOnTerminate(() -> handleTermination(userId, session));

        return session.send(output.map(session::textMessage));
    }

    private void handleTermination(int username, WebSocketSession session) {
        sessions.remove(username, session);
        insertToDBMessages(username);
    }

    private void insertToDBMessages(int senderId) {
        chatRepository.insertOrUpdateMessages(dbQueue.dequeueAll(senderId));
    }

    private void publishUnsendMessages(int userId, WebSocketSession session) {
        List<Message> messagesInQueue = messageQueue.dequeueAll(userId);
        dbQueue.enqueue(messagesInQueue);
        /**
         * get all messages which are not inserted in database yet in which user is sender or recipient
         */
        List<Message> messages = dbQueue.getNotInsertedMsg(userId);

        /**
         * get all chatId of user
         */
        List<String> chatIds = this.getChatIdsForUser(String.valueOf(userId));
        /**
         * get chat History of user with all recipients
         */
        List<Message> chatHistory = chatRepository.getMessages(chatIds, String.valueOf(userId));
        messages.addAll(chatHistory);

        session.send(Mono.just(session.textMessage(new JSONObject(messages.stream().collect(Collectors.groupingBy(m -> extractRecipentIdFromChatId(m.getChatId(), String.valueOf(userId))))).toString()))).subscribe();
    }

    private Mono<String> handleIncomingMessage(int senderId, String payload) {
        /**
         * Extract the recipient and message content from the incoming message
         */
        String[] parts = payload.split(":", 2);
        int recipientId = Integer.parseInt(parts[0]);
        String content = parts[1];
        Message message = new Message();
        /**
         * check for chatId
         */
        this.getChatId(message, senderId, recipientId);

        /**
         * create message payload
         */
        this.createMessagePayload(message, senderId, recipientId, content);
        /**
         *  Find the WebSocket session of the recipient
         */
        WebSocketSession recipientSession = sessions.get(recipientId);

        /**
         * If the recipient is online, send the message; otherwise, handle offline messaging
         */
        if (recipientSession != null && recipientSession.isOpen()) {
            message.setDelivered(true);
            recipientSession.send(Mono.just(recipientSession.textMessage(message.toString())))
                    .doOnError(error -> handleOfflineMessaging(message))
                    .doOnNext(result -> dbQueue.enqueue(message))
                    .subscribe();

        } else {
            /**
             * Handle offline messaging, store messages in queue
             */
            handleOfflineMessaging(message);
        }
        return Mono.empty(); // Empty response as acknowledgment
    }

    private void handleOfflineMessaging(Message message) {
        message.setDelivered(false);
        messageQueue.enqueue(message);
    }

    private void getChatId(Message message, int senderId, int recipientId) {
        if (CHATIDSLIST.contains(senderId + CHAT_ID_DELIMITER + recipientId)) {
            message.setChatId(senderId + CHAT_ID_DELIMITER + recipientId);
        } else if (CHATIDSLIST.contains(recipientId + CHAT_ID_DELIMITER + senderId)) {
            message.setChatId(recipientId + CHAT_ID_DELIMITER + senderId);
        } else {
            message.setChatId(senderId + CHAT_ID_DELIMITER + recipientId);
            chatRepository.insertChatId(message.getChatId(), senderId, recipientId);
            CHATIDSLIST.add(message.getChatId());
        }
    }


    private void createMessagePayload(Message message, int senderId, int recipientId, String content) {
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setTime(dateFormat.format(new Date(new Timestamp(System.currentTimeMillis()).getTime())));
        message.setContent(content);
    }

    private List<String> getChatIdsForUser(String userId) {
        return CHATIDSLIST.stream().filter(chatId -> {
            for (String id : chatId.split("-")) {
                if (id.equals(userId))
                    return true;
            }
            return false;
        }).toList();
    }


    private String extractRecipentIdFromChatId(String fullChatId, String userId) {
        String[] parts = fullChatId.split("-");
        if (parts[0].equals(userId))
            return parts[1];
        else
            return parts[0];
    }
}
