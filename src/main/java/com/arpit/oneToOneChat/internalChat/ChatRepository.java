package com.arpit.oneToOneChat.internalChat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Arpit Kumar
 */
@Repository
public class ChatRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;


    public void insertOrUpdateMessages(List<Message> messages) {

        try {
            // Group messages by ChatID
            Map<String, List<Message>> messagesByChatId = messages.stream()
                    .collect(Collectors.groupingBy(Message::getChatId));
            String query = "UPDATE CT_MEASSAGES SET Messages = CT_MEASSAGES.Messages || :messages ";

            for (Map.Entry<String, List<Message>> entry : messagesByChatId.entrySet()) {
                Map<String, Object> parameters = getParameters(entry);
                namedParameterJdbcTemplate.update(query, parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Map<String, Object> getParameters(Map.Entry<String, List<Message>> entry) throws SQLException {
        String chatId = entry.getKey();
        List<Message> chatMessages = entry.getValue();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("chatId", chatId);
        PGobject jsonbObject = new PGobject();
        jsonbObject.setType("jsonb");
        jsonbObject.setValue(chatMessages.toString());
        parameters.put("messages", jsonbObject);
        return parameters;
    }

    public List<String> getChatIds() {
        try {
            return jdbcTemplate.queryForList("SELECT ChatID FROM CT_MEASSAGES", String.class);
        } catch (Exception ex) {
            System.out.println("ex.getMessage() = " + ex.getMessage());
        }
        return new ArrayList<>();
    }


    public List<Message> getMessages(List<String> chatIds, String userId) {
        if (chatIds.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "SELECT Messages FROM CT_MEASSAGES WHERE chatId IN(:chatIds)"; // Replace with your actual table name
        List<PGobject> pgObjects = namedParameterJdbcTemplate.queryForList(sql, new MapSqlParameterSource().addValue("chatIds", chatIds), PGobject.class);

        // Convert PGobjects to List<Message>
        return pgObjects.stream()
                .map(PGobject::getValue)
                .map(this::jsonToMessageList)
                .flatMap(List::stream)
                .toList();
    }

    private List<Message> jsonToMessageList(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<Message>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void insertChatId(String chatId, int senderId, int recipientId) {
        jdbcTemplate.update("INSERT INTO CT_MEASSAGES (CHATID,USER1,USER2) VALUES (?,?,?)", chatId, senderId, recipientId);
    }

}
