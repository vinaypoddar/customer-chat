package org.jobrad.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MESSAGE")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private UserRole senderType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

}