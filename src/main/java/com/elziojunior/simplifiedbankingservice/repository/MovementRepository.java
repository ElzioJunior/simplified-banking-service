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
     * Reads one account-scoped page inside the resolved recent-history window,
     * while applying the optional type filter and caller-provided pagination.
     * The explicit type activation flag avoids an untyped null enum parameter
     * in PostgreSQL.
     */
    default Page<MovementEntity> findPageByAccountAndFilters(
            Long accountId,
            OffsetDateTime start,
            OffsetDateTime end,
            MovementType type,
            Pageable pageable) {
        return findPageByAccountAndResolvedFilters(
                accountId,
                start,
                end,
                type != null,
                type != null ? type : MovementType.CREDIT,
                pageable);
    }

    /** Executes the single pageable query with every PostgreSQL parameter carrying an explicit type. */
    @Query("""
            select movement
            from MovementEntity movement
            where movement.account.id = :accountId
              and movement.createdAt >= :start
              and movement.createdAt < :end
              and (:hasType = false or movement.type = :type)
            """)
    Page<MovementEntity> findPageByAccountAndResolvedFilters(
            @Param("accountId") Long accountId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("hasType") boolean hasType,
            @Param("type") MovementType type,
            Pageable pageable);
}
