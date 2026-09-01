package com.elziojunior.simplifiedbankingservice.metrics;

/** API operations permitted as bounded-cardinality metric tags. */
public enum ApiOperation {

    ACCOUNT_CREATE("account.create"),
    TRANSFER_TOKEN_ISSUE("transfer-token.issue"),
    TRANSFER_CREATE("transfer.create");

    private final String metricTag;

    ApiOperation(String metricTag) {
        this.metricTag = metricTag;
    }

    public String metricTag() {
        return metricTag;
    }
}
