package com.rms.repository;

import com.rms.domain.InboundMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundMessageRepository extends JpaRepository<InboundMessage, Long> {
}
