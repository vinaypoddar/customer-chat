package org.jobrad.backend.service;

import jakarta.annotation.PostConstruct;
import org.jobrad.backend.entity.User;
import org.jobrad.backend.entity.UserRole;
import org.jobrad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Create a default agent
        User agent = User.builder()
                .id(UUID.randomUUID())
                .username("agent1")
                .password(passwordEncoder.encode("password"))
                .role(UserRole.SUPPORT_AGENT)
                .build();
        userRepository.save(agent);
    }

    @Override
    public String createAnonymousCustomer() {
        String guestUsername = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
        User customer = User.builder()
                .id(UUID.randomUUID())
                .username(guestUsername)
                .password("") // No password for anonymous users
                .role(UserRole.CUSTOMER)
                .build();
        userRepository.save(customer);
        return guestUsername;
    }

    @Override
    public String assignAgent(String customerId) {
        // For simplicity, assign the first available agent
        User agent = userRepository.findByUsername("agent1");
        return agent != null ? agent.getUsername() : null;
    }
}