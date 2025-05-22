package org.jobrad.backend.controller;

import org.jobrad.backend.dto.AssignmentResponse;
import org.jobrad.backend.service.AgentAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/assignment")
public class AssignmentController {

    @Autowired
    private AgentAssignmentService agentAssignmentService;

    @GetMapping("/{agentId}")
    public ResponseEntity<?> getAssignedGuest(@PathVariable String agentId) {
        String guestId = agentAssignmentService.getAssignedGuest(agentId);
        return ResponseEntity.ok(new AssignmentResponse(Objects.requireNonNullElse(guestId, "")));
    }

    @PostMapping("/free/{agentId}")
    public ResponseEntity<?> freeAgent(@PathVariable String agentId) {
        agentAssignmentService.freeAgent(agentId);
        return ResponseEntity.ok("Agent freed");
    }

    @PostMapping("/guest/{guestId}")
    public ResponseEntity<?> getAssignedAgent(@PathVariable String guestId) {
        agentAssignmentService.freeGuest(guestId);
        return ResponseEntity.ok("Agent freed");
    }

    @PostMapping("/queue/guest/{guestId}")
    public ResponseEntity<?> queueGuest(@PathVariable String guestId) {
        agentAssignmentService.queueGuest(guestId);
        return ResponseEntity.ok("Guest queued");
    }
}