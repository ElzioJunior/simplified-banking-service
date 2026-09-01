package com.elziojunior.simplifiedbankingservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import com.elziojunior.simplifiedbankingservice.exception.AccountCreationValidationException;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateAccountDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreatedAccountDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elziojunior.simplifiedbankingservice.model.entity.AccountEntity;
import com.elziojunior.simplifiedbankingservice.repository.AccountRepository;

/** Creates accounts while enforcing the application-owned opening invariants. */
@Service
public class AccountService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MONETARY_SCALE = 2;
    private static final int MAX_INTEGER_DIGITS = 17;

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    /**
     * Validates, normalizes, and persists one account so invalid input cannot
     * reach the repository and successful creation has one authoritative UTC
     * timestamp.
     *
     * @param account requested account data
     * @return the generated identifier and persisted account values
     * @throws AccountCreationValidationException when an account invariant is violated
     */
    @Transactional
    public CreatedAccountDto createAccount(CreateAccountDto account) {
        if (account == null) {
            throw new AccountCreationValidationException("Account creation data is required.");
        }

        String name = validateName(account.name());
        BigDecimal balance = normalizeBalance(account.initialBalance());
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);

        AccountEntity savedAccountEntity = accountRepository.save(new AccountEntity(name, balance, createdAt));
        return new CreatedAccountDto(
                savedAccountEntity.getId(),
                savedAccountEntity.getName(),
                savedAccountEntity.getBalance(),
                savedAccountEntity.getCreatedAt());
    }

    /**
     * Enforces the required, nonblank, database-sized name contract before a
     * persistence entity is created.
     */
    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new AccountCreationValidationException("Account name is required.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new AccountCreationValidationException("Account name must not exceed 255 characters.");
        }
        return name;
    }

    /**
     * Rejects negative input before rounding, applies ADR-0022 normalization,
     * and prevents a value that cannot fit PostgreSQL {@code NUMERIC(19,2)}
     * from reaching the database.
     */
    private BigDecimal normalizeBalance(BigDecimal initialBalance) {
        if (initialBalance == null) {
            throw new AccountCreationValidationException("Initial balance is required.");
        }
        if (initialBalance.signum() < 0) {
            throw new AccountCreationValidationException("Initial balance must be greater than or equal to zero.");
        }

        BigDecimal normalizedBalance = initialBalance.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN);
        int integerDigits = Math.max(0, normalizedBalance.precision() - normalizedBalance.scale());
        if (integerDigits > MAX_INTEGER_DIGITS) {
            throw new AccountCreationValidationException("Initial balance exceeds the supported monetary range.");
        }
        return normalizedBalance;
    }
}
