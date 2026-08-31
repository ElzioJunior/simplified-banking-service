package com.elziojunior.simplifiedbankingservice.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.elziojunior.simplifiedbankingservice.model.api.TransferTokenResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.IssuedTransferTokenDto;

/** Maps transfer-token application results to their public API model. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransferTokenMapper {

    /** Converts an issued token DTO into the response returned by the HTTP endpoint. */
    TransferTokenResponse toResponse(IssuedTransferTokenDto issuedToken);
}
