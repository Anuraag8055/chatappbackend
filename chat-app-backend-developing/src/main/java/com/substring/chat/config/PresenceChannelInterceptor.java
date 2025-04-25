package com.substring.chat.config;

import com.substring.chat.entities.Room;
import com.substring.chat.repositories.RoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public class StringUtils {
        public static String cleanString(String input) {
            return input != null ? 
                input.replaceAll("^\"|\"$", "").trim().toLowerCase() : 
                null;
        }
    }
    private void handleUserConnect(StompHeaderAccessor accessor) {
        String rawUsername = accessor.getFirstNativeHeader("username");
        String roomId = accessor.getFirstNativeHeader("roomId");
        
        // Normalize username (remove all quotes and trim)
        String username = rawUsername != null ? 
            rawUsername.replaceAll("[\"]", "").trim() : 
            null;

        if (username == null || roomId == null) return;

        Room room = roomRepository.findByRoomId(roomId);
        if (room != null) {
            // Normalize existing users for comparison
            List<String> normalizedConnectedUsers = room.getConnectedUsers().stream()
                .map(u -> u.replaceAll("[\"]", "").trim())
                .collect(Collectors.toList());

            // Check if user already exists (comparing normalized names)
            if (!normalizedConnectedUsers.contains(username) && 
                !username.equals(room.getAdminUser().replaceAll("[\"]", "").trim())) {
                
                room.getConnectedUsers().add(username); // Store clean version
                roomRepository.save(room);
            }

            // Prepare clean list for broadcasting
            List<String> usersToBroadcast = new ArrayList<>();
            usersToBroadcast.add(room.getAdminUser());
            usersToBroadcast.addAll(
                room.getConnectedUsers().stream()
                    .filter(u -> !u.equals(room.getAdminUser()))
                    .collect(Collectors.toList())
            );
            
            broadcastUserList(roomId, usersToBroadcast);
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