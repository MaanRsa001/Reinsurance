package com.rms.repository;

import com.rms.domain.DafAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DafAccountRepository extends JpaRepository<DafAccount, Long> {
}
