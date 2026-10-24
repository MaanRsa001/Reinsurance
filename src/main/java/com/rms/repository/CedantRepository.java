package com.rms.repository;

import com.rms.domain.Cedant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CedantRepository extends JpaRepository<Cedant, Long> {
}
