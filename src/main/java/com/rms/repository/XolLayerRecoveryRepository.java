package com.rms.repository;

import com.rms.domain.XolLayerRecovery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XolLayerRecoveryRepository extends JpaRepository<XolLayerRecovery, Long> {
    List<XolLayerRecovery> findByXolEventId(Long xolEventId);
}
