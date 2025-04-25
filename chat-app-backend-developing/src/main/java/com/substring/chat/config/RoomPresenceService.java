package com.substring.chat.config;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.substring.chat.entities.Room;
import com.substring.chat.repositories.RoomRepository;

@Service
public class RoomPresenceService {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomPresenceService(RoomRepository roomRepository, 
                               SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void userJoined(String roomId, String username) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room != null && !room.getConnectedUsers().contains(username)) {
            room.getConnectedUsers().add(username);
            roomRepository.save(room);
            messagingTemplate.convertAndSend("/topic/onlineUsers/" + roomId, room.getConnectedUsers());
        }
    }

    public void userLeft(String roomId, String username) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room != null) {
            room.getConnectedUsers().remove(username);
            roomRepository.save(room);
            messagingTemplate.convertAndSend("/topic/onlineUsers/" + roomId, room.getConnectedUsers());
        }
    }
}
