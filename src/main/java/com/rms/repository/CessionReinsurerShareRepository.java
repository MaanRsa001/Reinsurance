package com.rms.repository;

import com.rms.domain.CessionReinsurerShare;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CessionReinsurerShareRepository extends JpaRepository<CessionReinsurerShare, Long> {
    List<CessionReinsurerShare> findByCessionLayerId(Long cessionLayerId);
}
