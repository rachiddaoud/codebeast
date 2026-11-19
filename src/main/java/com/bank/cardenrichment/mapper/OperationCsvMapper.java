package com.bank.cardenrichment.mapper;

import com.bank.cardenrichment.dto.OperationCsvRow;
import com.bank.cardenrichment.statement.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Locale;

/** Card statement operation (XSD) to CSV line, with the field treatments. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OperationCsvMapper {

    @Mapping(target = "transactionId", source = "operationID")
    @Mapping(target = "cardNumber", source = "cardNumber", qualifiedByName = "maskPan")
    @Mapping(target = "acceptorName", source = "merchantName", qualifiedByName = "trim")
    @Mapping(target = "merchantId", source = "transactionalDataElements.cardAcceptorID")
    @Mapping(target = "terminalId", source = "transactionalDataElements.cardAcceptorTerminalID")
    @Mapping(target = "city", source = "merchantLocality", qualifiedByName = "trim")
    @Mapping(target = "country", source = "merchantCountry", qualifiedByName = "upper")
    @Mapping(target = "zip", source = "merchantZip")
    @Mapping(target = "mcc", source = "MCC", qualifiedByName = "padMcc")
    @Mapping(target = "transactionTimestamp", source = "oprDateAndTime")
    @Mapping(target = "currency", source = "currency", qualifiedByName = "upper")
    OperationCsvRow toCsvRow(Operation operation);

    /** Keeps the last 4 digits only: the PAN must never leave in clear. */
    @Named("maskPan")
    default String maskPan(String pan) {
        String digits = pan.replaceAll("\\D", "");
        int hidden = Math.max(0, digits.length() - 4);
        return "*".repeat(hidden) + digits.substring(hidden);
    }

    /** MCCs are 4 digits: "742" becomes "0742". */
    @Named("padMcc")
    default String padMcc(String mcc) {
        String s = mcc.strip();
        return "0".repeat(Math.max(0, 4 - s.length())) + s;
    }

    @Named("trim")
    default String trim(String s) {
        return s.strip();
    }

    @Named("upper")
    default String upper(String s) {
        return s.strip().toUpperCase(Locale.ROOT);
    }
}
