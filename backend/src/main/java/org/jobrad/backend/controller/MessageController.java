package org.jobrad.backend.controller;

import org.jobrad.backend.dto.MessageRequest;
import org.jobrad.backend.dto.MessageResponse;
import org.jobrad.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping
    public List<MessageResponse> getMessages(@RequestParam String userId) {
        return messageService.getMessagesForUser(userId);
    }

    @MessageMapping("/chat")
    public void sendMessage(MessageRequest message) {
        messageService.saveAndSendMessage(message);
    }
}