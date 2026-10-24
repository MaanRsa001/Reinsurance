package com.rms.repository;

import com.rms.domain.Reinsurer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReinsurerRepository extends JpaRepository<Reinsurer, Long> {
}
