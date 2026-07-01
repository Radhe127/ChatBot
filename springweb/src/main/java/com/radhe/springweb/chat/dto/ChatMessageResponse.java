package com.radhe.springweb.chat.dto;

import com.radhe.springweb.chat.ChatMessageEntity;
import com.radhe.springweb.chat.ChatSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ChatMessageResponse {
    private UUID id;
    private String role;
    private String content;
    private Instant createdAt;

    public ChatMessageResponse(ChatMessageEntity e) {
        this.id = e.getId();
        this.role = e.getRole();
        this.content = e.getContent();
        this.createdAt = e.getCreatedAt();
    }

    public UUID getId() { return id; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
