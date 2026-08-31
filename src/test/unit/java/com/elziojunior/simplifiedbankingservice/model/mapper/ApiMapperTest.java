package com.elziojunior.simplifiedbankingservice.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
import com.elziojunior.simplifiedbankingservice.model.api.CreateTransferRequest;
import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;
import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateAccountDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreatedAccountDto;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;

class ApiMapperTest {

    private final AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);
    private final TransferMapper transferMapper = Mappers.getMapper(TransferMapper.class);
    private final TransferTokenMapper transferTokenMapper = Mappers.getMapper(TransferTokenMapper.class);

    /** Proves account requests and results cross the API boundary without losing values. */
    @Test
    void shouldMapAccountApiModels() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T18:45:00Z");

        assertThat(accountMapper.toDto(new CreateAccountRequest("Ada", new BigDecimal("12.345"))))
                .isEqualTo(new CreateAccountDto("Ada", new BigDecimal("12.345")));
        assertThat(accountMapper.toResponse(
                new CreatedAccountDto(7L, "Ada", new BigDecimal("12.34"), createdAt)))
                .isEqualTo(new AccountResponse(7L, "Ada", new BigDecimal("12.34"), createdAt));
    }

    /** Proves transfer mapping combines header and body input and supplies the stable response status. */
    @Test
    void shouldMapTransferApiModels() {
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID transferId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertThat(transferMapper.toDto(
                token, new CreateTransferRequest(1L, 2L, new BigDecimal("12.345"))))
                .isEqualTo(new CreateTransferDto(token, 1L, 2L, new BigDecimal("12.345")));
        assertThat(transferMapper.toResponse(
                new CompletedTransferDto(transferId, 1L, 2L, new BigDecimal("12.34"))))
                .isEqualTo(new TransferResponse(
                        transferId, "COMPLETED", 1L, 2L, new BigDecimal("12.34")));
    }

    /** Proves issued-token data is exposed through the public API without transformation loss. */
    @Test
    void shouldMapTransferTokenResponse() {
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-08-31T14:10:00Z");

        assertThat(transferTokenMapper.toResponse(new IssuedTransferTokenDto(token, expiresAt)))
                .isEqualTo(new TransferTokenResponse(token, expiresAt));
    }
}
