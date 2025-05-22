package org.jobrad.backend.repository;

import org.jobrad.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySenderOrRecipientOrderByTimestampAsc(String sender, String recipient);

}