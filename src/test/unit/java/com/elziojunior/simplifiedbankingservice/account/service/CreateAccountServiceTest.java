package com.elziojunior.simplifiedbankingservice.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.elziojunior.simplifiedbankingservice.account.model.Account;
import com.elziojunior.simplifiedbankingservice.account.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class CreateAccountServiceTest {

    private static final Instant CREATION_INSTANT = Instant.parse("2026-08-31T14:00:00Z");

    @Mock
    private AccountRepository accountRepository;

    private CreateAccountService service;

    @BeforeEach
    void setUp() {
        service = new CreateAccountService(accountRepository, Clock.fixed(CREATION_INSTANT, ZoneOffset.UTC));
    }

    /**
     * Proves that valid input is normalized, timestamped once, persisted, and
     * mapped with the database-generated ID required by the API contract.
     */
    @Test
    void createsAnAccountWithNormalizedValuesAndGeneratedId() {
        stubPersistedAccount(42L, "John Doe", new BigDecimal("123.45"));

        CreatedAccount result = service.create(new CreateAccountCommand("John Doe", new BigDecimal("123.450")));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account submittedAccount = accountCaptor.getValue();
        assertThat(submittedAccount.getName()).isEqualTo("John Doe");
        assertThat(submittedAccount.getBalance()).isEqualByComparingTo("123.45");
        assertThat(submittedAccount.getBalance().scale()).isEqualTo(2);
        assertThat(submittedAccount.getCreatedAt()).isEqualTo("2026-08-31T14:00:00Z");

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.balance()).isEqualByComparingTo("123.45");
        assertThat(result.createdAt()).isEqualTo("2026-08-31T14:00:00Z");
    }

    /** Proves that a zero opening balance remains a valid scale-two monetary value. */
    @Test
    void acceptsAZeroInitialBalance() {
        stubPersistedAccount(7L, "Zero account", new BigDecimal("0.00"));

        CreatedAccount result = service.create(new CreateAccountCommand("Zero account", BigDecimal.ZERO));

        assertThat(result.balance()).isEqualByComparingTo("0.00");
        verify(accountRepository).save(any(Account.class));
    }

    /**
     * Proves both HALF_EVEN tie directions so monetary normalization cannot
     * silently drift to a different rounding rule.
     */
    @ParameterizedTest
    @MethodSource("halfEvenCases")
    void normalizesInitialBalanceUsingHalfEven(BigDecimal requested, BigDecimal expected) {
        stubPersistedAccount(9L, "Rounded account", expected);

        service.create(new CreateAccountCommand("Rounded account", requested));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalance()).isEqualByComparingTo(expected);
    }

    /**
     * Proves that required, nonblank, and database-sized name rules fail before
     * persistence, keeping invalid requests free of side effects.
     */
    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsInvalidNamesBeforePersistence(String invalidName) {
        assertThatThrownBy(() -> service.create(new CreateAccountCommand(invalidName, BigDecimal.ZERO)))
                .isInstanceOf(AccountCreationValidationException.class);

        verify(accountRepository, never()).save(any());
    }

    /**
     * Proves negative values are rejected before rounding, including a
     * negative sub-cent value that would otherwise normalize to zero.
     */
    @ParameterizedTest
    @MethodSource("negativeBalances")
    void rejectsNegativeInputBeforeNormalization(BigDecimal negativeBalance) {
        assertThatThrownBy(() -> service.create(new CreateAccountCommand("John Doe", negativeBalance)))
                .isInstanceOf(AccountCreationValidationException.class)
                .hasMessage("Initial balance must be greater than or equal to zero.");

        verify(accountRepository, never()).save(any());
    }

    /** Proves a missing monetary value cannot reach persistence. */
    @Test
    void rejectsMissingInitialBalanceBeforePersistence() {
        assertThatThrownBy(() -> service.create(new CreateAccountCommand("John Doe", null)))
                .isInstanceOf(AccountCreationValidationException.class)
                .hasMessage("Initial balance is required.");

        verify(accountRepository, never()).save(any());
    }

    /**
     * Proves range validation occurs after rounding because rounding can carry
     * a nominally 17-digit input beyond the physical NUMERIC(19,2) boundary.
     */
    @Test
    void rejectsBalanceThatOverflowsAfterNormalization() {
        BigDecimal overflowsAfterRounding = new BigDecimal("99999999999999999.995");

        assertThatThrownBy(() -> service.create(new CreateAccountCommand("John Doe", overflowsAfterRounding)))
                .isInstanceOf(AccountCreationValidationException.class)
                .hasMessage("Initial balance exceeds the supported monetary range.");

        verify(accountRepository, never()).save(any());
    }

    /** Proves a missing command fails explicitly instead of producing a null-pointer failure. */
    @Test
    void rejectsMissingCreationCommand() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(AccountCreationValidationException.class)
                .hasMessage("Account creation data is required.");

        verify(accountRepository, never()).save(any());
    }

    private static Stream<Arguments> halfEvenCases() {
        return Stream.of(
                Arguments.of(new BigDecimal("1.225"), new BigDecimal("1.22")),
                Arguments.of(new BigDecimal("1.235"), new BigDecimal("1.24")));
    }

    private static Stream<String> invalidNames() {
        return Stream.of(null, "", "   ", "a".repeat(256));
    }

    private static Stream<BigDecimal> negativeBalances() {
        return Stream.of(new BigDecimal("-100.00"), new BigDecimal("-0.001"));
    }

    /** Supplies the generated persistence result while leaving input capture observable. */
    private void stubPersistedAccount(long id, String name, BigDecimal balance) {
        Account persistedAccount = mock(Account.class);
        when(persistedAccount.getId()).thenReturn(id);
        when(persistedAccount.getName()).thenReturn(name);
        when(persistedAccount.getBalance()).thenReturn(balance);
        when(persistedAccount.getCreatedAt()).thenReturn(OffsetDateTime.ofInstant(CREATION_INSTANT, ZoneOffset.UTC));
        when(accountRepository.save(any(Account.class))).thenReturn(persistedAccount);
    }
}
