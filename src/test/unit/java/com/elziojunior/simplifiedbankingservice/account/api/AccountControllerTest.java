package com.elziojunior.simplifiedbankingservice.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.elziojunior.simplifiedbankingservice.account.service.CreateAccountCommand;
import com.elziojunior.simplifiedbankingservice.account.service.CreateAccountService;
import com.elziojunior.simplifiedbankingservice.account.service.CreatedAccount;

class AccountControllerTest {

    /** Proves the HTTP adapter maps request and result without owning business rules. */
    @Test
    void shouldMapCreationRequestAndResult() {
        CreateAccountService service = mock(CreateAccountService.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-31T18:45:00Z");
        when(service.create(new CreateAccountCommand("Ada", new BigDecimal("12.345"))))
                .thenReturn(new CreatedAccount(7L, "Ada", new BigDecimal("12.34"), createdAt));
        AccountController controller = new AccountController(service);

        AccountResponse response = controller.create(
                new CreateAccountRequest("Ada", new BigDecimal("12.345")));

        assertThat(response).isEqualTo(
                new AccountResponse(7L, "Ada", new BigDecimal("12.34"), createdAt));
        verify(service).create(new CreateAccountCommand("Ada", new BigDecimal("12.345")));
    }
}
