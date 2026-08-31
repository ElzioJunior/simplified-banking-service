package com.elziojunior.simplifiedbankingservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.elziojunior.simplifiedbankingservice.model.entity.AccountEntity;

import jakarta.persistence.LockModeType;

/** Persistence boundary for Account records managed by Spring Data JPA. */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    /** Locks one account so callers can acquire both transfer accounts in deterministic order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountEntity account where account.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") Long id);
}
