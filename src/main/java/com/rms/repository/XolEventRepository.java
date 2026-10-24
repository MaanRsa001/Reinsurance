package com.rms.repository;

import com.rms.domain.XolEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XolEventRepository extends JpaRepository<XolEvent, Long> {
    List<XolEvent> findByEventCode(String eventCode);
}
