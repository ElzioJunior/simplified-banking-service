package com.elziojunior.simplifiedbankingservice.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementFilterRequest;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementPageResponse;
import com.elziojunior.simplifiedbankingservice.model.api.AccountMovementResponse;
import com.elziojunior.simplifiedbankingservice.model.dto.ListAccountMovementsDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementItemDto;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementLookbackPeriod;
import com.elziojunior.simplifiedbankingservice.model.dto.MovementPageDto;

/** Maps account-movement models at the HTTP-to-application boundary. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMovementMapper {

    /** Combines the path identity and optional request filters into one application query. */
    @Mapping(target = "accountId", source = "accountId")
    @Mapping(target = "page", source = "request.page", defaultValue = "0")
    @Mapping(target = "period", expression = "java(toPeriod(request.period()))")
    @Mapping(target = "type", source = "request.type")
    ListAccountMovementsDto toDto(Long accountId, AccountMovementFilterRequest request);

    /** Converts a validated public period value and applies the one-day default without exposing internal names. */
    default MovementLookbackPeriod toPeriod(String period) {
        return MovementLookbackPeriod.fromApiValue(period == null ? "1d" : period);
    }

    /** Converts an application page into the stable public page envelope. */
    AccountMovementPageResponse toResponse(MovementPageDto page);

    /** Converts one approved application movement item into its public representation. */
    AccountMovementResponse toResponse(MovementItemDto movement);
}
