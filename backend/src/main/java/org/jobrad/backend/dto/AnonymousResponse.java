package org.jobrad.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AnonymousResponse {
    private final String guestId;
    private final String agentId;
}