package com.elziojunior.simplifiedbankingservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.elziojunior.simplifiedbankingservice.exception.TransferConflictException;
import com.elziojunior.simplifiedbankingservice.exception.TransferNotFoundException;
import com.elziojunior.simplifiedbankingservice.exception.TransferValidationException;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;
import com.elziojunior.simplifiedbankingservice.model.entity.AccountEntity;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementEntity;
import com.elziojunior.simplifiedbankingservice.model.entity.MovementType;
import com.elziojunior.simplifiedbankingservice.model.entity.TransferIdempotencyTokenEntity;
import com.elziojunior.simplifiedbankingservice.repository.AccountRepository;
import com.elziojunior.simplifiedbankingservice.repository.MovementRepository;
import com.elziojunior.simplifiedbankingservice.repository.TransferIdempotencyTokenRepository;

/** Executes one complete idempotent account-to-account financial operation. */
@Service
public class CreateTransferService {

    private static final int MONETARY_SCALE = 2;
    private static final int MAX_INTEGER_DIGITS = 17;

    private final TransferIdempotencyTokenRepository tokenRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final TransferNotificationAfterCommitScheduler notificationScheduler;
    private final TransferLockTimeoutConfigurer lockTimeoutConfigurer;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    public CreateTransferService(
            TransferIdempotencyTokenRepository tokenRepository,
            AccountRepository accountRepository,
            MovementRepository movementRepository,
            TransferNotificationAfterCommitScheduler notificationScheduler,
            TransferLockTimeoutConfigurer lockTimeoutConfigurer,
            UuidGenerator uuidGenerator,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.notificationScheduler = notificationScheduler;
        this.lockTimeoutConfigurer = lockTimeoutConfigurer;
        this.uuidGenerator = uuidGenerator;
        this.clock = clock;
    }

    /**
     * Serializes token use and account mutation inside one READ_COMMITTED
     * transaction so a completed retry is replayed and every new operation is
     * all-or-nothing across balances, movements, and token association. A new
     * completion also schedules best-effort event publication after commit.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompletedTransferDto create(CreateTransferDto transfer) {
        ValidatedTransfer requested = validate(transfer);
        lockTimeoutConfigurer.configureCurrentTransaction();
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        TransferIdempotencyTokenEntity token = tokenRepository.findByTokenForUpdate(requested.token())
                .orElseThrow(() -> new TransferConflictException("The idempotency token is invalid."));

        if (token.getUsedAt() != null) {
            return replay(token, requested);
        }
        if (!now.isBefore(token.getExpiresAt())) {
            throw new TransferConflictException("The idempotency token has expired.");
        }

        AccountEntity firstLocked = lockAccount(
                Math.min(requested.sourceAccountId(), requested.destinationAccountId()));
        AccountEntity secondLocked = lockAccount(
                Math.max(requested.sourceAccountId(), requested.destinationAccountId()));
        AccountEntity source = firstLocked.getId().equals(requested.sourceAccountId()) ? firstLocked : secondLocked;
        AccountEntity destination = firstLocked.getId().equals(requested.destinationAccountId())
                ? firstLocked
                : secondLocked;

        if (source.getBalance().compareTo(requested.amount()) < 0) {
            throw new TransferConflictException("The source account has insufficient funds.");
        }

        UUID operationId = uuidGenerator.generate();
        UUID eventId = uuidGenerator.generate();
        source.updateBalance(source.getBalance().subtract(requested.amount()));
        destination.updateBalance(destination.getBalance().add(requested.amount()));
        accountRepository.saveAll(List.of(source, destination));
        movementRepository.saveAll(List.of(
                new MovementEntity(source, operationId, MovementType.DEBIT, requested.amount(), now),
                new MovementEntity(destination, operationId, MovementType.CREDIT, requested.amount(), now)));
        token.associate(operationId, source.getId(), destination.getId(), requested.amount(), now);
        tokenRepository.save(token);
        notificationScheduler.schedule(new TransferCompletedNotification(
                eventId,
                operationId,
                source.getId(),
                TransferCompletedNotification.TRANSFER_COMPLETED,
                requested.amount(),
                now));
        return new CompletedTransferDto(operationId, source.getId(), destination.getId(), requested.amount());
    }

    /** Validates transport-independent invariants before any lock or persistence interaction. */
    private ValidatedTransfer validate(CreateTransferDto transfer) {
        if (transfer == null || transfer.token() == null) {
            throw new TransferValidationException("An idempotency token is required.");
        }
        if (transfer.sourceAccountId() == null || transfer.destinationAccountId() == null) {
            throw new TransferValidationException("Source and destination accounts are required.");
        }
        if (transfer.sourceAccountId().equals(transfer.destinationAccountId())) {
            throw new TransferConflictException("Source and destination accounts must be different.");
        }
        BigDecimal amount = normalizeAmount(transfer.amount());
        return new ValidatedTransfer(
                transfer.token(), transfer.sourceAccountId(), transfer.destinationAccountId(), amount);
    }

    /** Applies ADR-0022 and rejects values that cannot represent a positive NUMERIC(19,2) amount. */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransferValidationException("Transfer amount must be greater than zero.");
        }
        BigDecimal normalized = amount.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        int integerDigits = Math.max(0, normalized.precision() - normalized.scale());
        if (normalized.signum() <= 0 || integerDigits > MAX_INTEGER_DIGITS) {
            throw new TransferValidationException("Transfer amount is outside the supported monetary range.");
        }
        return normalized;
    }

    /** Loads one account under a write lock and returns a stable public absence failure. */
    private AccountEntity lockAccount(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new TransferNotFoundException("A transfer account was not found."));
    }

    /** Replays only an identical normalized payload so token reuse cannot change financial intent. */
    private CompletedTransferDto replay(TransferIdempotencyTokenEntity token, ValidatedTransfer requested) {
        boolean samePayload = token.getSourceAccountId().equals(requested.sourceAccountId())
                && token.getDestinationAccountId().equals(requested.destinationAccountId())
                && token.getAmount().compareTo(requested.amount()) == 0;
        if (!samePayload) {
            throw new TransferConflictException("The idempotency token is associated with another transfer.");
        }
        return new CompletedTransferDto(
                token.getOperationId(), token.getSourceAccountId(), token.getDestinationAccountId(), token.getAmount());
    }

    private record ValidatedTransfer(UUID token, Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
    }
}
