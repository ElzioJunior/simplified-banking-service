package com.elziojunior.simplifiedbankingservice.model.dto;

import java.math.BigDecimal;

/**
 * Application input for account creation.
 *
 * @param name account-holder name
 * @param initialBalance requested opening balance before monetary normalization
 */
public record CreateAccountDto(String name, BigDecimal initialBalance) {
}
