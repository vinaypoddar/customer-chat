package org.jobrad.backend.service;

public interface AgentAssignmentService {
    String assignAgent(String guestId);

    String getAssignedAgent(String guestId);

    String getAssignedGuest(String agentId);

    void freeAgent(String agentId);

    void freeGuest(String guestId);

    void queueGuest(String guestId);
}
