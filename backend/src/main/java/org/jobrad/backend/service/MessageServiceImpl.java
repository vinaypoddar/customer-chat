package org.jobrad.backend.service;

import org.jobrad.backend.dto.MessageRequest;
import org.jobrad.backend.dto.MessageResponse;
import org.jobrad.backend.entity.Message;
import org.jobrad.backend.entity.UserRole;
import org.jobrad.backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void saveAndSendMessage(MessageRequest message) {
        Message messageEntity = convertToEntity(message);
        Message save = messageRepository.save(messageEntity);

        // Send to recipient
        messagingTemplate.convertAndSendToUser(
                message.getRecipient(),
                "/queue/messages",
                message
        );
        // Send to sender (for confirmation)
        messagingTemplate.convertAndSendToUser(
                message.getSender(),
                "/queue/messages",
                message
        );
    }

    @Override
    public List<MessageResponse> getMessagesForUser(String userId) {
        List<Message> messages = messageRepository.findBySenderOrRecipientOrderByTimestampAsc(userId, userId);
        return messages.stream().map(this::convertToResponseDto).collect(Collectors.toList());
    }

    private MessageResponse convertToResponseDto(Message message) {
        return MessageResponse.builder()
                .sender(message.getSender())
                .recipient(message.getRecipient())
                .senderType(message.getSenderType().name())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }

    private Message convertToEntity(MessageRequest message) {
        return Message.builder()
                .sender(message.getSender())
                .recipient(message.getRecipient())
                .senderType(UserRole.valueOf(message.getSenderType()))
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }
}