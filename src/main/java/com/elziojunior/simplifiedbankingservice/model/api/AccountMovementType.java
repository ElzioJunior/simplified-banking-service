package com.elziojunior.simplifiedbankingservice.model.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Public movement directions supported by the account-history API. */
@Schema(description = "Movement direction", allowableValues = {"CREDIT", "DEBIT"})
public enum AccountMovementType {
    CREDIT,
    DEBIT
}
