package com.radhe.springweb.chat.dto;

import com.radhe.springweb.chat.ChatSession;

import java.time.Instant;
import java.util.UUID;

public class ChatSessionResponse {
    private UUID id;
    private String title;
    private Instant createdAt;

    public ChatSessionResponse(ChatSession s) {
        this.id = s.getId();
        this.title = s.getTitle();
        this.createdAt = s.getCreatedAt();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
}
