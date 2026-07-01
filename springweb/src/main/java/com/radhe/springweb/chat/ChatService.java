package com.radhe.springweb.chat;

import com.radhe.springweb.chat.dto.SendMessageRequest;
import com.radhe.springweb.user.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public ChatService(ChatClient.Builder chatClientBuilder,
                        ChatSessionRepository sessionRepository,
                        ChatMessageRepository messageRepository) {
        this.chatClient = chatClientBuilder.build();
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public ChatSession createSession(User user, String title) {
        String safeTitle = (title == null || title.isBlank()) ? "New chat" : title;
        return sessionRepository.save(new ChatSession(user, safeTitle));
    }

    public List<ChatSession> getSessions(User user) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<ChatMessageEntity> getMessages(User user, UUID sessionId) {
        ChatSession session = requireOwnedSession(user, sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
    }

    public record ChatResult(ChatSession session, ChatMessageEntity userMessage, ChatMessageEntity assistantMessage) {}

    public ChatResult sendMessage(User user, SendMessageRequest request) {
        ChatSession session = request.getSessionId() != null
                ? requireOwnedSession(user, request.getSessionId())
                : createSession(user, deriveTitle(request.getMessage()));

        // Reconstruct prior turns so the model has conversational context
        List<ChatMessageEntity> priorEntities = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<Message> priorMessages = priorEntities.stream()
                .map(e -> (Message) ("user".equals(e.getRole())
                        ? new UserMessage(e.getContent())
                        : new AssistantMessage(e.getContent())))
                .collect(Collectors.toList());
        priorMessages.add(new UserMessage(request.getMessage()));

        String aiReply = chatClient.prompt()
                .messages(priorMessages)
                .call()
                .content();

        ChatMessageEntity userMsg = messageRepository.save(
                new ChatMessageEntity(session, "user", request.getMessage()));
        ChatMessageEntity assistantMsg = messageRepository.save(
                new ChatMessageEntity(session, "assistant", aiReply));

        return new ChatResult(session, userMsg, assistantMsg);
    }

    private ChatSession requireOwnedSession(User user, UUID sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new SecurityException("This chat session does not belong to the current user");
        }
        return session;
    }

    private String deriveTitle(String firstMessage) {
        String trimmed = firstMessage.trim();
        return trimmed.length() > 50 ? trimmed.substring(0, 50) + "…" : trimmed;
    }
}
