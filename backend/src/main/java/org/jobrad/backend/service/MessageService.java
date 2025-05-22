package org.jobrad.backend.service;

import org.jobrad.backend.dto.MessageRequest;
import org.jobrad.backend.dto.MessageResponse;
import org.jobrad.backend.entity.Message;

import java.util.List;

public interface MessageService {
    void saveAndSendMessage(MessageRequest message);

    List<MessageResponse> getMessagesForUser(String userId);
}
