package org.jobrad.backend.service;

public interface UserService {
    String createAnonymousCustomer();

    String assignAgent(String customerId);
}
