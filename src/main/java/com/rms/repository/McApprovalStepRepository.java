package com.rms.repository;

import com.rms.domain.McApprovalStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McApprovalStepRepository extends JpaRepository<McApprovalStep, Long> {
    List<McApprovalStep> findByRequestIdOrderByLevelNoAsc(Long requestId);
}
