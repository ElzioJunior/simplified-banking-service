package com.elziojunior.simplifiedbankingservice.service;

import java.math.BigDecimal;

/**
 * Application input for account creation.
 *
 * @param name account-holder name
 * @param initialBalance requested opening balance before monetary normalization
 */
public record CreateAccountCommand(String name, BigDecimal initialBalance) {
}
