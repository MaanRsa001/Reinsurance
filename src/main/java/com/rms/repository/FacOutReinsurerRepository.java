package com.rms.repository;

import com.rms.domain.FacOutReinsurer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacOutReinsurerRepository extends JpaRepository<FacOutReinsurer, Long> {
    List<FacOutReinsurer> findByPlacementId(Long placementId);
}
