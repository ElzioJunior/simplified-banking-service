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
     * in the caller-provided pageable. Explicit activation flags avoid sending
     * untyped null comparison parameters to PostgreSQL.
     */
    default Page<MovementEntity> findPageByAccountAndFilters(
            Long accountId,
            OffsetDateTime start,
            OffsetDateTime end,
            MovementType type,
            Pageable pageable) {
        OffsetDateTime typedDatePlaceholder = OffsetDateTime.parse("2000-01-01T00:00:00Z");
        return findPageByAccountAndResolvedFilters(
                accountId,
                start != null,
                start != null ? start : typedDatePlaceholder,
                end != null,
                end != null ? end : typedDatePlaceholder,
                type != null,
                type != null ? type : MovementType.CREDIT,
                pageable);
    }

    /** Executes the single pageable query with every PostgreSQL parameter carrying an explicit type. */
    @Query("""
            select movement
            from MovementEntity movement
            where movement.account.id = :accountId
              and (:hasStart = false or movement.createdAt >= :start)
              and (:hasEnd = false or movement.createdAt < :end)
              and (:hasType = false or movement.type = :type)
            """)
    Page<MovementEntity> findPageByAccountAndResolvedFilters(
            @Param("accountId") Long accountId,
            @Param("hasStart") boolean hasStart,
            @Param("start") OffsetDateTime start,
            @Param("hasEnd") boolean hasEnd,
            @Param("end") OffsetDateTime end,
            @Param("hasType") boolean hasType,
            @Param("type") MovementType type,
            Pageable pageable);
}
