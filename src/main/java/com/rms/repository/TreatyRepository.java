package com.rms.repository;

import com.rms.domain.Treaty;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreatyRepository extends JpaRepository<Treaty, Long>, JpaSpecificationExecutor<Treaty> {

    boolean existsByTreatyCode(String treatyCode);

    List<Treaty> findByTreatyCodeAndRecordStatusNotOrderByVersionNoDesc(String treatyCode, String recordStatus);

    @Query("select coalesce(max(t.versionNo), 0) from Treaty t where t.treatyCode = :code")
    int maxVersion(@Param("code") String code);

    boolean existsByTreatyCodeAndStatusIdInAndRecordStatusNot(String treatyCode, Collection<Long> statusIds, String recordStatus);
}
