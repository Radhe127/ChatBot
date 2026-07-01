package com.radhe.springweb.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class SendMessageRequest {

    // Optional - if null, a new session is created
    private UUID sessionId;

    @NotBlank
    private String message;

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
