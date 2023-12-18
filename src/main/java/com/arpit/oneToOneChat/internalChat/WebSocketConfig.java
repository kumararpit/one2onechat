package com.arpit.oneToOneChat.internalChat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Arpit Kumar
 */
@Configuration
public class WebSocketConfig {
    @Bean
    public ChatWebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler();
    }

    @Bean
    public HandlerMapping internalChatMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/internalchatsocket", chatWebSocketHandler());
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(1);
        return mapping;
    }
}
