package com.elziojunior.simplifiedbankingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementEntity;

/** Persistence boundary for immutable financial movements. */
public interface MovementRepository extends JpaRepository<MovementEntity, Long> {
}
