package com.rms.repository;

import com.rms.domain.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTypeRepository extends JpaRepository<MessageType, Long> {
}
