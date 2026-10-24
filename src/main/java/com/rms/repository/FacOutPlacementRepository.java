package com.rms.repository;

import com.rms.domain.FacOutPlacement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacOutPlacementRepository extends JpaRepository<FacOutPlacement, Long> {
    List<FacOutPlacement> findByPolicyNo(String policyNo);
}
