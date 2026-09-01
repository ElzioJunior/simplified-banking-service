package com.elziojunior.simplifiedbankingservice.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.elziojunior.simplifiedbankingservice.api.AccountController;
import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.model.dto.CreateAccountDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreatedAccountDto;
import com.elziojunior.simplifiedbankingservice.model.mapper.AccountMapper;
import com.elziojunior.simplifiedbankingservice.service.AccountService;

class AccountEntityControllerTest {

    /** Proves the HTTP adapter maps request and result without owning business rules. */
    @Test
    void shouldMapCreationRequestAndResult() {
        AccountService service = mock(AccountService.class);
        AccountMapper mapper = mock(AccountMapper.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T18:45:00Z");
        CreateAccountRequest request = new CreateAccountRequest("Ada", new BigDecimal("12.345"));
        CreateAccountDto input = new CreateAccountDto("Ada", new BigDecimal("12.345"));
        CreatedAccountDto created = new CreatedAccountDto(7L, "Ada", new BigDecimal("12.34"), createdAt);
        AccountResponse expected = new AccountResponse(7L, "Ada", new BigDecimal("12.34"), createdAt);
        when(mapper.toDto(request)).thenReturn(input);
        when(service.createAccount(input)).thenReturn(created);
        when(mapper.toResponse(created)).thenReturn(expected);
        AccountController controller = new AccountController(service, mapper);

        AccountResponse response = controller.create(request);

        assertThat(response).isEqualTo(expected);
        verify(mapper).toDto(request);
        verify(service).createAccount(input);
        verify(mapper).toResponse(created);
    }
}
