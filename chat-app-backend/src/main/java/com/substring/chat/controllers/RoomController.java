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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin
public class RoomController {
	
	private final SimpMessagingTemplate simpMessagingTemplate;

    private RoomRepository roomRepository;


    public RoomController(RoomRepository roomRepository,SimpMessagingTemplate simpMessagingTemplate) {
    	this.simpMessagingTemplate = simpMessagingTemplate;
        this.roomRepository = roomRepository;
    }

    /// Create Room (Now with Topic + Admin)
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> request) {
        String roomId = request.get("roomId");
        String adminUser = request.get("adminUser").replaceAll("^\"|\"$",""); // Clean admin name
        
        if (roomRepository.findByRoomId(roomId) != null) {
            return ResponseEntity.badRequest().body("Room exists!");
        }

        Room room = new Room();
        room.setRoomId(roomId);
        room.setRoomTopic(request.get("roomTopic"));
        room.setAdminUser(adminUser);
        room.setConnectedUsers(new ArrayList<>(List.of(adminUser))); // Initialize with just admin
        return ResponseEntity.ok(roomRepository.save(room));
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

	//Delete Message(deletes from database)
	@DeleteMapping("/{roomId}/messages/{messageId}")
	public ResponseEntity<?> deleteMessage(@PathVariable String roomId, @PathVariable String messageId,
			@RequestParam String requestedBy) {
		System.out.println("called");
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
		        Message msg = messageToDelete.get();
		        msg.setDeleted(true);
		        //msg.setContent("This message was deleted by an admin.");
		        roomRepository.save(room);

		        // Optional: Broadcast deleted message
		        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, msg);

		        return ResponseEntity.ok().build();
		    }

		return ResponseEntity.notFound().build();
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
