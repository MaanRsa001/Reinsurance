package com.rms.repository;

import com.rms.domain.RefLookupValue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefLookupValueRepository extends JpaRepository<RefLookupValue, Long> {

    @Query("select v from RefLookupValue v where v.typeId = (select t.id from RefLookupType t where t.code = :typeCode) and v.code = :code")
    Optional<RefLookupValue> findByTypeCodeAndCode(@Param("typeCode") String typeCode, @Param("code") String code);

    @Query("select v from RefLookupValue v where v.typeId = (select t.id from RefLookupType t where t.code = :typeCode) and v.activeFlag = true order by v.sortOrder")
    List<RefLookupValue> findActiveByType(@Param("typeCode") String typeCode);
}
