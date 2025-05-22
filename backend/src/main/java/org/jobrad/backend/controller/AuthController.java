package org.jobrad.backend.controller;

import org.jobrad.backend.dto.AnonymousResponse;
import org.jobrad.backend.service.AgentAssignmentService;
import org.jobrad.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AgentAssignmentService agentAssignmentService;

    @PostMapping("/login")
    public ResponseEntity<String> login() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok("Logged in as " + username);
    }

    @PostMapping("/anonymous")
    public ResponseEntity<?> createAnonymousUser() {
        String guestId = userService.createAnonymousCustomer();
        String agentId = agentAssignmentService.assignAgent(guestId);
        if (agentId == null) {
            return ResponseEntity.status(503).body("No agents available, you are in the queue");
        }
        return ResponseEntity.ok(new AnonymousResponse(guestId, agentId));
    }

}