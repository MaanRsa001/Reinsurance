package com.rms.repository;

import com.rms.domain.CessionLayer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CessionLayerRepository extends JpaRepository<CessionLayer, Long> {
    List<CessionLayer> findByCessionIdOrderByLayerNo(Long cessionId);
}
