package com.substring.chat.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Message {
	
	 @Id  // Add this annotation
	 private String id;  // MongoDB will auto-generate this

    private String sender;
    private String content;
    private LocalDateTime timeStamp;
    private boolean deleted;

    public Message(String sender, String content) {
    	this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.content = content;
        this.timeStamp = LocalDateTime.now();
        
    }
    
    
}
