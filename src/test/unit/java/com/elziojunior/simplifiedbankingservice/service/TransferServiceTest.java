package com.elziojunior.simplifiedbankingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.elziojunior.simplifiedbankingservice.exception.TransferConflictException;
import com.elziojunior.simplifiedbankingservice.exception.TransferNotFoundException;
import com.elziojunior.simplifiedbankingservice.exception.TransferValidationException;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.TransferCompletedNotification;
import com.elziojunior.simplifiedbankingservice.model.entity.AccountEntity;
import com.elziojunior.simplifiedbankingservice.model.entity.TransferIdempotencyTokenEntity;
import com.elziojunior.simplifiedbankingservice.repository.AccountRepository;
import com.elziojunior.simplifiedbankingservice.repository.MovementRepository;
import com.elziojunior.simplifiedbankingservice.repository.TransferIdempotencyTokenRepository;

@ExtendWith(MockitoExtension.class)
final class TransferServiceTest {

    private static final UUID TOKEN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OPERATION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-31T19:00:00Z");

    @Mock private TransferIdempotencyTokenRepository tokenRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private MovementRepository movementRepository;
    @Mock private TransferNotificationAfterCommitScheduler notificationScheduler;
    @Mock private TransferLockTimeoutConfigurer lockTimeoutConfigurer;

    private TransferService service;
    private TransferIdempotencyTokenEntity token;

    @BeforeEach
    void setUp() {
        UuidGenerator uuidGenerator = mock(UuidGenerator.class);
        lenient().when(uuidGenerator.generate()).thenReturn(OPERATION, EVENT);
        service = new TransferService(
                tokenRepository,
                accountRepository,
                movementRepository,
                notificationScheduler,
                lockTimeoutConfigurer,
                uuidGenerator,
                Clock.fixed(Instant.parse("2026-08-31T19:00:00Z"), ZoneOffset.UTC));
        token = new TransferIdempotencyTokenEntity(TOKEN, NOW.minusMinutes(1), NOW.plusMinutes(9));
    }

    /** Proves a successful transfer locks in ascending order and persists every atomic effect. */
    @Test
    void shouldCreateCompleteTransferWithDeterministicLockOrder() {
        AccountEntity source = account(20L, "100.00");
        AccountEntity destination = account(10L, "25.00");
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.of(token));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(destination));
        when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(source));

        CompletedTransferDto result = service.createTransfer(command(20L, 10L, "40.005"));

        assertThat(result).isEqualTo(new CompletedTransferDto(OPERATION, 20L, 10L, new BigDecimal("40.00")));
        verify(source).updateBalance(new BigDecimal("60.00"));
        verify(destination).updateBalance(new BigDecimal("65.00"));
        verify(movementRepository).saveAll(any());
        ArgumentCaptor<TransferCompletedNotification> notification =
                ArgumentCaptor.forClass(TransferCompletedNotification.class);
        verify(notificationScheduler).schedule(notification.capture());
        assertThat(notification.getValue().eventId()).isEqualTo(EVENT);
        assertThat(notification.getValue().operationId()).isEqualTo(OPERATION);
        assertThat(notification.getValue().recipientAccountId()).isEqualTo(20L);
        assertThat(notification.getValue().eventType()).isEqualTo(TransferCompletedNotification.TRANSFER_COMPLETED);
        assertThat(notification.getValue().amount()).isEqualByComparingTo("40.00");
        assertThat(token.getOperationId()).isEqualTo(OPERATION);
        InOrder locks = inOrder(accountRepository);
        locks.verify(accountRepository).findByIdForUpdate(10L);
        locks.verify(accountRepository).findByIdForUpdate(20L);
    }

    /** Proves an identical completed-token retry returns the established result without account effects. */
    @Test
    void shouldReplayIdenticalCompletedTransfer() {
        token.associate(OPERATION, 1L, 2L, new BigDecimal("10.00"), NOW.minusSeconds(1));
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.of(token));

        CompletedTransferDto result = service.createTransfer(command(1L, 2L, "10.0"));

        assertThat(result.transferId()).isEqualTo(OPERATION);
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(movementRepository, never()).saveAll(any());
        verify(notificationScheduler, never()).schedule(any());
    }

    /** Proves a completed token cannot authorize a different normalized payload. */
    @Test
    void shouldRejectCompletedTokenPayloadMismatch() {
        token.associate(OPERATION, 1L, 2L, new BigDecimal("10.00"), NOW.minusSeconds(1));
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "11.00")))
                .isInstanceOf(TransferConflictException.class);

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    /** Proves an expired token fails before account locks or financial mutation. */
    @Test
    void shouldRejectExpiredToken() {
        token = new TransferIdempotencyTokenEntity(TOKEN, NOW.minusMinutes(11), NOW);
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "10.00")))
                .isInstanceOf(TransferConflictException.class);

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    /** Proves an unknown token is an idempotency conflict without account access. */
    @Test
    void shouldRejectUnknownToken() {
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "10.00")))
                .isInstanceOf(TransferConflictException.class);
    }

    /** Proves same-account transfers are rejected before any repository interaction. */
    @Test
    void shouldRejectSameAccountTransfer() {
        assertThatThrownBy(() -> service.createTransfer(command(1L, 1L, "10.00")))
                .isInstanceOf(TransferConflictException.class);

        verify(tokenRepository, never()).findByTokenForUpdate(any());
    }

    /** Proves nonpositive and sub-cent-to-zero monetary values cannot reach token locking. */
    @Test
    void shouldRejectUnsupportedAmounts() {
        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "0")))
                .isInstanceOf(TransferValidationException.class);
        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "0.001")))
                .isInstanceOf(TransferValidationException.class);
        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "-1")))
                .isInstanceOf(TransferValidationException.class);
        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "99999999999999999.995")))
                .isInstanceOf(TransferValidationException.class);

        verify(tokenRepository, never()).findByTokenForUpdate(any());
    }

    /** Proves a missing account yields a stable not-found failure before mutation. */
    @Test
    void shouldRejectMissingAccount() {
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.of(token));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "10.00")))
                .isInstanceOf(TransferNotFoundException.class);
    }

    /** Proves insufficient funds are checked against locked state and persist no effects. */
    @Test
    void shouldRejectInsufficientFundsAfterBothLocks() {
        AccountEntity source = account(1L, "9.99");
        AccountEntity destination = account(2L, "0.00");
        when(tokenRepository.findByTokenForUpdate(TOKEN)).thenReturn(Optional.of(token));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> service.createTransfer(command(1L, 2L, "10.00")))
                .isInstanceOf(TransferConflictException.class);

        verify(movementRepository, never()).saveAll(any());
        verify(notificationScheduler, never()).schedule(any());
    }

    private CreateTransferDto command(long sourceId, long destinationId, String amount) {
        return new CreateTransferDto(TOKEN, sourceId, destinationId, new BigDecimal(amount));
    }

    private AccountEntity account(long id, String balance) {
        AccountEntity account = mock(AccountEntity.class);
        lenient().when(account.getId()).thenReturn(id);
        lenient().when(account.getBalance()).thenReturn(new BigDecimal(balance));
        return account;
    }
}
