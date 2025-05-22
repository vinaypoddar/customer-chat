package org.jobrad.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class MessageResponse {

    private Long id;

    private String content;

    private String recipient;

    private String sender;

    private String senderType;

    private LocalDateTime timestamp;

}