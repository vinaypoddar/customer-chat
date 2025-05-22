package org.jobrad.backend.service;

import org.jobrad.backend.dto.AssignmentMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentAssignmentServiceImpl implements AgentAssignmentService {
    private final Map<String, String> agentToGuest = new ConcurrentHashMap<>();
    private final Map<String, String> guestToAgent = new ConcurrentHashMap<>();
    private final Set<String> agentAvailability = new LinkedHashSet<>();
    private final Set<String> queuingGuests = new LinkedHashSet<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Initialize with agent1 as available
    public AgentAssignmentServiceImpl() {
        agentAvailability.add("agent1");
    }

    public synchronized String assignAgent(String guestId) {
        Optional<String> agentAvailable = agentAvailability.stream().findFirst();
        if (agentAvailable.isPresent()) {
            String agentId = agentAvailable.get();
            agentToGuest.put(agentId, guestId);
            guestToAgent.put(guestId, agentId);
            agentAvailability.remove(agentId);
            // Notify guest
            messagingTemplate.convertAndSendToUser(
                    guestId,
                    "/queue/assignment",
                    new AssignmentMessageResponse(agentId)
            );

            // Notify agent
            messagingTemplate.convertAndSendToUser(
                    agentId,
                    "/queue/assignment",
                    new AssignmentMessageResponse(guestId)
            );
            return agentId;
        }

        queuingGuests.add(guestId);
        return null;
    }

    public String getAssignedAgent(String guestId) {
        return guestToAgent.get(guestId);
    }

    public String getAssignedGuest(String agentId) {
        String guestId = agentToGuest.get(agentId);
        if(guestId == null) {
            Optional<String> waitingGuest = queuingGuests.stream().findFirst();
            if (waitingGuest.isPresent()) {
                guestId = waitingGuest.get();
                agentToGuest.put(agentId, guestId);
                guestToAgent.put(guestId, agentId);
                queuingGuests.remove(guestId);
                // Notify guest
                messagingTemplate.convertAndSendToUser(
                        guestId,
                        "/queue/assignment",
                        new AssignmentMessageResponse(agentId)
                );
            }
        }
        return guestId;
    }

    public synchronized void freeAgent(String agentId) {
        String guestId = agentToGuest.get(agentId);
        if (guestId != null) {
            agentToGuest.remove(agentId);
            guestToAgent.remove(guestId);
            agentAvailability.add(agentId);
            queuingGuests.add(guestId);
            // Notify guest
            messagingTemplate.convertAndSendToUser(
                    guestId,
                    "/queue/assignment",
                    new AssignmentMessageResponse("")
            );
        }
    }

    public synchronized void freeGuest(String guestId) {
        String agentId = guestToAgent.get(guestId);
        if (agentId != null) {
            agentToGuest.remove(agentId);
            guestToAgent.remove(guestId);
            // Remove guest from queue if present
            Optional<String> first = queuingGuests.stream().findFirst();
            if (first.isPresent()) {
                guestToAgent.put(first.get(), agentId);
                messagingTemplate.convertAndSendToUser(
                        first.get(),
                        "/queue/assignment",
                        new AssignmentMessageResponse(agentId)
                );
                queuingGuests.remove(first.get());
            } else {
                agentAvailability.add(agentId);
                // Notify guest
                messagingTemplate.convertAndSendToUser(
                        agentId,
                        "/queue/assignment",
                        new AssignmentMessageResponse("")
                );
            }
        }
    }

    @Override
    public void queueGuest(String guestId) {
        queuingGuests.add(guestId);
        // Notify guest
        messagingTemplate.convertAndSendToUser(
                guestId,
                "/queue/assignment",
                new AssignmentMessageResponse("You are in the queue")
        );
    }
}