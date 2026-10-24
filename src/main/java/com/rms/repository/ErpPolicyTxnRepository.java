package com.rms.repository;

import com.rms.domain.ErpPolicyTxn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Soft-deleted rows (record_status = 'DELETED') are excluded from the named queries below. */
public interface ErpPolicyTxnRepository extends JpaRepository<ErpPolicyTxn, Long>, JpaSpecificationExecutor<ErpPolicyTxn> {

    @Query("select t from ErpPolicyTxn t where t.policyNo = :policyNo and t.recordStatus <> 'DELETED' order by t.txnDate, t.id")
    List<ErpPolicyTxn> findByPolicyNoOrderByTxnDateAscIdAsc(@Param("policyNo") String policyNo);

    @Query("select t from ErpPolicyTxn t where t.recordStatus <> 'DELETED' and (lower(t.policyNo) like lower(concat('%', :q, '%')) "
         + "or lower(t.insuredName) like lower(concat('%', :q, '%'))) order by t.policyNo, t.id")
    List<ErpPolicyTxn> search(@Param("q") String q);
}
