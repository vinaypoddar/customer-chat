package org.jobrad.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jobrad.backend.entity.Message;
import org.jobrad.backend.repository.MessageRepository;
import org.jobrad.backend.service.AgentAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AgentAssignmentService agentAssignmentService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocketHandler: New connection established, session: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, String> data = objectMapper.readValue(payload, Map.class);

            if ("subscribe".equals(data.get("type"))) {
                String userId = data.get("userId");
                sessions.put(userId, session);
            } else {
                // Assume it's a chat message
                Message chatMessage = objectMapper.readValue(payload, Message.class);
                // Save to database
                chatMessage.setTimestamp(LocalDateTime.now());
                messageRepository.save(chatMessage);
                routeMessage(chatMessage);
            }
        } catch (Exception e) {
            logger.error("WebSocketHandler: Error processing message: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void routeMessage(Message message) throws IOException {
        String sender = message.getSender();
        String recipient = message.getRecipient();
        // Verify assignment
        String assignedRecipient = sender.startsWith("Guest_")
                ? agentAssignmentService.getAssignedAgent(sender)
                : agentAssignmentService.getAssignedGuest(sender);
        if (recipient != null && assignedRecipient != null && recipient.equals(assignedRecipient)) {
            WebSocketSession recipientSession = sessions.get(recipient);
            if (recipientSession != null && recipientSession.isOpen()) {
                recipientSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } else {
                logger.info("WebSocketHandler: Recipient session not found or closed: {}", recipient);
            }
        } else {
            logger.warn("WebSocketHandler: Invalid recipient or assignment: sender={}, recipient={}", sender , recipient);
        }
        // Send to sender for confirmation
        WebSocketSession senderSession = sessions.get(sender);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } else {
            logger.warn("WebSocketHandler: Sender session not found or closed: {}", sender);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = sessions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (userId != null) {
            sessions.remove(userId);
            if (userId.startsWith("Guest_")) {
                agentAssignmentService.freeGuest(userId);
            } else {
                agentAssignmentService.freeAgent(userId);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocketHandler: Transport error, session: {}, error: {}", session.getId(), exception.getMessage());
        exception.printStackTrace();
    }
}