package com.rms.repository;

import com.rms.domain.ClaimRecovery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRecoveryRepository extends JpaRepository<ClaimRecovery, Long> {
    List<ClaimRecovery> findByClaimTxnId(Long claimTxnId);

    List<ClaimRecovery> findByXolEventId(Long xolEventId);
}
