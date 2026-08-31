package com.elziojunior.simplifiedbankingservice.service;

import java.util.UUID;

/** Generates application-owned UUID identities at a deterministic test boundary. */
@FunctionalInterface
public interface UuidGenerator {
    UUID generate();
}
