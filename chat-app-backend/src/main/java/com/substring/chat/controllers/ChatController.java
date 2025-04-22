package com.substring.chat.controllers;

import com.substring.chat.entities.Message;
import com.substring.chat.entities.Room;
import com.substring.chat.playload.MessageRequest;
import com.substring.chat.repositories.RoomRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@CrossOrigin("http://localhost:5173")
public class ChatController {


    private RoomRepository roomRepository;

    public ChatController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }


    //for sending and receiving messages
    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message sendMessage(
            @DestinationVariable String roomId,
            @RequestBody MessageRequest request
    ) {
        // Retrieve the room from the repository
        Room room = roomRepository.findByRoomId(request.getRoomId());
        
        // Check if room exists
        if (room == null) {
            throw new RuntimeException("Room not found!");
        }
        
        // Create a new message
        Message message = new Message(request.getSender(), request.getContent());
//        message.setId(UUID.randomUUID().toString());
//        message.setContent(request.getContent());
//        message.setSender(request.getSender());
//        message.setTimeStamp(LocalDateTime.now());
//        
        // Add the message to the room's message list
        room.getMessages().add(message);
        
        // Save the room (this will also persist the message)
        roomRepository.save(room);
        
        // Log to check if the ID is generated
        System.out.println("Message ID after save: " + message.getId());
        
        // Return the message with the generated ID
        return message;
    }
    
	@MessageMapping("/userJoin/{roomId}")
	@SendTo("/topic/onlineUsers/{roomId}")
	public List<String> handleUserJoin(@DestinationVariable String roomId, @Payload String username) {
		username = username.replaceAll("^\"|\"$", "");
		Room room = roomRepository.findByRoomId(roomId);
		if (room != null) {
			// Remove if exists to avoid duplicates
			room.getConnectedUsers().remove(username);
			room.getConnectedUsers().add(username);
			roomRepository.save(room);
		}
		return room != null ? room.getConnectedUsers() : new ArrayList<>();
	}
    
}
