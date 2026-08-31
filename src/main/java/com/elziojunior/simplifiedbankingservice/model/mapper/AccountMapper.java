package com.elziojunior.simplifiedbankingservice.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.elziojunior.simplifiedbankingservice.model.api.AccountResponse;
import com.elziojunior.simplifiedbankingservice.model.api.CreateAccountRequest;
import com.elziojunior.simplifiedbankingservice.model.dto.CreateAccountDto;
import com.elziojunior.simplifiedbankingservice.model.dto.CreatedAccountDto;

/** Maps account API models at the HTTP-to-application boundary. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {

    /** Converts validated HTTP input into the DTO expected by account creation. */
    CreateAccountDto toDto(CreateAccountRequest request);

    /** Converts the persisted application result into the public account response. */
    AccountResponse toResponse(CreatedAccountDto account);
}
