package com.elziojunior.simplifiedbankingservice.model.dto;

/** Supported recent-history windows for account movement queries. */
public enum MovementLookbackPeriod {
    ONE_DAY("1d"),
    ONE_WEEK("1w"),
    ONE_MONTH("1M");

    private final String apiValue;

    MovementLookbackPeriod(String apiValue) {
        this.apiValue = apiValue;
    }

    /**
     * Resolves the exact public value so the HTTP mapper does not depend on
     * internal enum names.
     *
     * @param apiValue validated public period value
     * @return matching application period
     * @throws IllegalArgumentException when the value is not supported
     */
    public static MovementLookbackPeriod fromApiValue(String apiValue) {
        for (MovementLookbackPeriod period : values()) {
            if (period.apiValue.equals(apiValue)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unsupported movement lookback period.");
    }
}
