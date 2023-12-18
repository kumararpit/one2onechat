package com.arpit.oneToOneChat;

import com.arpit.oneToOneChat.internalChat.ChatRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.arpit.oneToOneChat.internalChat.ChatWebSocketHandler.CHATIDSLIST;

@SpringBootApplication
public class OneToOneChatApplication {

    @Autowired
    private ChatRepository chatRepository;

    public static void main(String[] args) {

        SpringApplication.run(OneToOneChatApplication.class, args);
    }

    @PostConstruct
    public void LoadChatIds() {
        CHATIDSLIST.addAll(chatRepository.getChatIds());
        System.out.println("CHATIDSLIST = " + CHATIDSLIST);
    }


}
