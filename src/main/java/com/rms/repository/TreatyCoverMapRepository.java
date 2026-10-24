package com.rms.repository;

import com.rms.domain.TreatyCoverMap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreatyCoverMapRepository extends JpaRepository<TreatyCoverMap, Long> {

    /** Mappings of OTHER treaties (different code) with the same sequence and an overlapping period; the caller expands them to covers. */
    @Query("select m from TreatyCoverMap m, Treaty t where m.treatyId = t.id and m.sequenceNo = :seq "
         + "and t.treatyCode <> :code and t.recordStatus <> 'DELETED' and t.statusId in :statusIds "
         + "and m.effectiveFrom <= :toDate and m.effectiveTo >= :fromDate")
    List<TreatyCoverMap> findCandidates(@Param("seq") Integer seq, @Param("code") String code, @Param("statusIds") Collection<Long> statusIds,
                                        @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /** All mappings of treaties in one of the given statuses that are in force on a date (the caller filters by cover). */
    @Query("select m from TreatyCoverMap m, Treaty t where m.treatyId = t.id and t.recordStatus <> 'DELETED' and t.statusId in :statusIds "
         + "and m.effectiveFrom <= :d and m.effectiveTo >= :d")
    List<TreatyCoverMap> findInForce(@Param("statusIds") Collection<Long> statusIds, @Param("d") LocalDate d);
}
