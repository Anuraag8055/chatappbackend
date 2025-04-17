package com.substring.chat.config;

import com.substring.chat.entities.Room;
import com.substring.chat.repositories.RoomRepository;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class PresenceChannelInterceptor implements ChannelInterceptor {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public PresenceChannelInterceptor(RoomRepository roomRepository, 
                                    @Lazy SimpMessagingTemplate simpMessagingTemplate) {
        this.roomRepository = roomRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleUserConnect(accessor);
        } 
        else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            handleUserDisconnect(accessor);
        }
        
        return message;
    }

//    private void handleUserConnect(StompHeaderAccessor accessor) {
//        String username = accessor.getFirstNativeHeader("username");
//        String roomId = accessor.getFirstNativeHeader("roomId");
//        Room room = roomRepository.findByRoomId(roomId);
//        if (room != null && !room.getConnectedUsers().contains(username)) {
//            room.getConnectedUsers().add(username);
//            roomRepository.save(room);
//            broadcastUserList(roomId, room.getConnectedUsers());
//        }
//    }
    private void handleUserConnect(StompHeaderAccessor accessor) {
        String username = accessor.getFirstNativeHeader("username");
        String roomId = accessor.getFirstNativeHeader("roomId");

        if (username != null) {
            username = username.replaceAll("^\"|\"$", ""); // Remove leading/trailing quotes
        }

        Room room = roomRepository.findByRoomId(roomId);
        if (room != null) {
            if (!username.equals(room.getAdminUser()) && !room.getConnectedUsers().contains(username)) {
                room.getConnectedUsers().add(username);
                roomRepository.save(room);
            }

            broadcastUserList(roomId, room.getConnectedUsers());
        }
    }

    private void handleUserDisconnect(StompHeaderAccessor accessor) {
        String username = accessor.getFirstNativeHeader("username");
        String roomId = accessor.getFirstNativeHeader("roomId");
        Room room = roomRepository.findByRoomId(roomId);
        if (room != null) {
            room.getConnectedUsers().remove(username);
            roomRepository.save(room);
            broadcastUserList(roomId, room.getConnectedUsers());
        }
    }

    private void broadcastUserList(String roomId, List<String> users) {
        simpMessagingTemplate.convertAndSend("/topic/onlineUsers/" + roomId, users);
    }
}