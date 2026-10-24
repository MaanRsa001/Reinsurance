package com.rms.repository;

import com.rms.domain.ErpPolicyCover;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Soft-deleted rows (record_status = 'DELETED') are excluded from the named queries below. */
public interface ErpPolicyCoverRepository extends JpaRepository<ErpPolicyCover, Long> {

    @Query("select c from ErpPolicyCover c where c.policyTxnId = :policyTxnId and c.recordStatus <> 'DELETED' order by c.id")
    List<ErpPolicyCover> findByPolicyTxnId(@Param("policyTxnId") Long policyTxnId);
}
