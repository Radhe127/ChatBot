package com.radhe.springweb.chat;

import com.radhe.springweb.chat.dto.ChatMessageResponse;
import com.radhe.springweb.chat.dto.ChatSessionResponse;
import com.radhe.springweb.chat.dto.SendMessageRequest;
import com.radhe.springweb.common.CurrentUserService;
import com.radhe.springweb.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> listSessions() {
        User user = currentUserService.getCurrentUser();
        List<ChatSessionResponse> sessions = chatService.getSessions(user).stream()
                .map(ChatSessionResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(@RequestBody(required = false) Map<String, String> body) {
        User user = currentUserService.getCurrentUser();
        String title = body != null ? body.get("title") : null;
        ChatSession session = chatService.createSession(user, title);
        return ResponseEntity.ok(new ChatSessionResponse(session));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable UUID sessionId) {
        User user = currentUserService.getCurrentUser();
        List<ChatMessageResponse> messages = chatService.getMessages(user, sessionId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        User user = currentUserService.getCurrentUser();
        try {
            ChatService.ChatResult result = chatService.sendMessage(user, request);
            return ResponseEntity.ok(Map.of(
                    "sessionId", result.session().getId(),
                    "userMessage", new ChatMessageResponse(result.userMessage()),
                    "assistantMessage", new ChatMessageResponse(result.assistantMessage())
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
