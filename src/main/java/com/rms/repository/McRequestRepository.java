package com.rms.repository;

import com.rms.domain.McRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McRequestRepository extends JpaRepository<McRequest, Long> {
    List<McRequest> findByStatusIdOrderByIdDesc(Long statusId);

    List<McRequest> findByMakerUserOrderByIdDesc(String makerUser);

    List<McRequest> findByEntityTypeIdAndEntityIdOrderByIdDesc(Long entityTypeId, Long entityId);

    List<McRequest> findAllByOrderByIdDesc();
}
