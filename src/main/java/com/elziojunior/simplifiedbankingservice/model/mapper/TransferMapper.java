package com.elziojunior.simplifiedbankingservice.model.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.elziojunior.simplifiedbankingservice.model.api.CreateTransferRequest;
import com.elziojunior.simplifiedbankingservice.model.api.TransferResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.CompletedTransferDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateTransferDto;

/** Maps transfer API models at the HTTP-to-application boundary. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransferMapper {

    /** Combines the idempotency header and validated body into one application DTO. */
    @Mapping(target = "token", source = "token")
    @Mapping(target = "sourceAccountId", source = "request.sourceAccountId")
    @Mapping(target = "destinationAccountId", source = "request.destinationAccountId")
    @Mapping(target = "amount", source = "request.amount")
    CreateTransferDto toDto(UUID token, CreateTransferRequest request);

    /** Exposes the completed application result with the API's stable status value. */
    @Mapping(target = "status", constant = "COMPLETED")
    TransferResponse toResponse(CompletedTransferDto transfer);
}
