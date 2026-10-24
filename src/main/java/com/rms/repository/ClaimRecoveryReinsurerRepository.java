package com.rms.repository;

import com.rms.domain.ClaimRecoveryReinsurer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRecoveryReinsurerRepository extends JpaRepository<ClaimRecoveryReinsurer, Long> {
    List<ClaimRecoveryReinsurer> findByClaimRecoveryId(Long claimRecoveryId);
}
