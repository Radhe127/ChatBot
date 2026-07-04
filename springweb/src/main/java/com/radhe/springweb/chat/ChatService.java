package com.radhe.springweb.chat;

import com.radhe.springweb.user.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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

        public ChatResult sendMessage(User user, UUID sessionId, String message, List<MultipartFile> attachments) {
        String safeMessage = message == null ? "" : message.trim();
        List<String> attachmentNames = normalizeAttachmentNames(attachments);
        ChatSession session = sessionId != null
            ? requireOwnedSession(user, sessionId)
            : createSession(user, deriveTitle(safeMessage.isEmpty() ? describeAttachments(attachmentNames) : safeMessage));

        // Reconstruct prior turns so the model has conversational context
        List<ChatMessageEntity> priorEntities = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<Message> priorMessages = priorEntities.stream()
                .map(e -> (Message) ("user".equals(e.getRole())
                        ? new UserMessage(e.getContent())
                        : new AssistantMessage(e.getContent())))
                .collect(Collectors.toList());
        String userPrompt = buildUserPrompt(safeMessage, attachmentNames);
        priorMessages.add(new UserMessage(userPrompt));

        String aiReply = generateReply(priorMessages, safeMessage, attachmentNames);

        ChatMessageEntity userMsg = messageRepository.save(
            new ChatMessageEntity(session, "user", userPrompt));
        ChatMessageEntity assistantMsg = messageRepository.save(
                new ChatMessageEntity(session, "assistant", aiReply));

        return new ChatResult(session, userMsg, assistantMsg);
    }

        private String generateReply(List<Message> priorMessages, String latestMessage, List<String> attachmentNames) {
        try {
            String content = chatClient.prompt()
                    .messages(priorMessages)
                    .call()
                    .content();
            if (content != null && !content.isBlank()) {
                return content;
            }
        } catch (Exception ignored) {
            // Fall through to a local reply so the app stays usable without an external model.
        }

        String trimmed = latestMessage == null ? "" : latestMessage.trim();
        String attachmentSummary = describeAttachments(attachmentNames);
        if (trimmed.isEmpty()) {
            return attachmentSummary.isEmpty()
                    ? "I’m running in local mode and need a message to respond to."
                    : "I received " + attachmentSummary + ". Add a note and I’ll respond to it.";
        }
        if (attachmentSummary.isEmpty()) {
            return "Local assistant mode: I received your message and saved it, but the external AI service is unavailable right now. You said: \""
                    + trimmed + "\"";
        }
        return "Local assistant mode: I received your message and " + attachmentSummary
                + ", but the external AI service is unavailable right now. Your note: \"" + trimmed + "\"";
    }

    private String buildUserPrompt(String message, List<String> attachmentNames) {
        String attachmentSummary = describeAttachments(attachmentNames);
        if (attachmentSummary.isEmpty()) {
            return message;
        }
        if (message.isEmpty()) {
            return "Uploaded attachments: " + attachmentSummary + ".";
        }
        return message + "\n\nUploaded attachments: " + attachmentSummary + ".";
    }

    private List<String> normalizeAttachmentNames(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .filter(attachment -> attachment != null && !attachment.isEmpty())
                .map(MultipartFile::getOriginalFilename)
                .filter(filename -> filename != null && !filename.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String describeAttachments(List<String> attachmentNames) {
        if (attachmentNames == null || attachmentNames.isEmpty()) {
            return "";
        }
        if (attachmentNames.size() == 1) {
            return "1 file attached: " + attachmentNames.get(0);
        }
        return attachmentNames.size() + " files attached: " + String.join(", ", attachmentNames);
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
