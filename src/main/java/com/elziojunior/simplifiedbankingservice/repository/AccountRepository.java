package com.elziojunior.simplifiedbankingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.elziojunior.simplifiedbankingservice.model.entity.AccountEntity;

/** Persistence boundary for Account records managed by Spring Data JPA. */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
}
