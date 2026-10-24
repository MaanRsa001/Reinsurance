package com.rms.repository;

import com.rms.domain.ErpClaimTxn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErpClaimTxnRepository extends JpaRepository<ErpClaimTxn, Long> {
    List<ErpClaimTxn> findByClaimNoOrderByTxnDateAscIdAsc(String claimNo);

    @Query("select t from ErpClaimTxn t where lower(t.claimNo) like lower(concat('%', :q, '%')) or lower(t.policyNo) like lower(concat('%', :q, '%')) order by t.claimNo, t.id")
    List<ErpClaimTxn> search(@Param("q") String q);
}
