package com.elziojunior.simplifiedbankingservice.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elziojunior.simplifiedbankingservice.account.model.Account;
import com.elziojunior.simplifiedbankingservice.account.repository.AccountRepository;

/** Creates accounts while enforcing the application-owned opening invariants. */
@Service
public class CreateAccountService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MONETARY_SCALE = 2;
    private static final int MAX_INTEGER_DIGITS = 17;

    private final AccountRepository accountRepository;
    private final Clock clock;

    public CreateAccountService(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    /**
     * Validates, normalizes, and persists one account so invalid input cannot
     * reach the repository and successful creation has one authoritative UTC
     * timestamp.
     *
     * @param command requested account data
     * @return the generated identifier and persisted account values
     * @throws AccountCreationValidationException when an account invariant is violated
     */
    @Transactional
    public CreatedAccount create(CreateAccountCommand command) {
        if (command == null) {
            throw new AccountCreationValidationException("Account creation data is required.");
        }

        String name = validateName(command.name());
        BigDecimal balance = normalizeBalance(command.initialBalance());
        OffsetDateTime createdAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);

        Account savedAccount = accountRepository.save(new Account(name, balance, createdAt));
        return new CreatedAccount(
                savedAccount.getId(),
                savedAccount.getName(),
                savedAccount.getBalance(),
                savedAccount.getCreatedAt());
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
