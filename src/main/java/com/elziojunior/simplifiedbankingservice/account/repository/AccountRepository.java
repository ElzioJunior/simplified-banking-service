package com.elziojunior.simplifiedbankingservice.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.elziojunior.simplifiedbankingservice.account.model.Account;

/** Persistence boundary for Account records managed by Spring Data JPA. */
public interface AccountRepository extends JpaRepository<Account, Long> {
}
