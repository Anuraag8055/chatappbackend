package com.substring.chat.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    private String id;//Mongo db : unique identifier
    private String roomId;
    private String roomTopic; // New field
    private String adminUser; // Creator = Admin
    private List<String> connectedUsers = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();

    public void addUser(String username) {
        if (!connectedUsers.contains(username) ) {
            connectedUsers.add(username);
        }
    }

    public void removeUser(String username) {
        connectedUsers.remove(username);
    }
}
