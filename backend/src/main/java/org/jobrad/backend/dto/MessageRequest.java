package org.jobrad.backend.dto;

import lombok.*;
import org.jobrad.backend.entity.UserRole;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    private Long id;

    private String content;

    private String recipient;

    private String sender;

    private String senderType;

    private LocalDateTime timestamp;

}