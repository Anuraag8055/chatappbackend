package com.substring.chat.controllers;

import com.substring.chat.entities.Message;
import com.substring.chat.entities.Room;
import com.substring.chat.playload.MessageRequest;
import com.substring.chat.repositories.RoomRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@CrossOrigin
public class ChatController {


    private RoomRepository roomRepository;
	private final SimpMessagingTemplate messagingTemplate;

    public ChatController(RoomRepository roomRepository, SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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

        // Add the message to the room's message list
        room.getMessages().add(message);
        
        // Save the room (this will also persist the message)
        roomRepository.save(room);
        
        
        // Return the message with the generated ID
        return message;
    }
    
    @MessageMapping("/join/{roomId}")
   // @SendTo("/topic/room/{roomId}")
    public Room joinRoom(
            @DestinationVariable String roomId,
            @Payload String username
    ) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            throw new RuntimeException("Room not found!");
        }

        // Add user to the connected users list
        	room.addUser(username);
        	
        // Save updated room state
        roomRepository.save(room);
        
         messagingTemplate.convertAndSend("/topic/roomUsers/" + roomId, room.getConnectedUsers());
        // Return updated room state with new user list
        return room;
    }
    
    @MessageMapping("/leave/{roomId}")
   // @SendTo("/topic/room/{roomId}")
    public Room leaveRoom(
            @DestinationVariable String roomId,
            @Payload String username
    ) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            throw new RuntimeException("Room not found!");
        }

        // Remove user from the connected users list
        room.removeUser(username);

        // Save updated room state
        roomRepository.save(room);
        
        messagingTemplate.convertAndSend("/topic/roomUsers/" + roomId, room.getConnectedUsers());

        // Return updated room state with user list after removal
        return room;
    }
    

    
}
