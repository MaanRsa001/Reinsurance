package com.rms.repository;

import com.rms.domain.XolEventLoss;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XolEventLossRepository extends JpaRepository<XolEventLoss, Long> {
    List<XolEventLoss> findByClaimNo(String claimNo);
}
