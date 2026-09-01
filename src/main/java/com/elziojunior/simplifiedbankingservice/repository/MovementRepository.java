package com.elziojunior.simplifiedbankingservice.repository;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.elziojunior.simplifiedbankingservice.model.entity.MovementEntity;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;

/** Persistence boundary for immutable financial movements. */
public interface MovementRepository extends JpaRepository<MovementEntity, Long> {

    /**
     * Reads one account-scoped page while applying only the optional filters
     * approved for movement history, leaving ordering and page bounds explicit
     * in the caller-provided pageable.
     */
    @Query("""
            select movement
            from MovementEntity movement
            where movement.account.id = :accountId
              and (:start is null or movement.createdAt >= :start)
              and (:end is null or movement.createdAt < :end)
              and (:type is null or movement.type = :type)
            """)
    Page<MovementEntity> findPageByAccountAndFilters(
            @Param("accountId") Long accountId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("type") MovementType type,
            Pageable pageable);
}
