package com.substring.chat.controllers;

import com.substring.chat.entities.Message;
import com.substring.chat.entities.Room;
import com.substring.chat.repositories.RoomRepository;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin("http://localhost:5173")
public class RoomController {
	
	private final SimpMessagingTemplate simpMessagingTemplate;

    private RoomRepository roomRepository;


    public RoomController(RoomRepository roomRepository,SimpMessagingTemplate simpMessagingTemplate) {
    	this.simpMessagingTemplate = simpMessagingTemplate;
        this.roomRepository = roomRepository;
    }

    /// Create Room (Now with Topic + Admin)
    @PostMapping
    public ResponseEntity<?> createRoom(
        @RequestBody Map<String, String> request // Accept JSON { "roomId": "123", "roomTopic": "General", "adminUser": "Anuraag" }
    ) {
        String roomId = request.get("roomId");
        if (roomRepository.findByRoomId(roomId) != null) {
            return ResponseEntity.badRequest().body("Room already exists!");
        }

        Room room = new Room();
        room.setRoomId(roomId);
        room.setRoomTopic(request.get("roomTopic")); // Set topic
        room.setAdminUser(request.get("adminUser")); // Set admin
        room.getConnectedUsers().add(request.get("adminUser")); // Admin joins automatically
        return ResponseEntity.status(HttpStatus.CREATED).body(roomRepository.save(room));
    }

    // Delete Room (Admin Only)
	@DeleteMapping("/{roomId}")
	public ResponseEntity<?> deleteRoom(@PathVariable String roomId, @RequestParam String requestedBy) {
		Room room = roomRepository.findByRoomId(roomId);
		if (room == null)
			return ResponseEntity.notFound().build();

		if (!room.getAdminUser().equals(requestedBy)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admin can delete this room!");
		}

		roomRepository.delete(room);

		// Broadcast room deletion
		simpMessagingTemplate.convertAndSend("/topic/roomDeleted/" + roomId, "Room has been deleted by admin");

		return ResponseEntity.ok("Room deleted!");
	}

	//Delete Message
	@DeleteMapping("/{roomId}/messages/{messageId}")
	public ResponseEntity<?> deleteMessage(@PathVariable String roomId, @PathVariable String messageId,
			@RequestParam String requestedBy) {
		Room room = roomRepository.findByRoomId(roomId);
		if (room == null)
			return ResponseEntity.notFound().build();

		// Verify requester is admin
		if (!room.getAdminUser().equals(requestedBy)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// Find and remove the message
		Optional<Message> messageToDelete = room.getMessages().stream().filter(m -> m.getId().equals(messageId))
				.findFirst();

		if (messageToDelete.isPresent()) {
			room.getMessages().remove(messageToDelete.get());
			roomRepository.save(room);
			return ResponseEntity.ok().build();
		}

		return ResponseEntity.notFound().build();
	}
	
	//User Removal 
	@DeleteMapping("/{roomId}/users/{username}")
	public ResponseEntity<?> removeUser(@PathVariable String roomId, @PathVariable String username,
			@RequestParam String requestedBy) {
		Room room = roomRepository.findByRoomId(roomId);
		if (room == null)
			return ResponseEntity.notFound().build();

		// Only admin can remove users
		if (!room.getAdminUser().equals(requestedBy)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// Cannot remove admin
		if (room.getAdminUser().equals(username)) {
			return ResponseEntity.badRequest().body("Cannot remove admin user");
		}

		room.getConnectedUsers().remove(username);
		roomRepository.save(room);

		// Broadcast updated user list
		simpMessagingTemplate.convertAndSend("/topic/onlineUsers/" + roomId, room.getConnectedUsers());

		// Notify the removed user
		simpMessagingTemplate.convertAndSend("/topic/userRemoved/" + roomId + "/" + username,
				"You have been removed by admin");

		return ResponseEntity.ok().build();
	}

    //get room: join
    @GetMapping("/{roomId}")
    public ResponseEntity<?> joinRoom(
            @PathVariable String roomId
    ) {

        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            return ResponseEntity.badRequest()
                    .body("Room not found!!");
        }
        return ResponseEntity.ok(room);
    }


    //get messages of room

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable String roomId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size
    ) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            return ResponseEntity.badRequest().build()
                    ;
        }
        //get messages :
        //pagination
        List<Message> messages = room.getMessages();
        int start = Math.max(0, messages.size() - (page + 1) * size);
        int end = Math.min(messages.size(), start + size);
        List<Message> paginatedMessages = messages.subList(start, end);
        return ResponseEntity.ok(paginatedMessages);

    }


}
